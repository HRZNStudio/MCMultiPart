package mcmultipart.api.multipart;

import mcmultipart.api.container.IPartInfo;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public interface IMultipartTile {

    static IMultipartTile wrap(TileEntity tile) {
        return new IMultipartTile() {

            @Override
            public TileEntity asTileEntity() {
                return tile;
            }
        };
    }

    default TileEntity asTileEntity() {
        if (!(this instanceof TileEntity)) {
            throw new IllegalStateException("This multipart tile isn't a TileEntity. Override IMultipartTile#asTileEntity()!");
        }
        return (TileEntity) this;
    }

    default boolean isTickable() {
        return getTickable() != null;
    }

    default ITickableTileEntity getTickable() {
        return asTileEntity() instanceof ITickableTileEntity ? (ITickableTileEntity) asTileEntity() : null;
    }

    default void setPartInfo(IPartInfo info) {
    }

    default World getPartWorld() {
        return asTileEntity().getWorld();
    }

    default void setPartWorld(World world) {
        asTileEntity().setWorld(world);
    }

    default boolean hasPartWorld() {
        return asTileEntity().hasWorld();
    }

    default BlockPos getPartPos() {
        return asTileEntity().getPos();
    }

    default void setPartPos(BlockPos pos) {
        asTileEntity().setPos(pos);
    }

    default <T> LazyOptional<T> getPartCapability(Capability<T> capability, Direction side) {
        return asTileEntity().getCapability(capability, side);
    }

    default <T> LazyOptional<T> getPartCapability(Capability<T> capability) {
        return asTileEntity().getCapability(capability);
    }

    default AxisAlignedBB getPartRenderBoundingBox() {
        return asTileEntity().getRenderBoundingBox();
    }

    default double getMaxPartRenderDistanceSquared() {
        return asTileEntity().getMaxRenderDistanceSquared();
    }
}