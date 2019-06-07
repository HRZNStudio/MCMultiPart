package mcmultipart._test;

import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.item.ItemBlockMultipart;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReaderBase;
import net.minecraftforge.registries.ForgeRegistries;

@MCMPAddon
public class TestMCMPAddon implements IMCMPAddon {
    @Override
    public void registerParts(IMultipartRegistry registry) {
        ForgeRegistries.BLOCKS.getValues().forEach(block -> {
            if (block instanceof BlockSlab) {
                registry.registerPartWrapper(block, new IMultipart() {
                    @Override
                    public Block getBlock() {
                        return block;
                    }

                    @Override
                    public IPartSlot getSlotForPlacement(BlockItemUseContext context, IBlockState state) {
                        return state.get(BlockSlab.TYPE) == SlabType.TOP ? EnumFaceSlot.UP.getSlot() : EnumFaceSlot.DOWN.getSlot();
                    }

                    @Override
                    public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state) {
                        return state.get(BlockSlab.TYPE) == SlabType.TOP ? EnumFaceSlot.UP.getSlot() : EnumFaceSlot.DOWN.getSlot();
                    }
                });
                registry.registerStackWrapper(block).setPlacementInfo(new ItemBlockMultipart.IBlockPlacementInfo() {
                    @Override
                    public IBlockState getStateForPlacement(BlockItemUseContext context) {
                        EnumFacing facing = context.getFace();
                        BlockPos pos = !context.replacingClickedOnBlock() ? context.getPos().offset(context.getFace().getOpposite()) : context.getPos();
                        float hitX = context.getHitX();
                        float hitY = context.getHitY() - pos.getY();
                        float hitZ = context.getHitZ();
                        IBlockState state = block.getStateForPlacement(context);
                        double x = Math.abs(hitY * facing.getYOffset());
                        boolean top = state.get(BlockSlab.TYPE) == SlabType.TOP;
                        return facing.getAxis() == EnumFacing.Axis.Y && x == 0.5D ? state.with(BlockSlab.TYPE, top ? SlabType.BOTTOM : SlabType.TOP) : state;
                    }
                }).setPartPlacementLogic((context, multipartBlock, state) -> {
                    IPartSlot slot = multipartBlock.getSlotForPlacement(context, state);
                    EnumFacing facing = context.getFace();
                    float hitX = context.getHitX() % 1;
                    float hitY = context.getHitY() % 1;
                    float hitZ = context.getHitZ() % 1;
                    double x = Math.abs((hitX * facing.getXOffset()) + (hitY * facing.getYOffset()) + (hitZ * facing.getZOffset()));
                    if (MultipartHelper.addPart(context.getWorld(), !context.replacingClickedOnBlock() && x != 0 ? context.getPos().offset(context.getFace().getOpposite()) : context.getPos(), slot, state, context.getWorld().isRemote)) {
                        SoundType soundtype = state.getSoundType(context.getWorld(), context.getPos(), context.getPlayer());
                        context.getWorld().playSound(context.getPlayer(), context.getPos(), soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                        return true;
                    }
                    return false;
                });
            }
        });
        registry.registerPartWrapper(Blocks.TORCH, new IMultipart() {
            @Override
            public Block getBlock() {
                return Blocks.TORCH;
            }

            @Override
            public IPartSlot getSlotForPlacement(BlockItemUseContext context, IBlockState state) {
                return EnumFaceSlot.DOWN.getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state) {
                return EnumFaceSlot.DOWN.getSlot();
            }
        });
        registry.registerPartWrapper(Blocks.WALL_TORCH, new IMultipart() {
            @Override
            public Block getBlock() {
                return Blocks.WALL_TORCH;
            }

            @Override
            public IPartSlot getSlotForPlacement(BlockItemUseContext context, IBlockState state) {
                return EnumFaceSlot.fromFace(state.get(BlockTorchWall.HORIZONTAL_FACING)).getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state) {
                return EnumFaceSlot.fromFace(state.get(BlockTorchWall.HORIZONTAL_FACING)).getSlot();
            }
        });
        registry.registerStackWrapper(Blocks.TORCH.asItem(), Blocks.WALL_TORCH).setPlacementInfo(context -> {
            IBlockState wall_state = Blocks.WALL_TORCH.getStateForPlacement(context);
            IBlockState finalState = null;
            IWorldReaderBase world = context.getWorld();
            BlockPos pos = context.getPos();
            EnumFacing[] directions = context.getNearestLookingDirections();

            for (EnumFacing facing : directions) {
                if (facing != EnumFacing.UP) {
                    IBlockState lvt_10_1_ = facing == EnumFacing.DOWN ? Blocks.TORCH.getStateForPlacement(context) : wall_state;
                    if (lvt_10_1_ != null && lvt_10_1_.isValidPosition(world, pos)) {
                        finalState = lvt_10_1_;
                        break;
                    }
                }
            }

            return finalState != null && world.checkNoEntityCollision(finalState, pos) ? finalState : null;
        });
    }
}