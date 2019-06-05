package mcmultipart.block;

import mcmultipart.MCMultiPart;
import mcmultipart.RayTraceHelper;
import mcmultipart.api.container.IMultipartContainerBlock;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.slot.SlotUtil;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelProperty;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class BlockMultipartContainer extends Block implements IMultipartContainerBlock {

    public static final BooleanProperty PROPERTY_TICKING = BooleanProperty.create("ticking");
    public static final ModelProperty<List<PartInfo.ClientInfo>> PROPERTY_INFO = new ModelProperty<>();
    private boolean callingLightOpacity = false;
    private boolean callingLightValue = false;

    public BlockMultipartContainer() {
        super(Block.Properties.create(Material.GROUND));
        setDefaultState(getDefaultState().with(PROPERTY_TICKING, true));
    }

    public static Optional<TileMultipartContainer> getTile(IBlockReader world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te != null && te instanceof TileMultipartContainer ? Optional.of((TileMultipartContainer) te) : Optional.empty();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(IBlockState state, IBlockReader world) {
        return state.get(PROPERTY_TICKING) ? new TileMultipartContainer.Ticking() : new TileMultipartContainer();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    //    @Override
//    public boolean isReplaceable(IWorldReader world, BlockPos pos) {
//        return getTile(world, pos).map(t -> t.getParts().isEmpty()).orElse(true);
//    }
//
//    @Override
//    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
//                                      Entity entity, boolean unknown) {
//        forEach(world, pos, i -> i.getPart().addCollisionBoxToList(i, entityBox, collidingBoxes, entity, unknown));
//    }

    @Nullable
    @Override
    public RayTraceResult getRayTraceResult(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end, RayTraceResult original) {
        Optional<TileMultipartContainer> te = getTile(world, pos);
        return te.map(t -> t.getParts().values()//
                .stream()//
                .map(i -> Pair.of(i, i.getPart().getRayTraceResult(i, start, end, original)))
                .filter(p -> p.getValue() != null)//
                .min(Comparator.comparingDouble(hit -> hit.getValue().hitVec.squareDistanceTo(start)))
                .map(p -> {
                    RayTraceResult hit = new RayTraceResult(p.getValue().hitVec, p.getValue().sideHit, p.getValue().getBlockPos());
                    hit.hitInfo = p.getValue();
                    hit.subHit = MCMultiPart.slotRegistry.getID(p.getKey().getSlot());
                    return hit;
                }).orElse(null)).orElse(original);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isTopSolid(IBlockState state) {
        return false;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> p_206840_1_) {
        super.fillStateContainer(p_206840_1_);
        p_206840_1_.add(PROPERTY_TICKING);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return true;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest, IFluidState fluid) {
        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
        RayTraceResult hit = collisionRayTrace(state, world, pos, vectors.getLeft(), vectors.getRight());
        Optional<TileMultipartContainer> tile = getTile(world, pos);
        if (hit != null && tile.isPresent()) {
            if (!world.isRemote) {
                IPartSlot slot = MCMultiPart.slotRegistry.getValue(hit.subHit);
                boolean canRemove = tile.get().get(slot).map(i -> {
                    if (i.getPart().canPlayerDestroy(i, player)) {
                        i.getPart().onPartHarvested(i, player);
                        if (player == null || !player.abilities.isCreativeMode) {
                            i.getPart().getDrops(i.getPartWorld(), pos, i, 0).forEach(s -> spawnAsEntity(world, pos, s));
                        }
                        return true;
                    } else {
                        return false;
                    }
                }).orElse(true);
                if (canRemove)
                    tile.get().removePart(slot);
            }
        }
        return false;
    }
//
//    @Override
//    public boolean isSideSolid(IBlockState state, IWorldReader world, BlockPos pos, EnumFacing side) {
//        return anyMatch(world, pos, i -> i.getPart().isSideSolid(i.wrapAsNeeded(world), pos, i, side));
//    }
//
//    @Override
//    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
//        forEach(world, pos, i -> i.getPart().randomDisplayTick(i, rand));
//    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean addDestroyEffects(IBlockState worldState, World world, BlockPos pos, ParticleManager manager) {
        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(MCMultiPart.proxy.getPlayer());
        RayTraceResult hit = collisionRayTrace(getDefaultState(), world, pos, vectors.getLeft(), vectors.getRight());
        if (hit != null) {
            IPartInfo part = getTile(world, pos).get().get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get();
            if (!part.getPart().addDestroyEffects(part, manager)) {
                IBlockState state = part.getState();
                for (int i = 0; i < 4; ++i) {
                    for (int j = 0; j < 4; ++j) {
                        for (int k = 0; k < 4; ++k) {
                            double xOff = (i + 0.5D) / 4.0D;
                            double yOff = (j + 0.5D) / 4.0D;
                            double zOff = (k + 0.5D) / 4.0D;
                            manager.addEffect(new ParticleDigging(world, pos.getX() + xOff, pos.getY() + yOff, pos.getZ() + zOff, xOff - 0.5D,
                                    yOff - 0.5D, zOff - 0.5D, state) {
                            }.setBlockPos(pos));
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean addHitEffects(IBlockState mpState, World world, RayTraceResult hit, ParticleManager manager) {
        if (hit != null) {
            BlockPos pos = hit.getBlockPos();
            IPartInfo part = getTile(world, pos).get().get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get();
            if (!part.getPart().addHitEffects(part, (RayTraceResult) hit.hitInfo, manager)) {
                if (part.getPart().getRenderType(part) != EnumBlockRenderType.INVISIBLE) {
                    int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                    AxisAlignedBB aabb = part.getPart().getShape(part.getState(), part.getActualWorld(), part.getPartPos()).getBoundingBox();
                    double pX = x + world.rand.nextDouble() * (aabb.maxX - aabb.minX - 0.2) + 0.1 + aabb.minX;
                    double pY = y + world.rand.nextDouble() * (aabb.maxY - aabb.minY - 0.2) + 0.1 + aabb.minY;
                    double pZ = z + world.rand.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2) + 0.1 + aabb.minZ;

                    switch (hit.sideHit) {
                        case DOWN:
                            pY = y + aabb.minY - 0.1;
                            break;
                        case UP:
                            pY = y + aabb.maxY + 0.1;
                            break;
                        case NORTH:
                            pZ = z + aabb.minZ - 0.1;
                            break;
                        case SOUTH:
                            pZ = z + aabb.maxZ + 0.1;
                            break;
                        case WEST:
                            pX = x + aabb.minX - 0.1;
                            break;
                        case EAST:
                            pX = x + aabb.maxX + 0.1;
                            break;
                    }

                    manager.addEffect(new ParticleDigging(world, pX, pY, pZ, 0.0D, 0.0D, 0.0D, part.getState()) {
                    }.setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
                }
            }
        }
        return true;
    }

    @Override
    public boolean addLandingEffects(IBlockState state, WorldServer worldObj, BlockPos blockPosition, IBlockState iblockstate,
                                     EntityLivingBase entity, int numberOfParticles) {
        return super.addLandingEffects(state, worldObj, blockPosition, iblockstate, entity, numberOfParticles);// TODO: Maybe?
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockReader world, BlockPos pos, @Nullable EnumFacing side) {
        if (side == null) {
            return false;
        }
        return getTile(world, pos)
                .map(t -> SlotUtil.viewContainer(t, i -> i.getPart().canConnectRedstone(((PartInfo) i).wrapAsNeeded(world), pos, i, side),
                        l -> l.stream().anyMatch(c -> c), false, true, side.getOpposite()))
                .orElse(false);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockReader world, BlockPos pos, EnumFacing side) {
        if (side == null) {
            return 0;
        }
        return getTile(world, pos).map(t -> SlotUtil.viewContainer(t, i -> i.getPart().getWeakPower(((PartInfo) i).wrapAsNeeded(world), pos, i, side),
                l -> l.stream().max(Integer::compare).get(), 0, true, side.getOpposite())).orElse(0);
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockReader world, BlockPos pos, EnumFacing side) {
        if (side == null) {
            return 0;
        }
        return getTile(world, pos)
                .map(t -> SlotUtil.viewContainer(t, i -> i.getPart().getStrongPower(((PartInfo) i).wrapAsNeeded(world), pos, i, side),
                        l -> l.stream().max(Integer::compare).get(), 0, true, side.getOpposite()))
                .orElse(0);
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IWorldReaderBase world, BlockPos pos, EntitySpawnPlacementRegistry.SpawnPlacementType type, EntityType<? extends EntityLiving> entity) {
        if (world instanceof IWorldReader)//TODO
            return anyMatch((IWorldReader) world, pos, i -> i.getPart().canCreatureSpawn(i.wrapAsNeeded((IWorldReader) world), pos, i, type, entity));
        return false;
    }
//
//    @Override
//    public boolean canSustainLeaves(IBlockState state, IWorldReader world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().canSustainLeaves(i.wrapAsNeeded(world), pos, i));
//    }
//
//    @Override
//    public boolean canSustainPlant(IBlockState state, IWorldReader world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
//        return anyMatch(world, pos, i -> i.getPart().canSustainPlant(i.wrapAsNeeded(world), pos, i, direction, plantable));
//    }

    @Override
    public void fillWithRain(World world, BlockPos pos) {
        forEach(world, pos, i -> i.getPart().fillWithRain(i));
    }

//    @Override
//    public float[] getBeaconColorMultiplier(IBlockState state, World world, BlockPos pos, BlockPos beaconPos) {
//        return super.getBeaconColorMultiplier(state, world, pos, beaconPos);// TODO: Maybe?
//    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos) {
        return max(world, pos, i -> i.getPart().getComparatorInputOverride(i));
    }

    @Override
    public void getDrops(IBlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune) {
        drops.addAll(getTile(world, pos).map(t -> t.getParts().values().stream().map(i -> i.getPart().getDrops(i.wrapAsNeeded(world), pos, i, fortune))
                .flatMap(List::stream).collect(Collectors.toList())).orElse(Collections.emptyList()));
    }

    @Override
    public float getEnchantPowerBonus(IBlockState state, IWorldReader world, BlockPos pos) {
        return addF(world, pos, i->i.getPart().getEnchantPowerBonus(i), Float.POSITIVE_INFINITY);
    }

    @Override
    public float getExplosionResistance(IBlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        return addF(world, pos, i -> i.getPart().getExplosionResistance(i, exploder, explosion), Float.POSITIVE_INFINITY);
    }

//
//    @Override
//    public int getFireSpreadSpeed(IWorldReader world, BlockPos pos, EnumFacing face) {
//        return super.getFireSpreadSpeed(world, pos, face);// TODO: Maybe?
//    }
//
//    @Override
//    public int getFlammability(IWorldReader world, BlockPos pos, EnumFacing face) {
//        return super.getFlammability(world, pos, face);// TODO: Maybe?
//    }
//
//    @Override
    public int getLightOpacity(IBlockState state, IWorldReader world, BlockPos pos) {
        if (callingLightOpacity) {
            return add(world, pos, i -> i.getPart().getLightOpacity(world, pos, i), 255);
        }
        callingLightOpacity = true;
        int res = add(world, pos, i -> i.getPart().getLightOpacity(i.wrapAsNeeded(world), pos, i), 255);
        callingLightOpacity = false;
        return res;
    }

    @Override
    public int getLightValue(IBlockState state, IWorldReader world, BlockPos pos) {
        if (callingLightValue) {
            return max(world, pos, i -> i.getPart().getLightValue(i.getState()));
        }
        callingLightValue = true;
        int res = max(world, pos, i -> i.getPart().getLightValue(i.wrapAsNeeded(world), pos, i));
        callingLightValue = false;
        return res;
    }

    @Override
    public int getPackedLightmapCoords(IBlockState state, IWorldReader world, BlockPos pos) {
        return super.getPackedLightmapCoords(state, world, pos);// TODO: Maybe?
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, EntityPlayer player) {
        if(target != null) {
            return getTile(world, pos).map(t -> t.get(MCMultiPart.slotRegistry.getValue(target.subHit))).filter(Optional::isPresent)
                    .map(o -> o.get().getPart().getPickPart(o.get(),(RayTraceResult) target.hitInfo, player)).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    //    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
        RayTraceResult hit = collisionRayTrace(getDefaultState(), world, pos, vectors.getLeft(), vectors.getRight());
        if (hit != null) {
            return getTile(world, pos).map(t -> t.get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get())
                    .map(i -> i.getPart().getPlayerRelativePartHardness(i, (RayTraceResult) hit.hitInfo, player)).orElse(0F);
        }
        return 0;
    }

    //    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, Entity entity) {
        return super.getSoundType(state, world, pos, entity);// TODO: Maybe? Needs a PR with the type of sound requested... >_>
    }
//    @Override
//    public boolean getTickRandomly() {
//        return true;
//    }
//
//    @Override
//    public boolean getWeakChanges(IWorldReader world, BlockPos pos) {
//        return true;
//    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return super.hasComparatorInputOverride(state);// TODO: Maybe? Needs a PR with the side... >_>
    }

    @Override
    public boolean hasCustomBreakingProgress(IBlockState state) {
        return true;
    }

//    @Override
//    public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB boundingBox, Material material) {
//        return getTile(world, pos).map(t -> t.getParts().values().stream().map(i -> i.getPart().isAABBInsideMaterial(i, boundingBox, material))
//                .filter(is -> is != null).anyMatch(i -> i)).orElse(false);
//    }
//
//    @Override
//    public boolean isAir(IBlockState state, IWorldReader world, BlockPos pos) {
//        return getTile(world, pos).map(t -> t.getParts().isEmpty()).orElse(true);
//    }
//
//    @Override
//    public boolean isBeaconBase(IWorldReader world, BlockPos pos, BlockPos beacon) {
//        return anyMatch(world, pos, i -> i.getPart().isBeaconBase(i.wrapAsNeeded(world), pos, i, beacon));
//    }
//
//    @Override
//    public boolean isBurning(IWorldReader world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().isBurning(i.wrapAsNeeded(world), pos, i));
//    }
//
//    @Override
//    public Boolean isEntityInsideMaterial(IWorldReader world, BlockPos pos, IBlockState state, Entity entity, double yToTest, Material material,
//                                          boolean testingHead) {
//        return getTile(world, pos).map(t -> t.getParts().values().stream()
//                .map(i -> i.getPart().isEntityInsideMaterial(i.wrapAsNeeded(world), pos, i, entity, yToTest, material, testingHead))
//                .filter(is -> is != null).anyMatch(i -> i)).orElse(false);
//    }
//
//    @Override
//    public boolean isFertile(World world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().isFertile(i));
//    }
//
//    @Override
//    public boolean isFireSource(World world, BlockPos pos, EnumFacing side) {
//        return anyMatch(world, pos, i -> i.getPart().isFireSource(i, side));
//    }
//
//    @Override
//    public boolean isFlammable(IWorldReader world, BlockPos pos, EnumFacing face) {
//        return anyMatch(world, pos, i -> i.getPart().isFlammable(i.wrapAsNeeded(world), pos, i, face));
//    }
//
//    @Override
//    public boolean isFoliage(IWorldReader world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().isFoliage(i.wrapAsNeeded(world), pos, i));
//    }

//    @Override
//    public boolean isLadder(IBlockState state, IWorldReader world, BlockPos pos, EntityLivingBase entity) {
//        return anyMatch(world, pos, i -> i.getPart().isLadder(i.wrapAsNeeded(world), pos, i, entity) &&
//                Optional.ofNullable(i.getPart().getCollisionShape(i.getPartWorld(), pos, i.getState()).offset(pos))
//                        .map(it -> it.intersects(entity.getEntityBoundingBox().grow(0.01 / 16F))).orElse(false));
//    }
//
//    @Override
//    public boolean isLeaves(IBlockState state, IWorldReader world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().isLeaves(i.wrapAsNeeded(world), pos, i));
//    }
//
//    @Override
//    public boolean isPassable(IWorldReader world, BlockPos pos) {
//        return anyMatch(world, pos, i -> i.getPart().isPassable(i.wrapAsNeeded(world), pos, i));
//    }
//
//    @Override
//    public boolean isWood(IWorldReader world, BlockPos pos) {
//        return allMatch(world, pos, i -> i.getPart().isWood(i.wrapAsNeeded(world), pos, i));
//    }
//
//    @Override
//    public Vec3d modifyAcceleration(World worldIn, BlockPos pos, Entity entityIn, Vec3d motion) {
//        return super.modifyAcceleration(worldIn, pos, entityIn, motion);// TODO: Maybe?
//    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        forEach(worldIn, pos, i -> i.getPart().neighborChanged(i, blockIn, fromPos));
    }
//
//    @Override
//    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
//                                    float hitY, float hitZ) {
//        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
//        RayTraceResult hit = collisionRayTrace(getDefaultState(), world, pos, vectors.getLeft(), vectors.getRight());
//        if (hit != null) {
//            return getTile(world, pos).map(t -> t.get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get())
//                    .map(i -> i.getPart().onPartActivated(i, player, hand, ((RayTraceResult) hit.hitInfo))).orElse(false);
//        }
//        return false;
//    }
//
//    @Override
//    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
//        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
//        RayTraceResult hit = collisionRayTrace(getDefaultState(), world, pos, vectors.getLeft(), vectors.getRight());
//        if (hit != null) {
//            getTile(world, pos).map(t -> t.get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get())
//                    .ifPresent(i -> i.getPart().onPartClicked(i, player, ((RayTraceResult) hit.hitInfo)));
//        }
//    }
//
//    @Override
//    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
//        // TODO: This is where we remove the parts. We can do fun stuff here!
//        super.onBlockExploded(world, pos, explosion);
//    }
//
//    @Override
//    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
//        forEach(world, pos, it -> {
//            List<AxisAlignedBB> boxes = new ArrayList<>();
//            AxisAlignedBB bb = entity.getCollisionBoundingBox();
//            if (bb != null)
//                it.getPart().addCollisionBoxToList(it, bb.grow(0.001), boxes, entity, false);
//            bb = entity.getEntityBoundingBox();
//            it.getPart().addCollisionBoxToList(it, bb.grow(0.001), boxes, entity, false);
//            if (!boxes.isEmpty())
//                it.getPart().onEntityCollidedWithPart(it, entity);
//        });
//    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        // TODO: Maybe? We need to check collision with the individual parts
    }

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        // TODO: Maybe? We need to check collision with the individual parts
    }
//
//    @Override
//    public void onLanded(World world, Entity entity) {
//        super.onLanded(world, entity);
//        // TODO: Maybe? We need to check collision with the individual parts
//    }
//
//    @Override
//    public void onNeighborChange(IWorldReader world, BlockPos pos, BlockPos neighbor) {
//        forEach(world, pos, i -> i.getPart().onNeighborChange(i, neighbor));
//    }
//
//    @Override
//    public void onPlantGrow(IBlockState state, World world, BlockPos pos, BlockPos source) {
//        forEach(world, pos, i -> i.getPart().onPlantGrow(i, source));
//    }
//
//    @Override
//    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
//        forEach(world, pos, i -> i.getPart().randomTick(i, random));
//    }
//
//    @Override
//    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
//        forEach(world, pos, i -> {
//            if (i.checkAndRemoveTick()) {
//                i.getPart().updateTick(i, rand);
//            }
//        });
//    }
//
//    @Override
//    public BlockFaceShape getBlockFaceShape(IWorldReader world, IBlockState state, BlockPos pos, EnumFacing face) {
//        if (face == null) {
//            return BlockFaceShape.UNDEFINED;
//        }
//        return getTile(world,
//                pos).map(t -> SlotUtil.viewContainer(t, i -> i.getPart().getPartFaceShape(i, face),
//                l -> l.stream().filter(Predicate.isEqual(BlockFaceShape.UNDEFINED).negate()).findFirst().orElse(BlockFaceShape.UNDEFINED),
//                BlockFaceShape.UNDEFINED, true, face)).orElse(BlockFaceShape.UNDEFINED);
//    }

    private void forEach(IWorldReader world, BlockPos pos, Consumer<PartInfo> consumer) {
        getTile(world, pos).ifPresent(t -> t.getParts().values().forEach(consumer));
    }

    private boolean anyMatch(IWorldReader world, BlockPos pos, Predicate<PartInfo> predicate) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().anyMatch(predicate)).orElse(false);
    }

    private boolean allMatch(IWorldReader world, BlockPos pos, Predicate<PartInfo> predicate) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().allMatch(predicate)).orElse(false);
    }

    private int add(IWorldReader world, BlockPos pos, ToIntFunction<PartInfo> converter, int max) {
        return Math.min(getTile(world, pos).map(t -> t.getParts().values().stream().mapToInt(converter).reduce(0, (a, b) -> a + b)).orElse(0), max);
    }

    private int max(IWorldReader world, BlockPos pos, ToIntFunction<PartInfo> converter) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().mapToInt(converter).max().orElse(0)).orElse(0);
    }

    private float addF(IWorldReader world, BlockPos pos, ToDoubleFunction<PartInfo> converter, double max) {
        return (float) Math.min(getTile(world, pos).map(t -> t.getParts().values().stream().mapToDouble(converter).reduce(0D, (a, b) -> a + b))
                .orElse(0D).floatValue(), max);
    }

}
