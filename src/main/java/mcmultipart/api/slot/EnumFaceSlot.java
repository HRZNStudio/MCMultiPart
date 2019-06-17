package mcmultipart.api.slot;

import lombok.experimental.Delegate;
import net.minecraft.util.Direction;

public enum EnumFaceSlot {
    DOWN(new PartSlot.FaceSlot(Direction.DOWN)),
    UP(new PartSlot.FaceSlot(Direction.UP)),
    NORTH(new PartSlot.FaceSlot(Direction.NORTH)),
    SOUTH(new PartSlot.FaceSlot(Direction.SOUTH)),
    WEST(new PartSlot.FaceSlot(Direction.WEST)),
    EAST(new PartSlot.FaceSlot(Direction.EAST));

    public static final EnumFaceSlot[] VALUES = values();

    @Delegate
    private final PartSlot.FaceSlot slot;

    EnumFaceSlot(PartSlot.FaceSlot slot) {
        this.slot = slot;
    }

    public static EnumFaceSlot fromFace(Direction face) {
        return VALUES[face.ordinal()];
    }

    public EnumFaceSlot getOpposite() {
        return EnumFaceSlot.VALUES[ordinal() ^ 1];
    }

    public PartSlot.FaceSlot getSlot() {
        return slot;
    }
}