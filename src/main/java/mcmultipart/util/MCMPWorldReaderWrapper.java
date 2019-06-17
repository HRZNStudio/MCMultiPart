package mcmultipart.util;

import lombok.experimental.Delegate;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.world.IMultipartBlockReader;
import mcmultipart.api.world.IWorldView;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityProvider;

import javax.annotation.Nullable;

public class MCMPWorldReaderWrapper implements IWorldReader, IMultipartBlockReader {

    @Delegate(excludes = Ignore.class, types = {IWorldReader.class, IBlockReader.class})
    private final IWorldReader parent;
    private final PartInfo partInfo;
    private final IWorldView view;

    public MCMPWorldReaderWrapper(IWorldReader parent, PartInfo partInfo, IWorldView view) {
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
    public BlockState getBlockState(BlockPos pos) {
        return view.getActualState(parent, pos);
    }

    private interface Ignore {
        BlockState getBlockState(BlockPos pos);

        TileEntity getTileEntity(BlockPos pos);

        boolean areCapsCompatible(CapabilityProvider other);

        boolean areCapsCompatible(@Nullable CapabilityDispatcher other);
    }
}
