package mcmultipart.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

public abstract class Packet<T extends Packet<T>> {

    public abstract void handleClient(EntityPlayer player);

    public abstract void handleServer(EntityPlayer player);

    public abstract void toBytes(PacketBuffer buf) throws Exception;

    public abstract void fromBytes(PacketBuffer buf) throws Exception;

}
