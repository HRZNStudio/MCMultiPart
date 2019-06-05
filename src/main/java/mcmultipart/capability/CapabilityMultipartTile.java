package mcmultipart.capability;

import mcmultipart.api.multipart.IMultipartTile;
import net.minecraft.nbt.INBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityMultipartTile {

    public static void register() {

        CapabilityManager.INSTANCE.register(IMultipartTile.class, new IStorage<IMultipartTile>() {

            @Override
            public INBTBase writeNBT(Capability<IMultipartTile> capability, IMultipartTile instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IMultipartTile> capability, IMultipartTile instance, EnumFacing side, INBTBase nbt) {
            }
        }, () -> null);
    }

}
