package mcmultipart.api.slot;

import lombok.experimental.Delegate;
import net.minecraft.util.EnumFacing;

public enum EnumCornerSlot implements IPartSlot, IPartSlot.ICornerSlot {

    CORNER_NNN(new PartSlot.CornerSlot("nnn", EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.NORTH)),
    CORNER_NNP(new PartSlot.CornerSlot("nnp", EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.SOUTH)),
    CORNER_NPN(new PartSlot.CornerSlot("npn", EnumFacing.WEST, EnumFacing.UP, EnumFacing.NORTH)),
    CORNER_NPP(new PartSlot.CornerSlot("npp", EnumFacing.WEST, EnumFacing.UP, EnumFacing.SOUTH)),
    CORNER_PNN(new PartSlot.CornerSlot("pnn", EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.NORTH)),
    CORNER_PNP(new PartSlot.CornerSlot("pnp", EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.SOUTH)),
    CORNER_PPN(new PartSlot.CornerSlot("ppn", EnumFacing.EAST, EnumFacing.UP, EnumFacing.NORTH)),
    CORNER_PPP(new PartSlot.CornerSlot("ppp", EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH));

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
