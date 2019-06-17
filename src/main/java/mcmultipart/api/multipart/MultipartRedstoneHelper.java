package mcmultipart.api.multipart;

import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.slot.EnumEdgeSlot;
import mcmultipart.api.slot.SlotUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class MultipartRedstoneHelper {

    // Multipart-only lookups

    public static boolean canConnectRedstone(IMultipartContainer container, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().canConnectRedstone(i, side),
                l -> l.stream().anyMatch(c -> c), false, true, side);
    }

    public static int getWeakPower(IMultipartContainer container, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().getWeakPower(i, side),
                l -> l.stream().max(Integer::compare).orElse(0), 0, true, side);
    }

    public static int getStrongPower(IMultipartContainer container, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().getStrongPower(i, side),
                l -> l.stream().max(Integer::compare).orElse(0), 0, true, side);
    }

    public static boolean canConnectRedstone(IMultipartContainer container, EnumEdgeSlot edge, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().canConnectRedstone(i, side),
                l -> l.stream().anyMatch(c -> c), false, true, edge, side);
    }

    public static int getWeakPower(IMultipartContainer container, EnumEdgeSlot edge, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().getWeakPower(i, side),
                l -> l.stream().max(Integer::compare).orElse(0), 0, true, edge, side);
    }

    public static int getStrongPower(IMultipartContainer container, EnumEdgeSlot edge, Direction side) {
        return SlotUtil.viewContainer(container, i -> i.getPart().getStrongPower(i, side),
                l -> l.stream().max(Integer::compare).orElse(0), 0, true, edge, side);
    }

    // Multipart lookups with world fallback

    public static boolean canConnectRedstone(IWorldReader world, BlockPos pos, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> canConnectRedstone(c, side)).orElseGet(() -> {
            BlockState state = world.getBlockState(pos);
            return state.getBlock().canConnectRedstone(state, world, pos, side.getOpposite());
        });
    }

    public static int getWeakPower(IWorldReader world, BlockPos pos, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> getWeakPower(c, side))
                .orElseGet(() -> world.getBlockState(pos).getWeakPower(world, pos, side.getOpposite()));
    }

    public static int getStrongPower(IWorldReader world, BlockPos pos, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> getStrongPower(c, side))
                .orElseGet(() -> world.getBlockState(pos).getStrongPower(world, pos, side.getOpposite()));
    }

    public static boolean canConnectRedstone(IWorldReader world, BlockPos pos, EnumEdgeSlot edge, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> canConnectRedstone(c, edge, side)).orElseGet(() -> {
            BlockState state = world.getBlockState(pos);
            return state.getBlock().canConnectRedstone(state, world, pos, side.getOpposite());
        });
    }

    public static int getWeakPower(IWorldReader world, BlockPos pos, EnumEdgeSlot edge, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> getWeakPower(c, edge, side))
                .orElseGet(() -> world.getBlockState(pos).getWeakPower(world, pos, side.getOpposite()));
    }

    public static int getStrongPower(IWorldReader world, BlockPos pos, EnumEdgeSlot edge, Direction side) {
        return MultipartHelper.getContainer(world, pos).map(c -> getStrongPower(c, edge, side))
                .orElseGet(() -> world.getBlockState(pos).getStrongPower(world, pos, side.getOpposite()));
    }

}