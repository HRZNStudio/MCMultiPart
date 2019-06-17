package mcmultipart.util;

import lombok.experimental.Delegate;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.api.world.IMultipartWorld;
import mcmultipart.api.world.IWorldView;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.multipart.PartInfo;
import mcmultipart.network.MultipartAction;
import mcmultipart.network.MultipartNetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public class MCMPWorldWrapper extends World implements IMultipartWorld {

    private final PartInfo part;
    private final IWorldView view;
    @Delegate(excludes = Ignore.class, types = {World.class, IEnviromentBlockReader.class})
    private final World world;

    public MCMPWorldWrapper(PartInfo part, IWorldView view) {
        super(part.getActualWorld().getWorldInfo(), part.getActualWorld().getDimension().getType(), (w, d) -> part.getActualWorld().getChunkProvider(), part.getActualWorld().getProfiler(), part.getActualWorld().isRemote);
        this.part = part;
        this.view = view;
        this.world = part.getActualWorld();
    }

    @Override
    public World getActualWorld() {
        return world;
    }

    @Override
    public IPartInfo getPartInfo() {
        return part;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
        if (part.getPartPos().equals(pos)) {
            if (state.getBlock() == Blocks.AIR) {
                part.remove();
                return true;
            } else {
                IMultipart newPart = MultipartRegistry.INSTANCE.getPart(state.getBlock());
                if (part.getPart() == newPart) {
                    BlockState prevState = part.getState();
                    part.setState(state);
                    notifyBlockUpdate(pos, prevState, state, flags);
                    return true;
                } else {
                    // TODO: Check if part replacement is possible
                    return false;
                }
            }
        }
        return getActualWorld().setBlockState(pos, state, flags);
    }

    @Override
    public void markAndNotifyBlock(BlockPos pos, Chunk chunk, BlockState BlockState, BlockState newState, int flags) {
        if (part.getPartPos().equals(pos)) {
            if ((flags & 2) != 0 && (!this.isRemote || (flags & 4) == 0) ) {
                notifyBlockUpdate(pos, BlockState, newState, flags);
            }
            if ((flags & 0b00001) != 0) {
                notifyNeighborsOfStateChange(pos, newState.getBlock());
                part.getContainer().getParts().values().forEach(i -> {
                    if (i != part) {
                        i.getPart().onPartChanged(i, part);
                    }
                });

                if (newState.hasComparatorInputOverride()) {
                    this.updateComparatorOutputLevel(pos, newState.getBlock());
                }
            } else if ((flags & 0b10000) == 0) {
                notifyBlockUpdate(pos, BlockState, newState, flags);
            }
        } else {
            getActualWorld().markAndNotifyBlock(pos, chunk, BlockState, newState, flags);
        }
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        if (part.getPartPos().equals(pos)) {
            this.playEvent(2001, pos, Block.getStateId(part.getState()));
            if (dropBlock) {
                part.getPart().dropPartAsItemWithChance(part, 1, 0);
            }
            return setBlockState(pos, Blocks.AIR.getDefaultState());
        }
        return getActualWorld().destroyBlock(pos, dropBlock);
    }

    @Override
    public void notifyBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        if (part.getPartPos().equals(pos)) {
            if ((flags & 0b00010) != 0) {
                MultipartNetworkHandler.queuePartChange(part.getActualWorld(), new MultipartAction.Change(part));
            }
            if ((flags & 0b00100) == 0) {
                markForRerender(pos);
            }
            return;
        }
        getActualWorld().notifyBlockUpdate(pos, oldState, newState, flags);
    }

    @Override
    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, Direction skipSide) {
        part.getContainer().getParts().values().forEach(i -> {
            if (i != part) {
                i.getPart().onPartChanged(i, part);
            }
        });
        getActualWorld().notifyNeighborsOfStateExcept(pos, blockType, skipSide);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return view.getActualState(getActualWorld(), pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return view.getActualTile(getActualWorld(), pos);
    }

    @Override
    public void setTileEntity(BlockPos pos, TileEntity tile) {
        if (part.getPartPos().equals(pos)) {

            LazyOptional<IMultipartTile> tileLazyOptional = tile.getCapability(MCMPCapabilities.MULTIPART_TILE);
            if (tileLazyOptional.isPresent()) {
                part.setTile(tileLazyOptional.orElseThrow(NullPointerException::new));
            } else {
                throw new IllegalArgumentException("The specified TileEntity is not a multipart!");
            }
        } else {
            getActualWorld().setTileEntity(pos, tile);
        }
    }

    @Override
    public void removeTileEntity(BlockPos pos) {
        if (part.getPartPos().equals(pos)) {
            TileEntity tileentity = this.getTileEntity(pos);
            if (tileentity != null) {
                tileentity.remove();
            }
            this.updateComparatorOutputLevel(pos, getBlockState(pos).getBlock());
        } else {
            getActualWorld().removeTileEntity(pos);
        }
    }

//    @Override
//    public void markTileEntityForRemoval(TileEntity tile) {
//        if (tile != null) {
//            BlockPos pos = tile.getPos();
//            if (part.getPartPos().equals(pos)) {
//                tile.remove();
//                this.updateComparatorOutputLevel(pos, getBlockState(pos).asBlock());
//                return;
//            }
//        }
//        getActualWorld().markTileEntityForRemoval(tile);
//    }


    private interface Ignore {
        void notifyBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags);

        void setTileEntity(BlockPos pos, TileEntity tile);

        boolean setBlockState(BlockPos pos, BlockState state, int flags);

        void markAndNotifyBlock(BlockPos pos, Chunk chunk, BlockState BlockState, BlockState newState, int flags);

        boolean destroyBlock(BlockPos pos, boolean dropBlock);

        void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, Direction skipSide);

        BlockState getBlockState(BlockPos pos);

        TileEntity getTileEntity(BlockPos pos);

        void removeTileEntity(BlockPos pos);

        void markTileEntityForRemoval(TileEntity tile);

        boolean areCapsCompatible(CapabilityProvider other);

        boolean areCapsCompatible(@Nullable CapabilityDispatcher other);
    }

}