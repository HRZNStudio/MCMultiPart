package mcmultipart.api.multipart;

import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.world.IWorldView;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public interface IMultipart {

    default Block asBlock() {
        if (!(this instanceof Block)) {
            throw new IllegalStateException("This multipart isn't a Block. Override IMultipart#asBlock()!");
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
                        "The block " + asBlock().getRegistryName() + " is multipart-compatible but its TileEntity isn't!"));
    }

    IPartSlot getSlotForPlacement(BlockItemUseContext context, BlockState state);

    IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, BlockState state);

    default VoxelShape getShape(IPartInfo i, ISelectionContext context) {
        return asBlock().getShape(i.getState(), i.getPartWorld(), i.getPartPos(), context);
    }

    default VoxelShape getOcclusionShape(IPartInfo i) {
        return getShape(i, ISelectionContext.dummy());
    }

    default VoxelShape getCollisionShape(PartInfo i, ISelectionContext context) {
        return asBlock().getCollisionShape(i.getState(), i.getPartWorld(), i.getPartPos(), context);
    }

    default VoxelShape getRenderShape(PartInfo i) {
        return asBlock().getRenderShape(i.getState(), i.getPartWorld(), i.getPartPos());
    }

    default boolean testIntersection(IPartInfo part1, IPartInfo part2) {
        return MultipartOcclusionHelper.testShapeIntersection(part1.getPart().getOcclusionShape(part1), part2.getPart().getOcclusionShape(part2));
    }

    @Nullable
    default IMultipartTile createMultipartTile(World world, IPartSlot slot, BlockState state) {
        TileEntity tile = state.getBlock().createTileEntity(state, world);
        return tile == null ? null : convertToMultipartTile(tile);
    }

    default boolean canConnectRedstone(IPartInfo i, Direction side) {
        return asBlock().canConnectRedstone(i.getState(), i.getPartWorld(), i.getPartPos(), side);
    }

    default int getStrongPower(IPartInfo i, Direction side) {
        return asBlock().getStrongPower(i.getState(), i.getPartWorld(), i.getPartPos(), side);
    }

    default int getWeakPower(IPartInfo i, Direction side) {
        return asBlock().getWeakPower(i.getState(), i.getPartWorld(), i.getPartPos(), side);
    }

    default void neighborChanged(PartInfo i, Block blockIn, BlockPos fromPos, boolean bool) {
        asBlock().neighborChanged(i.getState(), i.getPartWorld(), i.getPartPos(), blockIn, fromPos, bool);
    }

    default float getPlayerRelativePartHardness(IPartInfo i, RayTraceResult hitInfo, PlayerEntity player) {
        return asBlock().getPlayerRelativeBlockHardness(i.getState(), player, i.getPartWorld(), i.getPartPos());
    }

    default boolean isLadder(PartInfo i, LivingEntity entity) {
        return asBlock().isLadder(i.getState(), i.getPartWorld(), i.getPartPos(), entity);
    }

    default ItemStack getPickPart(IPartInfo i, RayTraceResult target, PlayerEntity player) {
        return asBlock().getPickBlock(i.getState(), target, i.getPartWorld(), i.getPartPos(), player);
    }

    default boolean canCreatureSpawn(PartInfo i, EntitySpawnPlacementRegistry.PlacementType type, EntityType<?> entity) {
        return asBlock().canCreatureSpawn(i.getState(), i.getPartWorld(), i.getPartPos(), type, entity);
    }
}