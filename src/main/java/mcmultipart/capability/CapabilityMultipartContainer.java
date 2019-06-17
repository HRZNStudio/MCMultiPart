package mcmultipart.capability;

import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.block.TileMultipartContainer;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityMultipartContainer {

    public static void register() {
        CapabilityManager.INSTANCE.register(IMultipartContainer.class, new IStorage<IMultipartContainer>() {

            @Override
            public INBT writeNBT(Capability<IMultipartContainer> capability, IMultipartContainer instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IMultipartContainer> capability, IMultipartContainer instance, Direction side, INBT nbt) {
            }
        }, TileMultipartContainer::new);
    }

}
