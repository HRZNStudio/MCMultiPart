package mcmultipart.api.multipart;

import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.slot.EnumEdgeSlot;
import mcmultipart.api.slot.SlotUtil;
import mcmultipart.capability.CapabilityJoiner;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MultipartCapabilityHelper {

    private static BiConsumer<Capability<?>, Function<?, ?>> registerJoiner;

    public static <T> void registerCapabilityJoiner(Capability<T> capability, Function<List<T>, T> joiner) {
        registerJoiner.accept(capability, joiner);
    }

    public static <T> LazyOptional<T> getCapability(IMultipartContainer container, Capability<T> capability, EnumFacing face) {
        T v = SlotUtil.viewContainer(container, i -> {
            if (i.getTile() != null) {
                LazyOptional<T> optional = i.getTile().getPartCapability(capability, face);
                if (optional.isPresent()) {
                    return optional.orElseThrow(NullPointerException::new);
                }
            }
            return null;
        }, l -> {
            return CapabilityJoiner.join(capability, l);
        }, null, true, face);
        if (v != null) {
            return LazyOptional.of(() -> v);
        }
        return LazyOptional.empty();
    }

    public static <T> LazyOptional<T> getCapability(IMultipartContainer container, Capability<T> capability, EnumEdgeSlot edge, EnumFacing face) {
        T v = SlotUtil.viewContainer(container, i -> {
            if (i.getTile() != null) {
                LazyOptional<T> optional = i.getTile().getPartCapability(capability, face);
                if (optional.isPresent()) {
                    return optional.orElseThrow(NullPointerException::new);
                }
            }
            return null;
        }, l -> {
            return CapabilityJoiner.join(capability, l);
        }, null, true, edge, face);
        if (v != null) {
            return LazyOptional.of(() -> v);
        }
        return LazyOptional.empty();
    }

}
