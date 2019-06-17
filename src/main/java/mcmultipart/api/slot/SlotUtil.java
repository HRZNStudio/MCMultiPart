package mcmultipart.api.slot;

import com.google.common.base.Throwables;
import net.minecraft.util.Direction;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Function;

public class SlotUtil {

    private static MethodHandle viewSide, viewEdge;

    public static <T, O> O viewContainer(ISlottedContainer<T> container, Function<T, O> converter, Function<List<O>, O> joiner, O startVal,
                                         boolean ignoreNull, Direction face) {
        try {
            return (O) viewSide.invoke(container, converter, joiner, startVal, ignoreNull, face);
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T, O> O viewContainer(ISlottedContainer<T> container, Function<T, O> converter, Function<List<O>, O> joiner, O startVal,
                                         boolean ignoreNull, EnumEdgeSlot edge, Direction face) {
        try {
            return (O) viewEdge.invoke(container, converter, joiner, startVal, ignoreNull, edge, face);
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }

}
