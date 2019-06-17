package mcmultipart.capability;

import mcmultipart.api.multipart.IMultipartTile;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityMultipartTile {

    public static void register() {

        CapabilityManager.INSTANCE.register(IMultipartTile.class, new IStorage<IMultipartTile>() {

            @Override
            public INBT writeNBT(Capability<IMultipartTile> capability, IMultipartTile instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IMultipartTile> capability, IMultipartTile instance, Direction side, INBT nbt) {
            }
        }, () -> null);
    }

}
