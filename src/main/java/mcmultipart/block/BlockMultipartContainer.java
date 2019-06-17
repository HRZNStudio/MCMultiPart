package mcmultipart.block;

import mcmultipart.MCMultiPart;
import mcmultipart.RayTraceHelper;
import mcmultipart.api.container.IMultipartContainerBlock;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.slot.SlotUtil;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.IPlantable;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

@SuppressWarnings("deprecation")
public class BlockMultipartContainer extends Block implements IMultipartContainerBlock {

    public static final BooleanProperty PROPERTY_TICKING = BooleanProperty.create("ticking");
    public static final ModelProperty<List<PartInfo.ClientInfo>> PROPERTY_INFO = new ModelProperty<>();
    private boolean callingLightOpacity = false;
    private boolean callingLightValue = false;

    public BlockMultipartContainer() {
        super(Block.Properties.create(Material.EARTH));
        setDefaultState(getDefaultState().with(PROPERTY_TICKING, true));
    }

    public static Optional<TileMultipartContainer> getTile(IBlockReader world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te != null && te instanceof TileMultipartContainer ? Optional.of((TileMultipartContainer) te) : Optional.empty();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return state.get(PROPERTY_TICKING) ? new TileMultipartContainer.Ticking() : new TileMultipartContainer();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public RayTraceResult getRayTraceResult(BlockState state, World world, BlockPos pos, Vec3d start, Vec3d end, RayTraceResult original) {
        Optional<TileMultipartContainer> te = getTile(world, pos);
        return te.map(t -> t.getParts().values()//
                .stream()//
                .map(i -> Pair.of(i, i.getPart().getRayTraceResult(i, start, end, world.func_217296_a(start, end, pos, i.getPart().getRayTraceShape(i), i.getState()))))
                .filter(p -> p.getValue() != null)//
                .min(Comparator.comparingDouble(hit -> hit.getValue().getHitVec().distanceTo(start)))
                .filter(p -> p.getValue() instanceof BlockRayTraceResult)
                .map(p -> Pair.of(p.getLeft(), (BlockRayTraceResult) p.getRight()))
                .map(p -> {
                    RayTraceResult hit = new BlockRayTraceResult(p.getValue().getHitVec(), p.getValue().getFace(), p.getValue().getPos(), false);
                    hit.hitInfo = p.getValue();
                    hit.subHit = MCMultiPart.slotRegistry.getID(p.getKey().getSlot());
                    return hit;
                }).orElse(null)).orElse(original);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction face, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        forEach(world, pos, i -> {
            BlockState s = i.getPart().updatePostPlacement(i, face, facingState, facingPos);
            if (s.isAir()) {
                i.remove();
            } else {
                i.setState(s);
            }
        });
        return state;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        super.fillStateContainer(p_206840_1_);
        p_206840_1_.add(PROPERTY_TICKING);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
        return true;
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid) {
        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
        RayTraceResult hit = world.func_217296_a(vectors.getLeft(), vectors.getRight(), pos, getRaytraceShape(state, world, pos), state);
        Optional<TileMultipartContainer> tile = getTile(world, pos);
        if (hit != null && tile.isPresent()) {
            IPartSlot slot = MCMultiPart.slotRegistry.getValue(hit.subHit);
            boolean canRemove = tile.get().get(slot).map(i -> {
                if (i.getPart().canPlayerDestroy(i, player)) {
                    i.getPart().onPartHarvested(i, player);
                    if (player == null || !player.abilities.isCreativeMode) {
                        if (!world.isRemote) {
//                            LootContext.Builder builder = new LootContext.Builder(i.getPartWorld());
//                            i.getPart().getDrops(i, builder);
//                            builder.forEach(s -> spawnAsEntity(world, pos, s));
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }).orElse(true);
            if (canRemove) {
                if (!world.isRemote) {
                    tile.get().removePart(slot);
                } else {
                    tile.flatMap(t -> t.getPartTile(slot)).ifPresent(mpt -> {
                        SoundType soundtype = mpt.getPartWorld().getBlockState(mpt.getPartPos()).getSoundType(mpt.getPartWorld(), mpt.getPartPos(), player);
                        mpt.getPartWorld().playSound(player, mpt.getPartPos(), soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    });
                }
            }
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean addDestroyEffects(BlockState wState, World world, BlockPos pos, ParticleManager manager) {
        Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(MCMultiPart.proxy.getPlayer());
        RayTraceResult hit = world.func_217296_a(vectors.getLeft(), vectors.getRight(), pos, getRaytraceShape(wState, world, pos), wState);
        if (hit != null) {
            IPartInfo part = getTile(world, pos).get().get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get();
            if (!part.getPart().addDestroyEffects(part, manager)) {
                BlockState state = part.getState();
                for (int i = 0; i < 4; ++i) {
                    for (int j = 0; j < 4; ++j) {
                        for (int k = 0; k < 4; ++k) {
                            double xOff = (i + 0.5D) / 4.0D;
                            double yOff = (j + 0.5D) / 4.0D;
                            double zOff = (k + 0.5D) / 4.0D;
                            manager.addEffect(new DiggingParticle(world, pos.getX() + xOff, pos.getY() + yOff, pos.getZ() + zOff, xOff - 0.5D,
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
    public boolean addHitEffects(BlockState mpState, World world, RayTraceResult hit, ParticleManager manager) {
        if (hit != null && hit instanceof BlockRayTraceResult) {
            BlockPos pos = new BlockPos(hit.getHitVec());
            getTile(world, pos).flatMap(tile -> tile.get(MCMultiPart.slotRegistry.getValue(hit.subHit))).ifPresent(part -> {
                if (!part.getPart().addHitEffects(part, (RayTraceResult) hit.hitInfo, manager)) {
                    if (part.getPart().getRenderType(part) != BlockRenderType.INVISIBLE) {
                        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                        AxisAlignedBB aabb = part.getPart().getShape(part).getBoundingBox();
                        double pX = x + world.rand.nextDouble() * (aabb.maxX - aabb.minX - 0.2) + 0.1 + aabb.minX;
                        double pY = y + world.rand.nextDouble() * (aabb.maxY - aabb.minY - 0.2) + 0.1 + aabb.minY;
                        double pZ = z + world.rand.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2) + 0.1 + aabb.minZ;

                        switch (((BlockRayTraceResult) hit).getFace()) {
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

                        manager.addEffect(new DiggingParticle(world, pX, pY, pZ, 0.0D, 0.0D, 0.0D, part.getState()) {
                        }.setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
                    }
                }
            });
        }
        return true;
    }

    @Override
    public boolean canProvidePower(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
        if (side == null) {
            return false;
        }
        return getTile(world, pos)
                .map(t -> SlotUtil.viewContainer(t, i -> i.getPart().canConnectRedstone(i, side),
                        l -> l.stream().anyMatch(c -> c), false, true, side.getOpposite()))
                .orElse(false);
    }

    @Override
    public int getWeakPower(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        if (side == null) {
            return 0;
        }
        return getTile(world, pos).map(t -> SlotUtil.viewContainer(t, i -> i.getPart().getWeakPower(i, side),
                l -> l.stream().max(Integer::compare).orElse(0), 0, true, side.getOpposite())).orElse(0);
    }

    @Override
    public int getStrongPower(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        if (side == null) {
            return 0;
        }
        return getTile(world, pos)
                .map(t -> SlotUtil.viewContainer(t, i -> i.getPart().getStrongPower(i, side),
                        l -> l.stream().max(Integer::compare).orElse(0), 0, true, side.getOpposite()))
                .orElse(0);
    }

    @Override
    public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, EntityType<?> entity) {
        return anyMatch(world, pos, i -> i.getPart().canCreatureSpawn(i, type, entity));
    }

    @Override
    public boolean canSustainPlant(BlockState state, IBlockReader world, BlockPos pos, Direction face, IPlantable plantable) {
        return anyMatch(world, pos, i -> i.getPart().canSustainPlant(i, face, plantable));
    }

    @Override
    public void fillWithRain(World world, BlockPos pos) {
        forEach(world, pos, i -> i.getPart().fillWithRain(i));
    }

//    @Override
//    public float[] getBeaconColorMultiplier(BlockState state, World world, BlockPos pos, BlockPos beaconPos) {
//        return super.getBeaconColorMultiplier(state, world, pos, beaconPos);// TODO: Maybe?
//    }

    @Override
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos) {
        return max(world, pos, i -> i.getPart().getComparatorInputOverride(i));
    }

//    @Override TODO
//    public void getDrops(BlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune) {
//        getTile(world, pos).map(t -> t.getParts().values()).orElse(Collections.emptyList())
//                .forEach(i -> i.getPart().getDrops(drops, i, fortune));
//    }

    @Override
    public float getEnchantPowerBonus(BlockState state, IWorldReader world, BlockPos pos) {
        return addF(world, pos, i -> i.getPart().getEnchantPowerBonus(i), Float.POSITIVE_INFINITY);
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        return addF(world, pos, i -> i.getPart().getExplosionResistance(i, exploder, explosion), Float.POSITIVE_INFINITY);
    }

    //    @Override
    public int getLightOpacity(BlockState state, IWorldReader world, BlockPos pos) {
        if (callingLightOpacity) {
            return add(world, pos, i -> i.getPart().getLightOpacity(i), 255);
        }
        callingLightOpacity = true;
        int res = add(world, pos, i -> i.getPart().getLightOpacity(i), 255);
        callingLightOpacity = false;
        return res;
    }

    //    @Override
    public int getLightValue(BlockState state, IWorldReader world, BlockPos pos) {
        if (callingLightValue) {
            return max(world, pos, i -> i.getPart().getLightValue(i.getState()));
        }
        callingLightValue = true;
        int res = max(world, pos, i -> i.getPart().getLightValue(i));
        callingLightValue = false;
        return res;
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        if (target != null) {
            return getTile(world, pos).flatMap(t -> t.get(MCMultiPart.slotRegistry.getValue(target.subHit)))
                    .map(o -> o.getPart().getPickPart(o, (RayTraceResult) target.hitInfo, player)).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isBeaconBase(BlockState state, IWorldReader world, BlockPos pos, BlockPos beacon) {
        return anyMatch(world, pos, i -> i.getPart().isBeaconBase(i, beacon));
    }

    @Override
    public boolean isBurning(BlockState state, IBlockReader world, BlockPos pos) {
        return anyMatch(world, pos, i -> i.getPart().isBurning(i));
    }

    @Override
    public boolean isFertile(BlockState state, IBlockReader world, BlockPos pos) {
        return anyMatch(world, pos, i -> i.getPart().isFertile(i));
    }

    @Override
    public boolean isLadder(BlockState state, IWorldReader world, BlockPos pos, LivingEntity entity) {
        return anyMatch(world, pos, i -> i.getPart().isLadder(i, entity));
    }

    @Override
    public float getPlayerRelativeBlockHardness(BlockState wState, PlayerEntity player, IBlockReader world, BlockPos pos) {
        if (world instanceof World) {
            Pair<Vec3d, Vec3d> vectors = RayTraceHelper.getRayTraceVectors(player);
            RayTraceResult hit = world.func_217296_a(vectors.getLeft(), vectors.getRight(), pos, getRaytraceShape(wState, world, pos), wState);
            if (hit != null) {
                return getTile(world, pos).map(t -> t.get(MCMultiPart.slotRegistry.getValue(hit.subHit)).get())
                        .map(i -> i.getPart().getPlayerRelativePartHardness(i, (RayTraceResult) hit.hitInfo, player)).orElse(0F);
            }
        }
        return 0;
    }

    @Override
    public boolean hasCustomBreakingProgress(BlockState state) {
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean bool) {
        forEach(worldIn, pos, i -> i.getPart().neighborChanged(i, blockIn, fromPos, bool));
    }

    @Override
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        //TODO
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return getTile(world, pos).map(tile -> {
            final VoxelShape[] shape = {VoxelShapes.empty()};
            tile.getParts().values().forEach(part -> shape[0] = VoxelShapes.or(shape[0], part.getPart().getShape(part, context)));
            return shape[0];
        }).orElse(VoxelShapes.empty());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return getTile(world, pos).map(tile -> {
            final VoxelShape[] shape = {VoxelShapes.empty()};
            tile.getParts().values().forEach(part -> shape[0] = VoxelShapes.or(shape[0], part.getPart().getCollisionShape(part, context)));
            return shape[0];
        }).orElse(VoxelShapes.empty());
    }

    private void forEach(IBlockReader world, BlockPos pos, Consumer<PartInfo> consumer) {
        getTile(world, pos).ifPresent(t -> t.getParts().values().forEach(consumer));
    }

    private boolean anyMatch(IBlockReader world, BlockPos pos, Predicate<PartInfo> predicate) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().anyMatch(predicate)).orElse(false);
    }

    private boolean allMatch(IBlockReader world, BlockPos pos, Predicate<PartInfo> predicate) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().allMatch(predicate)).orElse(false);
    }

    private int add(IBlockReader world, BlockPos pos, ToIntFunction<PartInfo> converter, int max) {
        return Math.min(getTile(world, pos).map(t -> t.getParts().values().stream().mapToInt(converter).reduce(0, (a, b) -> a + b)).orElse(0), max);
    }

    private int max(IBlockReader world, BlockPos pos, ToIntFunction<PartInfo> converter) {
        return getTile(world, pos).map(t -> t.getParts().values().stream().mapToInt(converter).max().orElse(0)).orElse(0);
    }

    private float addF(IBlockReader world, BlockPos pos, ToDoubleFunction<PartInfo> converter, double max) {
        return (float) Math.min(getTile(world, pos).map(t -> t.getParts().values().stream().mapToDouble(converter).reduce(0D, (a, b) -> a + b))
                .orElse(0D).floatValue(), max);
    }

}