package mcmultipart;

import com.google.common.collect.Lists;
import mcmultipart.api.item.ItemBlockMultipart;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.multipart.MultipartRegistry.WrappedBlock;
import mcmultipart.network.MultipartNetworkHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public class MCMPCommonProxy {

    public void preInit() {
    }

    public void init() {
    }

    public PlayerEntity getPlayer() {
        return null;
    }

    public NetworkManager getNetworkManager() {
        return null;
    }

    public void scheduleTick(Runnable runnable, Dist side) {
        if (side == Dist.DEDICATED_SERVER) {
            ((MinecraftServer) LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER)).func_213207_aT().execute(runnable);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            MultipartNetworkHandler.flushChanges();
        }
    }

    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getEntityPlayer();
        if (event.getWorld() == null || event.getFace() == null || player == null) {
            return;
        }
        ItemStack stack = player.getHeldItem(event.getHand());
        if (!stack.isEmpty()) {
            Pair<WrappedBlock, IMultipart> info = MultipartRegistry.INSTANCE.wrapPlacement(stack);
            BlockRayTraceResult result = (BlockRayTraceResult) player.func_213324_a(10, 0, false);
            if (result.getType() != BlockRayTraceResult.Type.MISS && result.getHitVec() != null) {
                if (info != null && info.getKey().getBlockPlacementLogic() != null) {
                    ActionResultType r = placePart(new BlockItemUseContext(new ItemUseContext(player, event.getHand(), result)), info);
                    if (r != ActionResultType.PASS) {
                        event.setCancellationResult(r);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    private ActionResultType placePart(BlockItemUseContext context, @Nonnull Pair<WrappedBlock, IMultipart> info) {
        ItemStack itemstack = context.getItem();
        int size = itemstack.getCount();
        CompoundNBT nbt = null;
        if (itemstack.hasTag()) {
            nbt = itemstack.getTag().copy();
        }

        if (!(itemstack.getItem() instanceof BucketItem)) // if not bucket
        {
            context.getWorld().captureBlockSnapshots = true;
        }
        ActionResultType ret = ItemBlockMultipart.place(context, info.getKey().getPlacementInfo(), info.getValue(),
                info.getKey().getBlockPlacementLogic(), info.getKey().getPartPlacementLogic());
        context.getWorld().captureBlockSnapshots = false;

        if (ret == ActionResultType.SUCCESS) {
            // save new item data
            int newSize = itemstack.getCount();
            CompoundNBT newNBT = null;
            if (itemstack.getTag() != null) {
                newNBT = itemstack.getTag().copy();
            }
            boolean placeEvent = false;
            @SuppressWarnings("unchecked")
            List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>) context.getWorld().capturedBlockSnapshots.clone();
            context.getWorld().capturedBlockSnapshots.clear();

            // make sure to set pre-placement item data for event
            itemstack.setCount(size);
            if (nbt != null) {
                itemstack.setTag(nbt);
            }
            if (blockSnapshots.size() > 1) {
                placeEvent = ForgeEventFactory.onMultiBlockPlace(context.getPlayer(), blockSnapshots, context.getFace());
            } else if (blockSnapshots.size() == 1) {
                placeEvent = ForgeEventFactory.onBlockPlace(context.getPlayer(), blockSnapshots.get(0), context.getFace());
            }

            if (placeEvent) {
                ret = ActionResultType.FAIL; // cancel placement
                // revert back all captured blocks
                for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots)) {
                    context.getWorld().restoringBlockSnapshots = true;
                    blocksnapshot.restore(true, false);
                    context.getWorld().restoringBlockSnapshots = false;
                }
            } else {
                // Change the stack to its new content
                itemstack.setCount(newSize);
                if (nbt != null) {
                    itemstack.setTag(newNBT);
                }

                for (BlockSnapshot snap : blockSnapshots) {
                    int updateFlag = snap.getFlag();
                    BlockState oldBlock = snap.getReplacedBlock();
                    BlockState newBlock = context.getWorld().getBlockState(snap.getPos());
                    if (!newBlock.getBlock().hasTileEntity(newBlock)) // Containers get placed automatically
                    {
                        newBlock.getBlock().onBlockAdded(newBlock, context.getWorld(), snap.getPos(), oldBlock, false);
                    }

                    context.getWorld().markAndNotifyBlock(snap.getPos(), null, oldBlock, newBlock, updateFlag);
                }
                context.getPlayer().addStat(Stats.ITEM_USED.get(itemstack.getItem()));
            }
        }
        context.getWorld().capturedBlockSnapshots.clear();

        return ret;
    }

}
