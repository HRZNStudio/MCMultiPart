package mcmultipart.network;

import com.google.common.base.Throwables;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.slot.IPartSlot;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.util.math.BlockPos;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.function.Function;

public abstract class MultipartAction {
    public final BlockPos pos;
    public final IPartSlot slot;
    public final int type;

    private MultipartAction(int type, BlockPos pos, IPartSlot slot) {
        this.pos = pos;
        this.slot = slot;
        this.type = type;
    }

    public abstract void handlePacket(PlayerEntity player);

    public static final class Add extends DataCarrier {
        public static final int TYPE = 0;

        public Add(BlockPos pos, IPartSlot slot, BlockState state, CompoundNBT data) {
            super(TYPE, pos, slot, state, data);
        }

        public Add(IPartInfo info) {
            this(info.getPartPos(), info.getSlot(), info.getState(), info.getTile() != null ? info.getTile().getPartUpdateTag() : null);
        }

        @Override
        public void handlePacket(PlayerEntity player) {
            PartInfo.handleAdditionPacket(player.world, pos, slot, state, data);
        }
    }

    public static final class Change extends DataCarrier {
        public static final int TYPE = 1;
        private static final MethodHandle SUpdateTileEntityPacket$nbt;
        private static final Function<SUpdateTileEntityPacket, CompoundNBT> getUpdateTag;

        static {
            try {
                Field f = null;
                try {
                    f = SUpdateTileEntityPacket.class.getDeclaredField("field_148860_e");
                } catch (Exception e) {
                    f = SUpdateTileEntityPacket.class.getDeclaredField("nbt");
                }
                SUpdateTileEntityPacket$nbt = MethodHandles.lookup().unreflectGetter(f);
            } catch (Exception ex) {
                throw Throwables.propagate(ex);
            }

            getUpdateTag = it -> {
                try {
                    return (CompoundNBT) SUpdateTileEntityPacket$nbt.invokeExact(it);
                } catch (Throwable ex) {
                    throw Throwables.propagate(ex);
                }
            };
        }

        public Change(BlockPos pos, IPartSlot slot, BlockState state, CompoundNBT data) {
            super(TYPE, pos, slot, state, data);
        }

        public Change(IPartInfo info) {
            this(info.getPartPos(), info.getSlot(), info.getState(), info.getTile() != null ? getUpdateTag.apply(info.getTile().getPartUpdatePacket()) : null);
        }

        @Override
        public void handlePacket(PlayerEntity player) {
            PartInfo.handleUpdatePacket(player.world, pos, slot, state, data != null ? new SUpdateTileEntityPacket(pos, 0, data) : null);
        }
    }

    public static final class Remove extends MultipartAction {
        public static final int TYPE = 2;

        public Remove(BlockPos pos, IPartSlot slot) {
            super(TYPE, pos, slot);
        }

        @Override
        public void handlePacket(PlayerEntity player) {
            PartInfo.handleRemovalPacket(player.world, pos, slot);
        }
    }

    public static abstract class DataCarrier extends MultipartAction {
        public final BlockState state;
        public final CompoundNBT data;

        private DataCarrier(int type, BlockPos pos, IPartSlot slot, BlockState state, CompoundNBT data) {
            super(type, pos, slot);
            this.state = state;
            this.data = data;
        }
    }
}
