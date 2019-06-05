package mcmultipart.api.slot;

import lombok.experimental.Delegate;
import mcmultipart.slot.PartSlot;
import net.minecraft.util.EnumFacing;

public enum EnumFaceSlot {
    DOWN(new PartSlot.FaceSlot(EnumFacing.DOWN)),
    UP(new PartSlot.FaceSlot(EnumFacing.UP)),
    NORTH(new PartSlot.FaceSlot(EnumFacing.NORTH)),
    SOUTH(new PartSlot.FaceSlot(EnumFacing.SOUTH)),
    WEST(new PartSlot.FaceSlot(EnumFacing.WEST)),
    EAST(new PartSlot.FaceSlot(EnumFacing.EAST));

    public static final EnumFaceSlot[] VALUES = values();

    @Delegate
    private final PartSlot.FaceSlot slot;

    EnumFaceSlot(PartSlot.FaceSlot slot) {
        this.slot = slot;
    }

    public static EnumFaceSlot fromFace(EnumFacing face) {
        return VALUES[face.ordinal()];
    }

    public EnumFaceSlot getOpposite() {
        return EnumFaceSlot.VALUES[ordinal() ^ 1];
    }

    public PartSlot.FaceSlot getSlot() {
        return slot;
    }
}