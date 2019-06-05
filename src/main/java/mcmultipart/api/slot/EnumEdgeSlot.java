package mcmultipart.api.slot;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.experimental.Delegate;
import mcmultipart.slot.PartSlot;
import net.minecraft.util.EnumFacing;

public enum EnumEdgeSlot {

    EDGE_XNN(new PartSlot.EdgeSlot("xnn", EnumFacing.Axis.X, EnumFacing.DOWN, EnumFacing.NORTH)),
    EDGE_XNP(new PartSlot.EdgeSlot("xnp", EnumFacing.Axis.X, EnumFacing.DOWN, EnumFacing.SOUTH)),
    EDGE_XPN(new PartSlot.EdgeSlot("xpn", EnumFacing.Axis.X, EnumFacing.UP, EnumFacing.NORTH)),
    EDGE_XPP(new PartSlot.EdgeSlot("xpp", EnumFacing.Axis.X, EnumFacing.UP, EnumFacing.SOUTH)),
    EDGE_NYN(new PartSlot.EdgeSlot("nyn", EnumFacing.Axis.Y, EnumFacing.WEST, EnumFacing.NORTH)),
    EDGE_NYP(new PartSlot.EdgeSlot("nyp", EnumFacing.Axis.Y, EnumFacing.WEST, EnumFacing.SOUTH)),
    EDGE_PYN(new PartSlot.EdgeSlot("pyn", EnumFacing.Axis.Y, EnumFacing.EAST, EnumFacing.NORTH)),
    EDGE_PYP(new PartSlot.EdgeSlot("pyp", EnumFacing.Axis.Y, EnumFacing.EAST, EnumFacing.SOUTH)),
    EDGE_NNZ(new PartSlot.EdgeSlot("nnz", EnumFacing.Axis.Z, EnumFacing.WEST, EnumFacing.DOWN)),
    EDGE_NPZ(new PartSlot.EdgeSlot("npz", EnumFacing.Axis.Z, EnumFacing.WEST, EnumFacing.UP)),
    EDGE_PNZ(new PartSlot.EdgeSlot("pnz", EnumFacing.Axis.Z, EnumFacing.EAST, EnumFacing.DOWN)),
    EDGE_PPZ(new PartSlot.EdgeSlot("ppz", EnumFacing.Axis.Z, EnumFacing.EAST, EnumFacing.UP));

    public static final EnumEdgeSlot[] VALUES = values();
    private static final Table<EnumFacing, EnumFacing, EnumEdgeSlot> LOOKUP = HashBasedTable.create();

    @Delegate
    private PartSlot.EdgeSlot slot;

    EnumEdgeSlot(PartSlot.EdgeSlot slot) {
        this.slot = slot;
    }

    public PartSlot.EdgeSlot getSlot() {
        return slot;
    }
}
