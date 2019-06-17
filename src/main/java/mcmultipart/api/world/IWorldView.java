package mcmultipart.api.world;

import mcmultipart.api.container.IPartInfo;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public interface IWorldView {

    static IWorldView getDefaultFor(IPartInfo part) {
        return new IWorldView() {

            @Override
            public BlockState getActualState(IBlockReader world, BlockPos pos) {
                return pos.equals(part.getPartPos()) ? part.getState() : world.getBlockState(pos);
            }

            @Override
            public TileEntity getActualTile(IBlockReader world, BlockPos pos) {
                return pos.equals(part.getPartPos()) ? part.getTile() != null ? part.getTile().asTileEntity() : null
                        : world.getTileEntity(pos);
            }

        };
    }

    BlockState getActualState(IBlockReader world, BlockPos pos);

    TileEntity getActualTile(IBlockReader world, BlockPos pos);

}
