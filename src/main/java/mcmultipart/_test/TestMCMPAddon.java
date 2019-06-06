package mcmultipart._test;

import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.item.ItemBlockMultipart;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReaderBase;
import net.minecraft.world.World;

@MCMPAddon
public class TestMCMPAddon implements IMCMPAddon {
    @Override
    public void registerParts(IMultipartRegistry registry) {
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