package mcmultipart.api.slot;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class PartSlot extends ForgeRegistryEntry<IPartSlot> implements IPartSlot {
    public static class CenterSlot extends PartSlot {
        public CenterSlot() {
            setRegistryName("center");
        }

        @Override
        public EnumSlotAccess getFaceAccess(EnumFacing face) {
            return EnumSlotAccess.MERGE;
        }

        @Override
        public int getFaceAccessPriority(EnumFacing face) {
            return -100;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, EnumFacing face) {
            return EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, EnumFacing face) {
            return 0;
        }
    }

    public static class FaceSlot extends PartSlot implements IFaceSlot {
        private final EnumFacing facing;

        FaceSlot(EnumFacing facing) {
            this.facing = facing;
            setRegistryName("face_" + facing.name().toLowerCase());
        }

        public EnumFacing getFacing() {
            return facing;
        }

        @Override
        public EnumSlotAccess getFaceAccess(EnumFacing face) {
            return face == this.getFacing() ? EnumSlotAccess.OVERRIDE
                    : (face != getFacing().getOpposite() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE);
        }

        @Override
        public int getFaceAccessPriority(EnumFacing face) {
            return face == this.getFacing() ? 300 : (face != getFacing().getOpposite() ? 250 : 0);
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, EnumFacing face) {

            return edge.getFace1() == face || edge.getFace2() == face ? EnumSlotAccess.OVERRIDE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, EnumFacing face) {
            return 200;
        }
    }

    public static class CornerSlot extends PartSlot implements ICornerSlot {
        private final EnumFacing face1, face2, face3;

        CornerSlot(String name, EnumFacing face1, EnumFacing face2, EnumFacing face3) {
            setRegistryName("corner_"+name.toLowerCase());
            this.face1 = face1;
            this.face2 = face2;
            this.face3 = face3;
        }

        public EnumFacing getFace1() {
            return face1;
        }

        public EnumFacing getFace2() {
            return face2;
        }

        public EnumFacing getFace3() {
            return face3;
        }

        @Override
        public EnumSlotAccess getFaceAccess(EnumFacing face) {
            return face == getFace1() || face == getFace2() || face == getFace3() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getFaceAccessPriority(EnumFacing face) {
            return 100;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, EnumFacing face) {
            return (edge.getFace1() == getFace1() || edge.getFace1() == getFace2() || edge.getFace1() == getFace3()
                    || edge.getFace2() == getFace1() || edge.getFace2() == getFace2() || edge.getFace2() == getFace3())
                    && (face == getFace1() || face == getFace2() || face == getFace3()) ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, EnumFacing face) {
            return 100;
        }
    }

    public static class EdgeSlot extends PartSlot implements IEdgeSlot {
        private final EnumFacing.Axis axis;
        private final EnumFacing face1, face2;

        EdgeSlot(String name, EnumFacing.Axis axis, EnumFacing face1, EnumFacing face2) {
            setRegistryName("edge_" + name.toLowerCase());
            this.axis = axis;
            this.face1 = face1;
            this.face2 = face2;
        }

        public EnumFacing.Axis getAxis() {
            return axis;
        }

        public EnumFacing getFace1() {
            return face1;
        }

        public EnumFacing getFace2() {
            return face2;
        }

        @Override
        public EnumSlotAccess getFaceAccess(EnumFacing face) {
            return face == getFace1() || face == getFace2() || face.getAxis() == getAxis() ? EnumSlotAccess.MERGE : EnumSlotAccess.NONE;
        }

        @Override
        public int getFaceAccessPriority(EnumFacing face) {
            return 200;
        }

        @Override
        public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, EnumFacing face) {
            return edge.getSlot() == this && (face == getFace1() || face == getFace2()) ? EnumSlotAccess.OVERRIDE : EnumSlotAccess.NONE;
        }

        @Override
        public int getEdgeAccessPriority(EnumEdgeSlot edge, EnumFacing face) {
            return 300;
        }
    }
}