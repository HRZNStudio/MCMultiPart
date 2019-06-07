package mcmultipart.api.multipart;

import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.world.IWorldView;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("deprecation")
public interface IMultipart {

    default Block getBlock() {
        if (!(this instanceof Block)) {
            throw new IllegalStateException("This multipart isn't a Block. Override IMultipart#getBlock()!");
        }
        return (Block) this;
    }

    default boolean shouldWrapWorld() {
        return true;
    }

    default IWorldView getWorldView(IPartInfo part) {
        return IWorldView.getDefaultFor(part);
    }

    default IMultipartTile convertToMultipartTile(TileEntity tileEntity) {
        return tileEntity.getCapability(MCMPCapabilities.MULTIPART_TILE)
                .orElseThrow(() -> new IllegalStateException(
                        "The block " + getBlock().getRegistryName() + " is multipart-compatible but its TileEntity isn't!"));
    }

    default VoxelShape getCollisionShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.getCollisionShape(worldIn, pos);
    }

    default VoxelShape getRenderShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.getRenderShape(worldIn, pos);
    }

    default VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.getShape(worldIn, pos);
    }

    default VoxelShape getRaytraceShape(IBlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.getRaytraceShape(worldIn, pos);
    }

    default IMultipartTile createMultipartTile(World world, IPartSlot slot, IBlockState state) {
        TileEntity tileEntity = state.getBlock().createTileEntity(state, world);
        return tileEntity != null ? convertToMultipartTile(tileEntity) : null;
    }

    default IMultipartTile loadMultipartTile(World world, NBTTagCompound tag) {
        TileEntity tile = TileEntity.create(tag);
        tile.setWorld(world);
        return convertToMultipartTile(tile);
    }

    IPartSlot getSlotForPlacement(BlockItemUseContext context, IBlockState state);

    IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state);

    default Set<IPartSlot> getGhostSlots(IPartInfo part) {
        return Collections.emptySet();
    }

    default List<AxisAlignedBB> getOcclusionBoxes(IPartInfo part) {
        return part.getState().getShape(part.getPartWorld(), part.getPartPos()).toBoundingBoxList(); // TODO: Maybe replace bounding boxes with shapes
    }

    default boolean testIntersection(IPartInfo self, IPartInfo otherPart) {
        return MultipartOcclusionHelper.testBoxIntersection(this.getOcclusionBoxes(self), otherPart.getPart().getOcclusionBoxes(otherPart));
    }

    default boolean canRenderInLayer(IPartInfo part, IBlockState state, BlockRenderLayer layer) {
        return state.getBlock().canRenderInLayer(state, layer);
    }

    default void onPartPlacedBy(IPartInfo part, EntityLivingBase placer, ItemStack stack) {
        part.getState().getBlock().onBlockPlacedBy(part.getPartWorld(), part.getPartPos(), part.getState(), placer, stack);
    }

    default boolean addDestroyEffects(IPartInfo part, ParticleManager manager) {
        return part.getState().getBlock().addDestroyEffects(part.getState(), part.getPartWorld(), part.getPartPos(), manager);
    }

    default boolean addHitEffects(IPartInfo part, RayTraceResult hit, ParticleManager manager) {
        return part.getState().getBlock().addHitEffects(part.getState(), part.getPartWorld(), hit, manager);
    }

    default EnumBlockRenderType getRenderType(IPartInfo part) {
        return part.getState().getRenderType();
    }


    default boolean canConnectRedstone(IPartInfo part, EnumFacing side) {
        return part.getState().getBlock().canConnectRedstone(part.getState(), part.getPartWorld(), part.getPartPos(), side);
    }

    default int getWeakPower(IPartInfo part, EnumFacing side) {
        return part.getState().getWeakPower(part.getPartWorld(), part.getPartPos(), side);
    }

    default int getStrongPower(IPartInfo part, EnumFacing side) {
        return part.getState().getStrongPower(part.getPartWorld(), part.getPartPos(), side);
    }

    default boolean canCreatureSpawn(IPartInfo part, EntitySpawnPlacementRegistry.SpawnPlacementType type, EntityType<? extends EntityLiving> entity) {
        return part.getState().getBlock().canCreatureSpawn(part.getState(), part.getPartWorld(), part.getPartPos(), type, entity);
    }

    default boolean canSustainPlant(IPartInfo part, EnumFacing direction, IPlantable plantable) {
        return part.getState().getBlock().canSustainPlant(part.getState(), part.getPartWorld(), part.getPartPos(), direction, plantable);
    }

    default void fillWithRain(IPartInfo part) {
        part.getState().getBlock().fillWithRain(part.getPartWorld(), part.getPartPos());
    }

    default int getComparatorInputOverride(IPartInfo part) {
        return part.getState().getComparatorInputOverride(part.getPartWorld(), part.getPartPos());
    }

    default void getDrops(NonNullList<ItemStack> list, IPartInfo part, int fortune) {
        part.getState().getDrops(list, part.getPartWorld(), part.getPartPos(), fortune);
    }

    default float getExplosionResistance(IPartInfo part, Entity exploder, Explosion explosion) {
        return part.getState().getBlock().getExplosionResistance(part.getState(), part.getPartWorld(), part.getPartPos(), exploder, explosion);
    }

    default float getEnchantPowerBonus(IPartInfo part) {
        return part.getState().getBlock().getEnchantPowerBonus(part.getState(), part.getPartWorld(), part.getPartPos());
    }

    default int getLightOpacity(IPartInfo part) {
        return part.getState().getOpacity(part.getPartWorld(), part.getPartPos());
    }

    default int getLightValue(IPartInfo part) {
        return part.getState().getLightValue(part.getPartWorld(), part.getPartPos());
    }

    default int getLightValue(IBlockState state) {
        return state.getLightValue();
    }

    default ItemStack getPickPart(IPartInfo part, RayTraceResult hit, EntityPlayer player) {
        return part.getState().getBlock().getPickBlock(part.getState(), hit, part.getPartWorld(), part.getPartPos(), player);
    }

    default float getPlayerRelativePartHardness(IPartInfo part, RayTraceResult hit, EntityPlayer player) {
        return part.getState().getPlayerRelativeBlockHardness(player, part.getPartWorld(), part.getPartPos());
    }

    default boolean isBeaconBase(IPartInfo part, BlockPos beacon) {
        return part.getState().getBlock().isBeaconBase(part.getState(), part.getPartWorld(), part.getPartPos(), beacon);
    }

    default boolean isBurning(IPartInfo part) {
        return part.getState().getBlock().isBurning(part.getState(), part.getPartWorld(), part.getPartPos());
    }

    default boolean isFertile(IPartInfo part) {
        return part.getState().getBlock().isFertile(part.getState(), part.getPartWorld(), part.getPartPos());
    }

    default boolean isFireSource(IPartInfo part, EnumFacing side) {
        return part.getState().getBlock().isFireSource(part.getState(), part.getPartWorld(), part.getPartPos(), side);
    }

    default boolean isFlammable(IPartInfo part, EnumFacing face) {
        return part.getState().getBlock().isFlammable(part.getState(), part.getPartWorld(), part.getPartPos(), face);
    }

    default boolean isFoliage(IPartInfo part) {
        return part.getState().getBlock().isFoliage(part.getState(), part.getPartWorld(), part.getPartPos());
    }

    default boolean isLadder(IPartInfo part, EntityLivingBase entity) {
        return part.getState().getBlock().isLadder(part.getState(), part.getPartWorld(), part.getPartPos(), entity);
    }


    default void onPartClicked(IPartInfo part, EntityPlayer player, RayTraceResult hit) {
        part.getState().getBlock().onBlockClicked(part.getState(), part.getPartWorld(), part.getPartPos(), player);
    }

    default void neighborChanged(IPartInfo part, Block neighborBlock, BlockPos neighborPos) {
        part.getState().neighborChanged(part.getPartWorld(), part.getPartPos(), neighborBlock, neighborPos);
    }

    default void onNeighborChange(IPartInfo part, BlockPos neighbor) {
        part.getState().getBlock().onNeighborChange(part.getState(), part.getPartWorld(), part.getPartPos(), neighbor);
    }

    default boolean onPartActivated(IPartInfo part, EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        return part.getState().getBlock().onBlockActivated(part.getState(), part.getPartWorld(), part.getPartPos(), player, hand,
                hit.sideHit, (float) hit.hitVec.x - hit.getBlockPos().getX(), (float) hit.hitVec.y - hit.getBlockPos().getY(),
                (float) hit.hitVec.z - hit.getBlockPos().getZ());
    }

    default void onPlantGrow(IPartInfo part, BlockPos source) {
        part.getState().getBlock().onPlantGrow(part.getState(), part.getPartWorld(), part.getPartPos(), source);
    }

    default boolean canPlayerDestroy(IPartInfo part, EntityPlayer player) {
        return true;
    }

    default void onPartHarvested(IPartInfo part, EntityPlayer player) {
        part.getState().getBlock().onBlockHarvested(part.getPartWorld(), part.getPartPos(), part.getState(), player);
    }

    default void randomTick(IPartInfo part, Random random) {
        part.getState().getBlock().randomTick(part.getState(), part.getPartWorld(), part.getPartPos(), random);
    }

    default BlockFaceShape getPartFaceShape(IPartInfo part, EnumFacing face) {
        return part.getState().getBlockFaceShape(part.getPartWorld(), part.getPartPos(), face);
    }

    default void onAdded(IPartInfo part) {
    }

    default void onRemoved(IPartInfo part) {
    }

    default boolean canPlacePartAt(World world, BlockPos pos) {
        return true;
    }

    default boolean canPlacePartOnSide(World world, BlockPos pos, EnumFacing side, IPartSlot slot) {
        return canPlacePartAt(world, pos);
    }

    default void onPartAdded(IPartInfo part, IPartInfo otherPart) {
        onPartChanged(part, otherPart);
    }

    default void onPartRemoved(IPartInfo part, IPartInfo otherPart) {
        onPartChanged(part, otherPart);
    }

    default void onPartChanged(IPartInfo part, IPartInfo otherPart) {
    }

    default void tick(IPartInfo part, Random rand) {
        part.getState().getBlock().tick(part.getState(), part.getPartWorld(), part.getPartPos(), rand);
    }

    default void dropPartAsItemWithChance(IPartInfo part, float chance, int fortune) {
        part.getState().getBlock().dropBlockAsItemWithChance(part.getState(), part.getPartWorld(), part.getPartPos(), chance, fortune);
    }

    default RayTraceResult getRayTraceResult(PartInfo info, Vec3d start, Vec3d end, RayTraceResult original) {
        return info.getPart().getBlock().getRayTraceResult(info.getState(), info.getPartWorld(), info.getPartPos(), start, end, original);
    }

    default IBlockState updatePostPlacement(PartInfo i, EnumFacing face, IBlockState facingState, BlockPos facingPos) {
        return i.getState().updatePostPlacement(face, facingState, i.getPartWorld(), i.getPartPos(), facingPos);
    }
}