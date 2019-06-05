package mcmultipart.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public class WorldExt {

    private WorldExt() {
    }

    /**
     * Workaround that is World#setBlockState but without calling Block#breakBlock on the old block
     *
     * @param self  The world instance
     * @param pos   The position where the block state should be set
     * @param state The new block state
     * @param flags Bit set:
     *              <p>
     *              Flag 1 will cause a block update. Flag 2 will send the change to clients. Flag 4 will prevent the block from
     *              being re-rendered, if this is a client world. Flag 8 will force any re-renders to run on the main thread instead
     *              of the worker pool, if this is a client world and flag 4 is clear. Flag 16 will prevent observers from seeing
     *              this change. Flags can be OR-ed
     */
    public static void setBlockStateHack(World self, BlockPos pos, IBlockState state, int flags) {
        try {
            Chunk chunk = self.getChunk(pos);

            int x = pos.getX() & 15;
            int y = pos.getY() & 15;
            int z = pos.getZ() & 15;
            ChunkSection[] storageArrays = chunk.getSections();
            ChunkSection storage = storageArrays[pos.getY() >> 4];
            if (storage != null) storage.set(x, y, z, state);
        } catch (Throwable e) { // Chunk$storageArrays.invoke throws Throwable
            e.printStackTrace();
        }

        self.setBlockState(pos, state, flags);
    }
}
