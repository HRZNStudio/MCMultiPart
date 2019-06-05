package mcmultipart.api.world;

import net.minecraft.world.World;

public interface IMultipartWorld extends IMultipartBlockReader {

    public World getActualWorld();

}
