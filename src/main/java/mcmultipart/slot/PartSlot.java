package mcmultipart.slot;

import mcmultipart.api.slot.EnumEdgeSlot;
import mcmultipart.api.slot.EnumSlotAccess;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class PartSlot extends ForgeRegistryEntry<IPartSlot> implements IPartSlot {
    public static class FaceSlot extends PartSlot implements IFaceSlot {
        private final EnumFacing facing;

        public FaceSlot(EnumFacing facing) {
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

    public static class EdgeSlot extends PartSlot implements IEdgeSlot {
        private final EnumFacing.Axis axis;
        private final EnumFacing face1, face2;

        public EdgeSlot(String name, EnumFacing.Axis axis, EnumFacing face1, EnumFacing face2) {
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