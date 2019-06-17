package mcmultipart.api.slot;

import net.minecraft.util.Direction;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class PartSlot extends ForgeRegistryEntry<IPartSlot> implements IPartSlot {
    public static class CenterSlot extends PartSlot {
        public CenterSlot() {
            setRegistryName("center");
        }

        @Override
        public EnumSlotAccess getFaceAccess(Direction face) {
            return EnumSlotAccess.MERGE;
        }

        @Override
        public int getFaceAccessPriority(Direction face) {
            return -100;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, Direction face) {
            return EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, Direction face) {
            return 0;
        }
    }

    public static class FaceSlot extends PartSlot implements IFaceSlot {
        private final Direction facing;

        FaceSlot(Direction facing) {
            this.facing = facing;
            setRegistryName("face_" + facing.name().toLowerCase());
        }

        public Direction getFacing() {
            return facing;
        }

        @Override
        public EnumSlotAccess getFaceAccess(Direction face) {
            return face == this.getFacing() ? EnumSlotAccess.OVERRIDE
                    : (face != getFacing().getOpposite() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE);
        }

        @Override
        public int getFaceAccessPriority(Direction face) {
            return face == this.getFacing() ? 300 : (face != getFacing().getOpposite() ? 250 : 0);
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, Direction face) {

            return edge.getFace1() == face || edge.getFace2() == face ? EnumSlotAccess.OVERRIDE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, Direction face) {
            return 200;
        }
    }

    public static class CornerSlot extends PartSlot implements ICornerSlot {
        private final Direction face1, face2, face3;

        CornerSlot(String name, Direction face1, Direction face2, Direction face3) {
            setRegistryName("corner_" + name.toLowerCase());
            this.face1 = face1;
            this.face2 = face2;
            this.face3 = face3;
        }

        public Direction getFace1() {
            return face1;
        }

        public Direction getFace2() {
            return face2;
        }

        public Direction getFace3() {
            return face3;
        }

        @Override
        public EnumSlotAccess getFaceAccess(Direction face) {
            return face == getFace1() || face == getFace2() || face == getFace3() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getFaceAccessPriority(Direction face) {
            return 100;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, Direction face) {
            return (edge.getFace1() == getFace1() || edge.getFace1() == getFace2() || edge.getFace1() == getFace3()
                    || edge.getFace2() == getFace1() || edge.getFace2() == getFace2() || edge.getFace2() == getFace3())
                    && (face == getFace1() || face == getFace2() || face == getFace3()) ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, Direction face) {
            return 100;
        }
    }

    public static class EdgeSlot extends PartSlot implements IEdgeSlot {
        private final Direction.Axis axis;
        private final Direction face1, face2;

        EdgeSlot(String name, Direction.Axis axis, Direction face1, Direction face2) {
            setRegistryName("edge_" + name.toLowerCase());
            this.axis = axis;
            this.face1 = face1;
            this.face2 = face2;
        }

        public Direction.Axis getAxis() {
            return axis;
        }

        public Direction getFace1() {
            return face1;
        }

        public Direction getFace2() {
            return face2;
        }

        @Override
        public EnumSlotAccess getFaceAccess(Direction face) {
            return face == getFace1() || face == getFace2() || face.getAxis() == getAxis() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getFaceAccessPriority(Direction face) {
            return 200;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, Direction face) {
            return edge.getSlot() == this && (face == getFace1() || face == getFace2()) ? EnumSlotAccess.OVERRIDE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, Direction face) {
            return 300;
        }
    }
}