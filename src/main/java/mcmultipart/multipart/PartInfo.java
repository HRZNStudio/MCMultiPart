package mcmultipart.multipart;

import com.google.common.base.Preconditions;
import mcmultipart.MCMultiPart;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.world.IWorldView;
import mcmultipart.block.TileMultipartContainer;
import mcmultipart.util.MCMPBlockReaderWrapper;
import mcmultipart.util.MCMPWorldReaderWrapper;
import mcmultipart.util.MCMPWorldWrapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.function.IntUnaryOperator;

public final class PartInfo implements IPartInfo {

    private static final List<BlockRenderLayer> RENDER_LAYERS;

    static {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RENDER_LAYERS = Arrays.asList(BlockRenderLayer.values());
        } else {
            RENDER_LAYERS = new ArrayList<>();
        }
    }

    private final IPartSlot slot;
    private TileMultipartContainer container;
    private IMultipart part;
    private IBlockState state;
    private IMultipartTile tile;

    private IWorldView view;
    private MCMPWorldWrapper world;

    private Set<Long> scheduledTicks;

    public PartInfo(TileMultipartContainer container, IPartSlot slot, IMultipart part, IBlockState state, IMultipartTile tile) {
        this.container = container;
        this.slot = slot;
        setState(state, false);
        setTile(tile);
    }

    public static PartInfo fromWorld(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
        Preconditions.checkState(part != null, "The blockstate " + state + " could not be converted to a multipart!");
        TileEntity te = world.getTileEntity(pos);
        IPartSlot slot = part.getSlotFromWorld(world, pos, state);
        IMultipartTile tile = Optional.ofNullable(te).map(part::convertToMultipartTile).orElse(null);
        return new PartInfo(null, slot, part, state, tile);
    }

    public static void handleAdditionPacket(World world, BlockPos pos, IPartSlot slot, IBlockState state, NBTTagCompound tag) {
        MultipartHelper.getInfo(world, pos, slot).map(i -> i instanceof PartInfo ? (PartInfo) i : null).ifPresent(IPartInfo::remove);
        TileMultipartContainer tile = (TileMultipartContainer) MultipartHelper.getOrConvertContainer(world, pos).orElse(null);
        if (tile != null) {
            tile.addPart(slot, state);
            tile = (TileMultipartContainer) MultipartHelper.getOrConvertContainer(world, pos).orElse(null);
            PartInfo info = (PartInfo) tile.get(slot).orElse(null);
            if (info != null) {
                if (tag != null) {
                    if (info.getTile() != null) {
                        info.getTile().handlePartUpdateTag(tag);
                    } else {
                        MCMultiPart.log.error("Failed to handle the addition of the part " + state.getBlock().getRegistryName());
                        return;
                    }
                }
            } else {
                MCMultiPart.log.error("Failed to handle the addition of the part " + state.getBlock().getRegistryName());
                return;
            }
        } else {
            MCMultiPart.log.error("Failed to handle the addition of the part " + state.getBlock().getRegistryName());
            return;
        }
        world.markBlockRangeForRenderUpdate(pos, pos);
    }

    public static void handleUpdatePacket(World world, BlockPos pos, IPartSlot slot, IBlockState state, SPacketUpdateTileEntity pkt) {
        PartInfo info = MultipartHelper.getInfo(world, pos, slot).map(i -> i instanceof PartInfo ? (PartInfo) i : null).orElse(null);
        if (info != null) {
            info.setState(state);
            if (pkt != null) {
                if (info.getTile() == null) {
                    info.setTile(info.part.createMultipartTile(world, slot, state));
                }
                if (info.getTile() != null) {
                    info.getTile().onPartDataPacket(MCMultiPart.proxy.getNetworkManager(), pkt);
                }
            } else {
                info.setTile(info.part.createMultipartTile(world, slot, state));
            }
        } else {
            TileMultipartContainer tile = (TileMultipartContainer) MultipartHelper.getOrConvertContainer(world, pos).orElse(null);
            if (tile != null) {
                tile.addPart(slot, state);
                tile = (TileMultipartContainer) MultipartHelper.getOrConvertContainer(world, pos).orElse(null);
                info = (PartInfo) tile.get(slot).orElse(null);
                if (info != null) {
                    if (pkt != null) {
                        if (info.getTile() != null) {
                            info.getTile().onPartDataPacket(MCMultiPart.proxy.getNetworkManager(), pkt);
                        } else {
                            MCMultiPart.log.error("Failed to handle update packet for part " + state.getBlock().getRegistryName());
                            return;
                        }
                    }
                } else {
                    MCMultiPart.log.error("Failed to handle update packet for part " + state.getBlock().getRegistryName());
                    return;
                }
            } else {
                MCMultiPart.log.error("Failed to handle update packet for part " + state.getBlock().getRegistryName());
                return;
            }
        }
        world.markBlockRangeForRenderUpdate(pos, pos);
    }

    public static void handleRemovalPacket(World world, BlockPos pos, IPartSlot slot) {
        MultipartHelper.getInfo(world, pos, slot).map(i -> i instanceof PartInfo ? (PartInfo) i : null).ifPresent(info -> {
            info.remove();
            world.markBlockRangeForRenderUpdate(pos, pos);
        });
    }

    @Override
    public World getPartWorld() {
        return world == null ? getActualWorld() : world;
    }

    @Override
    public TileMultipartContainer getContainer() {
        return container;
    }

    public void setContainer(TileMultipartContainer container) {
        this.container = container;
        if (container != null && container.isInWorld()) {
            refreshWorld();
        }
    }

    @Override
    public IPartSlot getSlot() {
        return slot;
    }

    @Override
    public IMultipart getPart() {
        return part;
    }

    @Override
    public IBlockState getState() {
        return state;
    }

    public void setState(IBlockState state) {
        setState(state, true);
    }

    @Override
    public IMultipartTile getTile() {
        return tile;
    }

    public void setTile(IMultipartTile tile) {
        this.tile = tile;
        if (this.container != null && this.tile != null) {
            this.tile.setPartWorld(getPartWorld());
            this.tile.setPartPos(getPartPos());
            this.tile.setPartInfo(this);
        }
    }

    private void setState(IBlockState state, boolean checkTE) {
        if (state == this.state) {
            return;
        }
        IBlockState oldState = this.state;
        this.state = state;

        if (oldState == null || oldState.getBlock() != state.getBlock()) {
            this.part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
            refreshWorld();
        }

        if (checkTE && this.tile == null) {
            setTile(part.createMultipartTile(getPartWorld(), getSlot(), state));
        }
    }

    public void setWorld(World world) {
        this.view = null;
        this.world = null;
        if (this.tile != null) {
            this.tile.setPartWorld(world);
        }
    }

    public void refreshWorld() {
        this.view = container != null && part.shouldWrapWorld() ? part.getWorldView(this) : null;
        this.world = this.view != null ? new MCMPWorldWrapper(this, this, this.view) : null;
        if (this.tile != null) {
            setTile(this.tile); // Refreshes the world, position and PartInfo
        }
    }

    public IWorldReader wrapAsNeeded(IWorldReader world) {
        if (view != null) {
            if (world == this.world || world == this.world.getActualWorld()) {
                return this.world;
            } else {
                return new MCMPWorldReaderWrapper(world, this, view);
            }
        }
        return world;
    }
    public IBlockReader wrapAsNeeded(IBlockReader world) {
        if (view != null) {
            if (world == this.world || world == this.world.getActualWorld()) {
                return this.world;
            } else {
                return new MCMPBlockReaderWrapper(world, this, view);
            }
        }
        return world;
    }

    public void copyMetaFrom(PartInfo info) {
        scheduledTicks = info.scheduledTicks;
    }

    public void scheduleTick(int delay) {
        if (scheduledTicks == null) {
            scheduledTicks = new HashSet<>();
        }
        scheduledTicks.add(delay + getContainer().getPartWorld().getWorldInfo().getGameTime());
        getContainer().getPartWorld().getPendingBlockTicks().scheduleTick(getContainer().getPartPos(), MCMultiPart.multipart, delay);
    }

    public boolean checkAndRemoveTick() {
        return scheduledTicks != null && scheduledTicks.remove(getContainer().getPartWorld().getWorldInfo().getGameTime());
    }

    public boolean hasPendingTicks() {
        return scheduledTicks != null && !scheduledTicks.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    public ClientInfo getInfo(IWorldReader world, BlockPos pos) {
        IWorldReader world_ = wrapAsNeeded(world);
        Set<BlockRenderLayer> renderLayers;
        if (state.getRenderType() != EnumBlockRenderType.INVISIBLE) {
            renderLayers = EnumSet.noneOf(BlockRenderLayer.class);
            RENDER_LAYERS//
                    .stream()//
                    .filter(layer -> part.canRenderInLayer(world_, pos, this, state, layer))//
                    .forEach(renderLayers::add);
        } else {
            renderLayers = Collections.emptySet();
        }
        return new ClientInfo(state, state, renderLayers,
                index -> Minecraft.getInstance().getBlockColors().getColor(state, world_, pos, index));
    }

    @OnlyIn(Dist.CLIENT)
    public class ClientInfo {

        private final IBlockState actualState, extendedState;
        private final Set<BlockRenderLayer> renderLayers;
        private final IntUnaryOperator tintGetter;

        private ClientInfo(IBlockState actualState, IBlockState extendedState, Set<BlockRenderLayer> renderLayers, IntUnaryOperator tintGetter) {
            this.actualState = actualState;
            this.extendedState = extendedState;
            this.renderLayers = renderLayers;
            this.tintGetter = tintGetter;
        }

        public IBlockState getActualState() {
            return actualState;
        }

        public IBlockState getExtendedState() {
            return extendedState;
        }

        public boolean canRenderInLayer(BlockRenderLayer layer) {
            return renderLayers.contains(layer);
        }

        public int getTint(int index) {
            return tintGetter.applyAsInt(index);
        }

    }

}
