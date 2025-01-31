package mcmultipart.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import mcmultipart.MCMultiPart;
import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartOcclusionHelper;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.slot.SlotUtil;
import mcmultipart.capability.CapabilityJoiner;
import mcmultipart.client.TESRMultipartContainer;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.multipart.PartInfo;
import mcmultipart.network.MultipartAction;
import mcmultipart.network.MultipartNetworkHandler;
import mcmultipart.util.WorldExt;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.GameData;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TileMultipartContainer extends TileEntity implements IMultipartContainer {

    private final Map<IPartSlot, PartInfo> parts = new ConcurrentHashMap<>(), partView = Collections.unmodifiableMap(parts);
    private boolean isInWorld = true;
    private Map<IPartSlot, CompoundNBT> missingParts;
    private World loadingWorld;

    private TileMultipartContainer(TileEntityType tileEntityType, World world, BlockPos pos) {
        super(tileEntityType);
        setWorld(world);
        setPos(pos);
        isInWorld = false;
    }

    private TileMultipartContainer(World world, BlockPos pos) {
        this(MCMultiPart.TYPE, world, pos);
    }

    public TileMultipartContainer(TileEntityType type) {
        super(type);
    }

    public TileMultipartContainer() {
        super(MCMultiPart.TYPE);
    }

    // Just make a tile. Not sure why this needs the world and position, but apparently it does...
    public static IMultipartContainer createTile(World world, BlockPos pos) {
        return new TileMultipartContainer(world, pos);
    }

    public static IMultipartContainer createTileFromWorldInfo(World world, BlockPos pos) {
        PartInfo info = PartInfo.fromWorld(world, pos);
        boolean tick = info.getTile() != null && info.getTile().isTickable();

        TileMultipartContainer container = tick ? new TileMultipartContainer.Ticking(world, pos) : new TileMultipartContainer(world, pos);
        container.isInWorld = false;
        container.add(info.getSlot(), info);

        return container;
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        return new ModelDataMap.Builder().withInitial(BlockMultipartContainer.PROPERTY_INFO, parts.values().stream().map(part -> part.getInfo(world, pos)).collect(Collectors.toList())).build();
    }

    @Override
    public void setWorld(World world) {
        World prevWorld = getWorld();
        super.setWorld(world);
        isInWorld = true;
        if (world != prevWorld) {
            parts.values().forEach(PartInfo::refreshWorld);
        }
    }

    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        forEachTile(te -> te.setPartPos(pos));
    }

    public boolean isInWorld() {
        return isInWorld;
    }

    @Override
    public World getPartWorld() {
        return getWorld();
    }

    @Override
    public BlockPos getPartPos() {
        return getPos();
    }

    @Override
    public Optional<IPartInfo> get(IPartSlot slot) {
        if (slot == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parts.get(slot));
    }

    @Override
    public boolean canAddPart(IPartSlot slot, BlockState state, IMultipartTile tile) {
        Preconditions.checkNotNull(slot);
        Preconditions.checkNotNull(state);

        PartInfo otherInfo = null;
        World otherWorld = null;
        try {
            if (!isInWorld) { // Simulate being a multipart if it's not one
                otherInfo = getParts().values().iterator().next();
                otherWorld = otherInfo.getTile() != null ? otherInfo.getTile().getPartWorld() : null;
                otherInfo.refreshWorld();
            }

            IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
            Preconditions.checkState(part != null, "The blockstate " + state + " could not be converted to a multipart!");
            PartInfo info = new PartInfo(this, slot, part, state, tile);

            // If any of the slots required by this multipart aren't empty, fail.
            Set<IPartSlot> partSlots = Sets.newIdentityHashSet();
            partSlots.addAll(part.getGhostSlots(info));
            partSlots.add(slot);
            if (partSlots.stream().anyMatch(parts::containsKey)
                    || parts.values().stream().map(i -> i.getPart().getGhostSlots(i)).flatMap(Set::stream).anyMatch(partSlots::contains)) {
                partSlots.clear();
                return false;
            }
            partSlots.clear();

            // If the occlusion boxes of this part intersect with any other parts', fail.
            if (MultipartOcclusionHelper.testContainerPartIntersection(this, info)) {
                return false;
            }
        } finally {
            if (otherWorld != null) { // Return to the old world if it's not a multipart
                otherInfo.setWorld(otherWorld);
            }
        }

        return true;
    }

    @Override
    public void addPart(IPartSlot slot, BlockState state, IMultipartTile tile) {
        IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
        Preconditions.checkState(part != null, "The blockstate " + state + " could not be converted to a multipart!");

        addPartDo(slot, state, part, tile);

        updateWorldState();
    }

    private void addPartDo(IPartSlot slot, BlockState state, IMultipart part, IMultipartTile tile) {
        if (missingParts != null) {
            missingParts.remove(slot);
        }

        PartInfo info = new PartInfo(this, slot, part, state, tile);
        add(slot, info);

        if (tile != null) {
            tile.validatePart();
        }

        if (!getWorld().isRemote) {
            part.onAdded(info);
            parts.values().forEach(i -> {
                if (i != info) {
                    i.getPart().onPartAdded(i, info);
                }
            });

            MultipartNetworkHandler.queuePartChange(getWorld(), new MultipartAction.Add(info));
        }
    }

    @Override
    public void removePart(IPartSlot slot) {
        PartInfo info = parts.get(slot);
        removePartDo(slot, info);
        updateWorldState();
    }

    private void removePartDo(IPartSlot slot, PartInfo info) {
        if (missingParts != null) {
            missingParts.remove(slot);
        }

        remove(slot);

        if (!getWorld().isRemote) {
//            info.getPart().breakPart(info);
            info.getPart().onRemoved(info);
            parts.values().forEach(i -> i.getPart().onPartRemoved(i, info));

            MultipartNetworkHandler.queuePartChange(getWorld(), new MultipartAction.Remove(getPos(), slot));
        }
    }

    protected void updateWorldState() {
        BlockState prevSt = getWorld().getBlockState(getPos());

        if (parts.size() == 1) {
            PartInfo part = parts.values().iterator().next();

            // After breaking a block, Minecraft automatically sends an update packet to update the block the player
            // destroyed. This causes the TE to get lost, since setting a new block state removes the old TE.
            // We don't want this to happen, so we flush the part changes before Minecraft sends the update packet so
            // the block replacing can be handled by MCMultiPart and therefore the TE is kept.
            MultipartNetworkHandler.flushChanges(getWorld(), getPos());

            getWorld().setBlockState(getPos(), part.getState(), 0);
            if (part.getTile() != null) {
                TileEntity te = part.getTile().asTileEntity();
                te.validate();
                getWorld().setTileEntity(getPos(), te);
            }

            this.isInWorld = false;
        } else {
            int currentTicking = countTickingParts();
            boolean isTETicking = this instanceof ITickableTileEntity;
            TileMultipartContainer container = this;
            boolean needsBlockUpdate = false;

            if (currentTicking == 0 && isTETicking) {
                needsBlockUpdate = true;
                container = new TileMultipartContainer(getWorld(), getPos());
            } else if (currentTicking > 0 && !isTETicking) {
                needsBlockUpdate = true;
                container = new TileMultipartContainer.Ticking(getWorld(), getPos());
            } else if (prevSt.getBlock() != MCMultiPart.multipart) {
                needsBlockUpdate = true;
                parts.values().forEach(it -> {
                    it.setContainer(this);
                    it.refreshWorld();
                });
            }

            if (needsBlockUpdate) {
                if (container != this) transferTo(container);

                WorldExt.setBlockStateHack(getWorld(), getPos(), MCMultiPart.multipart.getDefaultState()
                        .with(BlockMultipartContainer.PROPERTY_TICKING, container instanceof ITickableTileEntity), 0);
                getWorld().setTileEntity(getPos(), container);

                this.isInWorld = false;
                container.isInWorld = true;
            }
        }

        BlockState st = getWorld().getBlockState(getPos());
        getWorld().markAndNotifyBlock(getPos(), null, prevSt, st, 1); // Only cause a block update, clients are notified through a packet
        getWorld().getLight(getPos());
        requestModelDataUpdate();
    }

    private int countTickingParts() {
        return (int) parts.values().stream().map(IPartInfo::getTile).filter(t -> t != null && t.isTickable()).count();
    }

    protected void add(IPartSlot slot, PartInfo partInfo) {
        parts.put(slot, partInfo);
        partInfo.setContainer(this);
    }

    protected void remove(IPartSlot slot) {
        parts.remove(slot);
    }

    protected void clear() {
        parts.clear();
    }

    protected void transferTo(TileMultipartContainer container) {
        parts.forEach(container::add); // Doing it like this to add them to the ticking list if needed
        if (missingParts != null) {
            container.missingParts = missingParts;
        }
    }

    @Override
    public Map<IPartSlot, PartInfo> getParts() {
        return partView;
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);
        tag = writeParts(tag, false);
        return tag;
    }

    @Override
    public void read(CompoundNBT tag) {
        super.read(tag);
        readParts(tag, false, loadingWorld);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = super.getUpdateTag();
        writeParts(tag, true);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundNBT tag) {
        super.read(tag);
        readParts(tag, true, getPartWorld());
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getPartPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    private CompoundNBT writeParts(CompoundNBT tag, boolean update) {
        CompoundNBT parts = new CompoundNBT();
        this.parts.forEach((s, i) -> {
            CompoundNBT t = new CompoundNBT();
            t.putInt("state", MCMultiPart.stateMap.get(i.getState()));
            IMultipartTile tile = i.getTile();
            if (tile != null) {
                if (update) {
                    t.put("tile", tile.getPartUpdateTag());
                } else {
                    t.put("tile", tile.writePart(new CompoundNBT()));
                }
            }
            parts.put(Integer.toString(MCMultiPart.slotRegistry.getID(s)), t);
        });
        if (this.missingParts != null) {
            this.missingParts.forEach((s, t) -> parts.put(Integer.toString(MCMultiPart.slotRegistry.getID(s)), t));
        }
        tag.put("parts", parts);
        return tag;
    }

    private void readParts(CompoundNBT tag, boolean update, World world) {
        World prevWorld = this.world;
        this.world = world;
        ObjectIntIdentityMap<BlockState> stateMap = GameData.getBlockStateIDMap();
        CompoundNBT parts = tag.getCompound("parts");
        Set<IPartSlot> visitedSlots = new HashSet<>();
        for (String sID : parts.keySet()) {
            IPartSlot slot = MCMultiPart.slotRegistry.getValue(Integer.parseInt(sID));
            if (slot != null) {
                visitedSlots.add(slot);
                PartInfo prevInfo = this.parts.get(slot);

                CompoundNBT t = parts.getCompound(sID);
                BlockState state = stateMap.getByValue(t.getInt("state"));
                if (prevInfo != null) {
                    prevInfo.setState(state);
                    if (t.contains("tile")) {
                        CompoundNBT tileTag = t.getCompound("tile");
                        IMultipartTile tile = prevInfo.getTile();
                        if (update) {
                            if (tile == null) {
                                tile = prevInfo.getPart().createMultipartTile(world, slot, state);
                            }
                        } else {
                            tile = prevInfo.getPart().loadMultipartTile(world, tileTag);
                        }
                        prevInfo.setTile(tile);
                        if (update) {
                            tile.handlePartUpdateTag(tileTag);
                        }
                    }
                } else {
                    IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
                    if (part != null) {
                        IMultipartTile tile = null;
                        CompoundNBT tileTag = null;
                        if (t.contains("tile")) {
                            tileTag = t.getCompound("tile");
                            if (update) {
                                tile = part.createMultipartTile(world, slot, state);
                            } else {
                                tile = part.loadMultipartTile(world, tileTag);
                            }
                        }
                        add(slot, new PartInfo(this, slot, part, state, tile));
                        if (update && tileTag != null) {
                            tile.handlePartUpdateTag(tileTag);
                        }
                    } else if (!update) {
                        if (missingParts == null) {
                            missingParts = new HashMap<>();
                        }
                        missingParts.put(slot, t);
                    } else {
                        throw new IllegalStateException("Server sent a multipart of type " + state + " which is not registered on the client.");
                    }
                }
            }
        }
        Set<IPartSlot> removed = new HashSet<>(this.parts.keySet());
        removed.removeAll(visitedSlots);
        removed.forEach(this::remove);
        this.world = prevWorld;
    }

    @Override
    public void onLoad() {
        forEachTile(te -> te.setPartPos(getPartPos()));
        parts.values().forEach(PartInfo::refreshWorld);
        forEachTile(IMultipartTile::onPartLoad);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        forEachTile(IMultipartTile::onPartChunkUnloaded);
    }

    @Override
    public void mirror(Mirror mirror) {
        super.mirror(mirror);
        forEachTile(te -> te.mirrorPart(mirror));
    }

    @Override
    public void rotate(Rotation rotation) {
        super.rotate(rotation);
        forEachTile(te -> te.rotatePart(rotation));
    }

    @Override
    public void remove() {
        super.remove();
        if (!isInWorld) {
            forEachTile(IMultipartTile::removePart);
        }
    }

    @Override
    public void validate() {
        super.validate();
        forEachTile(IMultipartTile::validatePart);
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        forEachTile(IMultipartTile::updatePartContainerInfo);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return parts.values().stream().map(IPartInfo::getTile).filter(Objects::nonNull).mapToDouble(IMultipartTile::getMaxPartRenderDistanceSquared)
                .max().orElse(super.getMaxRenderDistanceSquared());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return parts.values().stream().map(IPartInfo::getTile).filter(Objects::nonNull)//
                .reduce(super.getRenderBoundingBox(), (a, b) -> a.union(b.getPartRenderBoundingBox()), (a, b) -> b);
    }

    @Override
    public boolean canRenderBreaking() {
        return true;
    }

    @Override
    public boolean hasFastRenderer() {
        return !FMLEnvironment.dist.isClient() || hasFastRendererC();
    }

    @OnlyIn(Dist.CLIENT)
    private boolean hasFastRendererC() {
        for (IPartInfo info : parts.values()) {
            TileEntity te = info.getTile() != null ? info.getTile().asTileEntity() : null;
            if (te != null && TileEntityRendererDispatcher.instance.getRenderer(te) != null && !te.hasFastRenderer()) {
                return false;
            }
        }
        return true;
    }

    //    @Override
    public boolean shouldRenderInPass(int pass) {
        if (FMLEnvironment.dist.isClient()) {
            shouldRenderInPassC(pass);
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void shouldRenderInPassC(int pass) {
        TESRMultipartContainer.pass = pass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == MCMPCapabilities.MULTIPART_CONTAINER) {
            return LazyOptional.of(() -> this).cast();
        }
        T val = SlotUtil
                .viewContainer(this,
                        i -> {
                            if (i.getTile() != null) {
                                LazyOptional<T> t = i.getTile().getPartCapability(capability, facing);
                                if (t.isPresent()) {
                                    return t.orElseThrow(NullPointerException::new);
                                }
                            }
                            return null;
                        },
                        l -> CapabilityJoiner.join(capability, l), null, true, facing);
        if (val != null) {
            return LazyOptional.of(() -> val);
        }
        return super.getCapability(capability, facing);
    }

    protected void forEachTile(Consumer<IMultipartTile> consumer) {
        for (PartInfo info : parts.values()) {
            IMultipartTile tile = info.getTile();
            if (tile != null) {
                consumer.accept(tile);
            }
        }
    }

    public static class Ticking extends TileMultipartContainer implements ITickableTileEntity {

        private final Set<ITickableTileEntity> tickingParts = Collections.newSetFromMap(new WeakHashMap<>());

        private Ticking(World world, BlockPos pos) {
            super(MCMultiPart.TICKING_TYPE, world, pos);
        }

        public Ticking() {
            super(MCMultiPart.TICKING_TYPE);
        }

        @Override
        public void tick() {
            if (tickingParts.isEmpty()) {
                updateWorldState();
            }
            tickingParts.forEach(ITickableTileEntity::tick);
        }

        @Override
        protected void add(IPartSlot slot, PartInfo partInfo) {
            super.add(slot, partInfo);
            IMultipartTile te = partInfo.getTile();
            if (te != null && te.isTickable()) {
                tickingParts.add(te.getTickable());
            }
        }

        @Override
        protected void remove(IPartSlot slot) {
            getPartTile(slot).ifPresent(tickingParts::remove);
            super.remove(slot);
        }

        @Override
        protected void clear() {
            tickingParts.clear();
            super.clear();
        }

    }

}