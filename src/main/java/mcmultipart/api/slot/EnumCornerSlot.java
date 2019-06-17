package mcmultipart.api.slot;

import lombok.experimental.Delegate;
import net.minecraft.util.Direction;

public enum EnumCornerSlot implements IPartSlot, IPartSlot.ICornerSlot {

    CORNER_NNN(new PartSlot.CornerSlot("nnn", Direction.WEST, Direction.DOWN, Direction.NORTH)),
    CORNER_NNP(new PartSlot.CornerSlot("nnp", Direction.WEST, Direction.DOWN, Direction.SOUTH)),
    CORNER_NPN(new PartSlot.CornerSlot("npn", Direction.WEST, Direction.UP, Direction.NORTH)),
    CORNER_NPP(new PartSlot.CornerSlot("npp", Direction.WEST, Direction.UP, Direction.SOUTH)),
    CORNER_PNN(new PartSlot.CornerSlot("pnn", Direction.EAST, Direction.DOWN, Direction.NORTH)),
    CORNER_PNP(new PartSlot.CornerSlot("pnp", Direction.EAST, Direction.DOWN, Direction.SOUTH)),
    CORNER_PPN(new PartSlot.CornerSlot("ppn", Direction.EAST, Direction.UP, Direction.NORTH)),
    CORNER_PPP(new PartSlot.CornerSlot("ppp", Direction.EAST, Direction.UP, Direction.SOUTH));

    public static final EnumCornerSlot[] VALUES = values();

    @Delegate
    private final PartSlot.CornerSlot slot;

    EnumCornerSlot(PartSlot.CornerSlot slot) {
        this.slot = slot;
    }

    public PartSlot.CornerSlot getSlot() {
        return slot;
    }
}
