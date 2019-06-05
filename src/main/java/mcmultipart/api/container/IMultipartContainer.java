package mcmultipart.api.container;

import com.google.common.base.Preconditions;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.api.slot.ISlottedContainer;
import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;

public interface IMultipartContainer extends ISlottedContainer<IPartInfo> {

    World getPartWorld();

    BlockPos getPartPos();

    @Override
    Optional<IPartInfo> get(IPartSlot slot);

    default Optional<IMultipart> getPart(IPartSlot slot) {
        return get(slot).map(IPartInfo::getPart);
    }

    default Optional<IMultipartTile> getPartTile(IPartSlot slot) {
        return get(slot).map(IPartInfo::getTile);
    }

    default Optional<IBlockState> getState(IPartSlot slot) {
        return get(slot).map(IPartInfo::getState);
    }

    Map<IPartSlot, ? extends IPartInfo> getParts();

    default boolean canAddPart(IPartSlot slot, IBlockState state) {
        IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
        Preconditions.checkState(part != null, "The blockstate " + state + " could not be converted to a multipart!");
        IMultipartTile tile = part.createMultipartTile(getPartWorld(), slot, state);
        return canAddPart(slot, state, tile);
    }

    boolean canAddPart(IPartSlot slot, IBlockState state, IMultipartTile tile);

    default void addPart(IPartSlot slot, IBlockState state) {
        IMultipart part = MultipartRegistry.INSTANCE.getPart(state.getBlock());
        Preconditions.checkState(part != null, "The blockstate " + state + " could not be converted to a multipart!");
        IMultipartTile tile = part.createMultipartTile(getPartWorld(), slot, state);
        addPart(slot, state, tile);
    }

    void addPart(IPartSlot slot, IBlockState state, IMultipartTile tile);

    void removePart(IPartSlot slot);

    default void notifyChange(IPartInfo part) {
        for (IPartInfo info : getParts().values()) {
            if (info != part) {
                info.getPart().onPartChanged(info, part);
            }
        }
    }

}
