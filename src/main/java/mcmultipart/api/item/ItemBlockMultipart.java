package mcmultipart.api.item;

import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockMultipart extends ItemBlock {

    protected final IMultipart multipartBlock;

    public ItemBlockMultipart(Block block, Item.Properties properties, IMultipart multipartBlock) {
        super(block, properties);
        this.multipartBlock = multipartBlock;
    }

    public ItemBlockMultipart(IMultipart multipartBlock, Item.Properties properties) {
        this(multipartBlock.getBlock(), properties, multipartBlock);
    }

    public static boolean setMultipartTileNBT(EntityPlayer player, ItemStack stack, IPartInfo info) {
        World world = info.getActualWorld();
        BlockPos pos = info.getPartPos();

        MinecraftServer server = world.getServer();

        if (server == null) {
            return false;
        } else {
            NBTTagCompound tag = stack.getOrCreateChildTag("BlockEntityTag");

            if (tag != null) {
                IMultipartTile tile = info.getTile();

                if (tile != null) {
                    if (!world.isRemote && tile.onlyOpsCanSetPartNbt() && (player == null || !player.canUseCommandBlock())) {
                        return false;
                    }

                    NBTTagCompound tag1 = tile.writePartToNBT(new NBTTagCompound());
                    NBTTagCompound tag2 = tag1.copy();
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

    public static EnumActionResult place(BlockItemUseContext context, IBlockPlacementInfo placementInfo, IMultipart value, IBlockPlacementLogic blockPlacementLogic, IPartPlacementLogic partPlacementLogic) {
        BlockPos pos = context.getPos();
        World world =context.getWorld();
        if(!world.isRemote) {
            IBlockState state = placementInfo.getStateForPlacement(context);
            value = MultipartRegistry.INSTANCE.getPart(state.getBlock());
            IPartSlot slot = value.getSlotForPlacement(context, state);
            if (MultipartHelper.addPart(world, pos, slot, state, false)) {
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    @Override
    protected boolean placeBlock(BlockItemUseContext context, IBlockState iblockstate) {
        return context.canPlace() && super.placeBlock(context, iblockstate);
    }

    @Override
    public EnumActionResult tryPlace(BlockItemUseContext context) {
        IBlockState iblockstate = this.getStateForPlacement(context);

        if (iblockstate!=null&&placeBlock(context, iblockstate)) {
            BlockPos blockpos = context.getPos();
            World world = context.getWorld();
            EntityPlayer entityplayer = context.getPlayer();
            ItemStack itemstack = context.getItem();
            IBlockState iblockstate1 = world.getBlockState(blockpos);
            Block block = iblockstate1.getBlock();
            if (block == iblockstate.getBlock()) {
                this.onBlockPlaced(blockpos, world, entityplayer, itemstack, iblockstate1);
                block.onBlockPlacedBy(world, blockpos, iblockstate1, entityplayer, itemstack);
                if (entityplayer instanceof EntityPlayerMP) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP)entityplayer, blockpos, itemstack);
                }
            }

            SoundType soundtype = iblockstate1.getSoundType(world, blockpos, context.getPlayer());
            world.playSound(entityplayer, blockpos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            itemstack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    public interface IPartPlacementLogic {

        boolean placePart(BlockItemUseContext context, IMultipart multipartBlock, IBlockState state);

    }

    public interface IBlockPlacementLogic {

        boolean place(BlockItemUseContext context, IBlockState newState);

    }

    public interface IBlockPlacementInfo {
        IBlockState getStateForPlacement(BlockItemUseContext context);

    }

    public interface IExtendedBlockPlacementInfo {

        IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer, IBlockState state);

    }

}
