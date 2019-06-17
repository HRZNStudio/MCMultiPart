package mcmultipart.api.item;

import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockMultipart extends BlockItem {

    protected final IMultipart multipartBlock;

    public ItemBlockMultipart(Block block, Item.Properties properties, IMultipart multipartBlock) {
        super(block, properties);
        this.multipartBlock = multipartBlock;
    }

    public ItemBlockMultipart(IMultipart multipartBlock, Item.Properties properties) {
        this(multipartBlock.asBlock(), properties, multipartBlock);
    }

    public static boolean setMultipartTileNBT(PlayerEntity player, ItemStack stack, IPartInfo info) {
        World world = info.getActualWorld();
        BlockPos pos = info.getPartPos();

        MinecraftServer server = world.getServer();

        if (server == null) {
            return false;
        } else {
            CompoundNBT tag = stack.getOrCreateChildTag("BlockEntityTag");

            if (tag != null) {
                IMultipartTile tile = info.getTile();

                if (tile != null) {
                    if (!world.isRemote && tile.onlyOpsCanSetPartNbt() && (player == null || !player.canUseCommandBlock())) {
                        return false;
                    }

                    CompoundNBT tag1 = tile.writePartToNBT(new CompoundNBT());
                    CompoundNBT tag2 = tag1.copy();
                    tag1.merge(tag);
                    tag1.putInt("x", pos.getX());
                    tag1.putInt("y", pos.getY());
                    tag1.putInt("z", pos.getZ());

                    if (!tag1.equals(tag2)) {
                        tile.readPartFromNBT(tag1);
                        tile.markPartDirty();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static boolean place(BlockItemUseContext context, IMultipart value, BlockState state) {
        IPartSlot slot = value.getSlotForPlacement(context, state);
        if (MultipartHelper.addPart(context.getWorld(), context.getPos(), slot, state, context.getWorld().isRemote)) {
            SoundType soundtype = state.getSoundType(context.getWorld(), context.getPos(), context.getPlayer());
            context.getWorld().playSound(context.getPlayer(), context.getPos(), soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            return true;
        }
        return false;
    }

    public static ActionResultType place(BlockItemUseContext context, IBlockPlacementInfo placementInfo, IMultipart value, IBlockPlacementLogic blockPlacementLogic, IPartPlacementLogic partPlacementLogic) {
        BlockState state = placementInfo.getStateForPlacement(context);
        if (state == null)
            return ActionResultType.PASS;
        value = MultipartRegistry.INSTANCE.getPart(state.getBlock());
        return partPlacementLogic.placePart(context, value, state) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    protected boolean placeBlock(BlockItemUseContext context, BlockState BlockState) {
        return context.canPlace() && super.placeBlock(context, BlockState);
    }

    @Override
    public ActionResultType tryPlace(BlockItemUseContext context) {
        BlockState BlockState = this.getStateForPlacement(context);

        if (BlockState != null && placeBlock(context, BlockState)) {
            BlockPos blockpos = context.getPos();
            World world = context.getWorld();
            PlayerEntity PlayerEntity = context.getPlayer();
            ItemStack itemstack = context.getItem();
            BlockState BlockState1 = world.getBlockState(blockpos);
            Block block = BlockState1.getBlock();
            if (block == BlockState.getBlock()) {
                this.onBlockPlaced(blockpos, world, PlayerEntity, itemstack, BlockState1);
                block.onBlockPlacedBy(world, blockpos, BlockState1, PlayerEntity, itemstack);
                if (PlayerEntity instanceof ServerPlayerEntity) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) PlayerEntity, blockpos, itemstack);
                }
            }

            SoundType soundtype = BlockState1.getSoundType(world, blockpos, context.getPlayer());
            world.playSound(PlayerEntity, blockpos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            itemstack.shrink(1);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    public interface IPartPlacementLogic {
        boolean placePart(BlockItemUseContext context, IMultipart multipartBlock, BlockState state);
    }

    public interface IBlockPlacementLogic {
        boolean place(BlockItemUseContext context, BlockState newState);
    }

    public interface IBlockPlacementInfo {
        BlockState getStateForPlacement(BlockItemUseContext context);
    }
}