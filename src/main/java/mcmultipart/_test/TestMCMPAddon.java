package mcmultipart._test;

import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

@MCMPAddon
public class TestMCMPAddon implements IMCMPAddon {
    @Override
    public void registerParts(IMultipartRegistry registry) {
        registry.registerPartWrapper(Blocks.TORCH, new IMultipart() {
            @Override
            public IPartSlot getSlotForPlacement(World world, BlockPos pos, IBlockState state, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer) {
                return EnumFaceSlot.DOWN.getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state) {
                return EnumFaceSlot.DOWN.getSlot();
            }
        });
        registry.registerPartWrapper(Blocks.WALL_TORCH, new IMultipart() {
            @Override
            public IPartSlot getSlotForPlacement(World world, BlockPos pos, IBlockState state, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer) {
                return EnumFaceSlot.fromFace(state.get(BlockTorchWall.HORIZONTAL_FACING)).getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, IBlockState state) {
                return EnumFaceSlot.fromFace(state.get(BlockTorchWall.HORIZONTAL_FACING)).getSlot();
            }
        });
        registry.registerStackWrapper(Blocks.TORCH.asItem(), Blocks.WALL_TORCH);
    }
}
