package mcmultipart.util;

import lombok.experimental.Delegate;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.world.IMultipartBlockReader;
import mcmultipart.api.world.IWorldView;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class MCMPBlockReaderWrapper implements IBlockReader, IMultipartBlockReader {

    @Delegate(excludes = Ignore.class)
    private final IBlockReader parent;
    private final PartInfo partInfo;
    private final IWorldView view;

    public MCMPBlockReaderWrapper(IBlockReader parent, PartInfo partInfo, IWorldView view) {
        this.parent = parent;
        this.partInfo = partInfo;
        this.view = view;
    }

    @Override
    public IBlockReader getActualWorld() {
        return parent;
    }

    @Override
    public IPartInfo getPartInfo() {
        return partInfo;
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return view.getActualTile(parent, pos);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return view.getActualState(parent, pos);
    }

    private interface Ignore {
        IBlockState getBlockState(BlockPos pos);

        TileEntity getTileEntity(BlockPos pos);
    }
}
