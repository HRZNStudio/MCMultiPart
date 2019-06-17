package mcmultipart.api.multipart;

import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IWorldReader;

import java.util.function.Predicate;

public class MultipartOcclusionHelper {

    private static final Predicate<IPartSlot> NEVER = a -> false;

    public static boolean testShapeIntersection(VoxelShape shape1, VoxelShape shape2) {
        return shape1.toBoundingBoxList().stream().anyMatch(b1 -> shape2.toBoundingBoxList().stream().anyMatch(b1::intersects));
    }

    public static boolean testPartIntersection(IPartInfo part1, IPartInfo part2) {
        return part1.getPart().testIntersection(part1, part2) || part2.getPart().testIntersection(part2, part1);
    }

    public static boolean testContainerShapeIntersection(IWorldReader world, BlockPos pos, VoxelShape shape) {
        return testContainerShapeIntersection(world, pos, shape, NEVER);
    }

    public static boolean testContainerShapeIntersection(IWorldReader world, BlockPos pos, VoxelShape shape, Predicate<IPartSlot> ignore) {
        return MultipartHelper.getContainer(world, pos).map(c -> testContainerShapeIntersection(c, shape, ignore)).orElse(false);
    }

    public static boolean testContainerShapeIntersection(IMultipartContainer container, VoxelShape shape) {
        return testContainerShapeIntersection(container, shape, NEVER);
    }

    public static boolean testContainerShapeIntersection(IMultipartContainer container, VoxelShape shape, Predicate<IPartSlot> ignore) {
        return container.getParts().values().stream()
                .filter(i -> !ignore.test(i.getSlot()))
                .anyMatch(i -> testShapeIntersection(i.getPart().getOcclusionShape(i), shape));
    }

    public static boolean testContainerPartIntersection(IWorldReader world, BlockPos pos, IPartInfo part) {
        return testContainerPartIntersection(world, pos, part, NEVER);
    }

    public static boolean testContainerPartIntersection(IWorldReader world, BlockPos pos, IPartInfo part, Predicate<IPartSlot> ignore) {
        return MultipartHelper.getContainer(world, pos).map(c -> testContainerPartIntersection(c, part, ignore)).orElse(false);
    }

    public static boolean testContainerPartIntersection(IMultipartContainer container, IPartInfo part) {
        return testContainerPartIntersection(container, part, NEVER);
    }

    public static boolean testContainerPartIntersection(IMultipartContainer container, IPartInfo part, Predicate<IPartSlot> ignore) {
        return container.getParts().values().stream().filter(i -> !ignore.test(i.getSlot())).anyMatch(i -> testPartIntersection(part, i));
    }

}
