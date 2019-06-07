package mcmultipart.api.slot;

import lombok.experimental.Delegate;

public enum EnumCenterSlot {
    CENTER(new PartSlot.CenterSlot());

    @Delegate
    private final PartSlot.CenterSlot slot;

    EnumCenterSlot(PartSlot.CenterSlot slot) {
        this.slot = slot;
    }

    public PartSlot.CenterSlot getSlot() {
        return slot;
    }
}
