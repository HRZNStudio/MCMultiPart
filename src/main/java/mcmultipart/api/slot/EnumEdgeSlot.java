package mcmultipart.api.slot;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.experimental.Delegate;
import net.minecraft.util.Direction;

public enum EnumEdgeSlot {

    EDGE_XNN(new PartSlot.EdgeSlot("xnn", Direction.Axis.X, Direction.DOWN, Direction.NORTH)),
    EDGE_XNP(new PartSlot.EdgeSlot("xnp", Direction.Axis.X, Direction.DOWN, Direction.SOUTH)),
    EDGE_XPN(new PartSlot.EdgeSlot("xpn", Direction.Axis.X, Direction.UP, Direction.NORTH)),
    EDGE_XPP(new PartSlot.EdgeSlot("xpp", Direction.Axis.X, Direction.UP, Direction.SOUTH)),
    EDGE_NYN(new PartSlot.EdgeSlot("nyn", Direction.Axis.Y, Direction.WEST, Direction.NORTH)),
    EDGE_NYP(new PartSlot.EdgeSlot("nyp", Direction.Axis.Y, Direction.WEST, Direction.SOUTH)),
    EDGE_PYN(new PartSlot.EdgeSlot("pyn", Direction.Axis.Y, Direction.EAST, Direction.NORTH)),
    EDGE_PYP(new PartSlot.EdgeSlot("pyp", Direction.Axis.Y, Direction.EAST, Direction.SOUTH)),
    EDGE_NNZ(new PartSlot.EdgeSlot("nnz", Direction.Axis.Z, Direction.WEST, Direction.DOWN)),
    EDGE_NPZ(new PartSlot.EdgeSlot("npz", Direction.Axis.Z, Direction.WEST, Direction.UP)),
    EDGE_PNZ(new PartSlot.EdgeSlot("pnz", Direction.Axis.Z, Direction.EAST, Direction.DOWN)),
    EDGE_PPZ(new PartSlot.EdgeSlot("ppz", Direction.Axis.Z, Direction.EAST, Direction.UP));

    public static final EnumEdgeSlot[] VALUES = values();
    private static final Table<Direction, Direction, EnumEdgeSlot> LOOKUP = HashBasedTable.create();

    @Delegate
    private PartSlot.EdgeSlot slot;

    EnumEdgeSlot(PartSlot.EdgeSlot slot) {
        this.slot = slot;
    }

    public PartSlot.EdgeSlot getSlot() {
        return slot;
    }
}
