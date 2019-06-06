package mcmultipart._test;

import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReaderBase;
import net.minecraftforge.registries.ForgeRegistries;

@MCMPAddon
public class TestMCMPAddon implements IMCMPAddon {
    @Override
    public void registerParts(IMultipartRegistry registry) {
        ForgeRegistries.BLOCKS.getValues().forEach(block -> {
            if(block instanceof BlockSlab) {
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
                registry.registerStackWrapper(block);
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