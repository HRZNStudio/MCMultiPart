package mcmultipart.api.container;

import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IPartInfo {

    default World getActualWorld() {
        return getContainer() != null ? getContainer().getPartWorld() : null;
    }

    World getPartWorld();

    default BlockPos getPartPos() {
        return getContainer() != null ? getContainer().getPartPos() : BlockPos.ORIGIN;
    }

    IMultipartContainer getContainer();

    IPartSlot getSlot();

    IMultipart getPart();

    IBlockState getState();

    IMultipartTile getTile();

    default void remove() {
        getContainer().removePart(getSlot());
    }

    default void notifyChange() {
        getContainer().notifyChange(this);
    }

}
