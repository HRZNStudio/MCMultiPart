package mcmultipart.api.world;

import mcmultipart.api.container.IPartInfo;
import net.minecraft.world.IBlockReader;

public interface IMultipartBlockReader {

    public IBlockReader getActualWorld();

    public IPartInfo getPartInfo();

}
