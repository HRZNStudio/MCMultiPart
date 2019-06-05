package mcmultipart.api.item;

import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
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

    public static EnumActionResult place(ItemUseContext context, IBlockPlacementInfo stateProvider, IMultipart multipartBlock,
                                         IBlockPlacementLogic blockLogic, IPartPlacementLogic partLogic) {
        BlockPos pos = context.getPos();
        if (!context.getItem().isEmpty()) {
            float d = Math.abs(context.getHitX() * context.getFace().getXOffset() + context.getHitY() * context.getFace().getYOffset() + context.getHitZ() * context.getFace().getZOffset());
            if (d == 0 || d == 1 || !placeAt(context, stateProvider, multipartBlock,
                    blockLogic, partLogic)) {
                pos = pos.offset(context.getFace());
                if (!placeAt(context, stateProvider, multipartBlock, blockLogic,
                        partLogic)) {
                    return EnumActionResult.FAIL;
                }
            }
            SoundType soundtype = context.getWorld().getBlockState(pos).getBlock().getSoundType(context.getWorld().getBlockState(pos), context.getWorld(), pos, context.getPlayer());
            context.getWorld().playSound(context.getPlayer(), pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F,
                    soundtype.getPitch() * 0.8F);
            if (!context.getPlayer().abilities.isCreativeMode) {
                context.getItem().shrink(1);
            }

            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    public static boolean placeAt(ItemUseContext context, IBlockPlacementInfo stateProvider, IMultipart multipartBlock,
                                  IBlockPlacementLogic blockLogic, IPartPlacementLogic partLogic) {
        BlockItemUseContext blockContext = new BlockItemUseContext(context);
        IBlockState state = stateProvider.getStateForPlacement(blockContext);
        VoxelShape shape = state.getCollisionShape(context.getWorld(), context.getPos());
        if ((shape.isEmpty() || context.getWorld().checkNoEntityCollision(null, shape.withOffset(context.getPos().getX(), context.getPos().getY(), context.getPos().getZ())))
                && blockLogic.place(blockContext, state)) {
            return true;
        }
        shape = multipartBlock.getCollisionShape(state, context.getWorld(), context.getPos());
        return (shape.isEmpty() || context.getWorld().checkNoEntityCollision(null, shape.withOffset(context.getPos().getX(), context.getPos().getY(), context.getPos().getZ())))
                && partLogic.placePart(blockContext, multipartBlock, state);
    }

    public static boolean placePartAt(BlockItemUseContext context, IMultipart multipartBlock, IBlockState state) {
        IPartSlot slot = multipartBlock.getSlotForPlacement(context.getWorld(), context.getPos(), state, context.getFace(), context.getHitX(), context.getHitY(), context.getHitZ(), context.getPlayer());
        if (!multipartBlock.canPlacePartAt(context.getWorld(), context.getPos()) || !multipartBlock.canPlacePartOnSide(context.getWorld(), context.getPos(), context.getFace(), slot))
            return false;

        if (MultipartHelper.addPart(context.getWorld(), context.getPos(), slot, state, false)) {
            if (!context.getWorld().isRemote()) {
                IPartInfo info = MultipartHelper.getContainer(context.getWorld(), context.getPos()).flatMap(c -> c.get(slot)).orElse(null);
                if (info != null) {
                    setMultipartTileNBT(context.getPlayer(), context.getItem(), info);
                    multipartBlock.onPartPlacedBy(info, context.getPlayer(), context.getItem());
                }
            }
            return true;
        }
        return false;
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

    @Override
    public EnumActionResult onItemUse(ItemUseContext context) {
        return place(context, this.getBlock()::getStateForPlacement,
                multipartBlock, this::placeBlockAtTested, ItemBlockMultipart::placePartAt);
    }

    public boolean placeBlockAtTested(BlockItemUseContext context, IBlockState newState) {
        return context.getPlayer().canPlayerEdit(context.getPos(), context.getFace(), context.getItem()) && context.getWorld().getBlockState(context.getPos()).isReplaceable(context)
//                && getBlock().canPlaceBlockAt(world, pos) && getBlock().canPlaceBlockOnSide(world, pos, facing)
                && super.placeBlock(context, newState);
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

        IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer, EnumHand hand, IBlockState state);

    }

}
