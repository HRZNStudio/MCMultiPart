package mcmultipart.api.slot;

import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public interface IPartSlot extends IForgeRegistryEntry<IPartSlot> {

    public EnumSlotAccess getFaceAccess(Direction face);

    public int getFaceAccessPriority(Direction face);

    public EnumSlotAccess getEdgeAccess(EnumEdgeSlot edge, Direction face);

    public int getEdgeAccessPriority(EnumEdgeSlot edge, Direction face);

    @Override
    default IPartSlot setRegistryName(ResourceLocation name) {
        throw new IllegalStateException("Cannot set the registry name of an IPartSlot");
    }

    @Override
    default Class<IPartSlot> getRegistryType() {
        return IPartSlot.class;
    }

    public interface IFaceSlot extends IPartSlot {

    }

    public interface IEdgeSlot extends IPartSlot {

    }

    public interface ICornerSlot extends IPartSlot {

    }

}
