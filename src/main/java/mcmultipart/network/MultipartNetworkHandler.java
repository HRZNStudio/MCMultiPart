package mcmultipart.network;

import mcmultipart.MCMultiPart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.Map;

public class MultipartNetworkHandler {

    private static Map<Triple<Integer, Integer, Integer>, ChangeList> changeList = new HashMap<>();

    public static void init() {
        // wrapper.registerMessage(PacketMultipartChange.class, PacketMultipartChange.class, 0, Side.CLIENT);
        // wrapper.registerMessage(PacketMultipartAdd.class, PacketMultipartAdd.class, 1, Side.CLIENT);
        // wrapper.registerMessage(PacketMultipartRemove.class, PacketMultipartRemove.class, 2, Side.CLIENT);
        MCMultiPart.channel.registerMessage(0, PacketMultipartAction.class, PacketMultipartAction::toBytes, buffer -> {
            PacketMultipartAction action = new PacketMultipartAction();
            action.fromBytes(buffer);
            return action;
        }, (packetMultipartAction, contextSupplier) -> {
            NetworkEvent.Context context = contextSupplier.get();
            if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                packetMultipartAction.handleClient(Minecraft.getInstance().player);
            } else {
                packetMultipartAction.handleServer(context.getSender());
            }
        });
    }

    public static void queuePartChange(World world, MultipartAction action) {
        if (world.isRemote) return;

        int chunkX = action.pos.getX() >> 4;
        int chunkZ = action.pos.getZ() >> 4;

        Triple<Integer, Integer, Integer> key = Triple.of(chunkX, chunkZ, world.getDimension().getType().getId());
        ChangeList cl = changeList.getOrDefault(key, new ChangeList());
        changeList.put(key, cl);

        cl.addChange(action);
    }

    public static void flushChanges() {
        for (Map.Entry<Triple<Integer, Integer, Integer>, ChangeList> list : changeList.entrySet()) {
            flushChanges(list.getKey().getLeft(), list.getKey().getMiddle(), list.getKey().getRight(), list.getValue());
        }
        changeList.clear();
    }

    public static void flushChanges(World world, BlockPos pos) {
        if (world.isRemote) return;
        int chunkX = pos.getX() >> 4;
        int chunkY = pos.getZ() >> 4;
        int dim = world.getDimension().getType().getId();
        Triple<Integer, Integer, Integer> key = Triple.of(chunkX, chunkY, dim);
        ChangeList cl = changeList.get(key);
        if (cl != null) {
            flushChanges(chunkX, chunkY, dim, cl);
            changeList.remove(key);
        }
    }

    private static void flushChanges(int chunkX, int chunkY, int dim, ChangeList list) {
        WorldServer world = DimensionManager.getWorld(LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER), DimensionType.getById(dim), false, false);
        if (world != null) {
            PlayerChunkMap manager = world.getPlayerChunkMap();
            for (EntityPlayer player : world.playerEntities) {
                if (manager.isPlayerWatchingChunk((EntityPlayerMP) player, chunkX, chunkY)) {
                    MCMultiPart.channel.send(PacketDistributor.PLAYER.with(() -> (EntityPlayerMP) player), new PacketMultipartAction(list));
                }
            }
        }
    }

    public static void sendToServer(Packet<?> message) {
        MCMultiPart.channel.sendToServer(message);
    }
}