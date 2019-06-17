package mcmultipart.api.microblock;

import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.Optional;

public abstract class MicroMaterial extends ForgeRegistryEntry<MicroMaterial> {

    protected Optional<IMicroMaterialDelegate> delegate;

    public MicroMaterial() {
        this(true);
    }

    protected MicroMaterial(boolean hasDelegate) {
        if (hasDelegate) {
            delegate = this instanceof IMicroMaterialDelegate ? Optional.of((IMicroMaterialDelegate) this) : Optional.empty();
        }
    }

    public abstract String getLocalizedName();

    public abstract boolean isSolid();

    public abstract int getLightValue();

    public abstract float getHardness();

    public abstract int getCuttingStrength();

    public abstract ItemStack getStack();

    public abstract SoundType getSound(BlockState state, World world, BlockPos pos, Entity entity);

    public abstract BlockState getDefaultState();

    public abstract BlockState getStateForPlacement(World world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, LivingEntity placer, Hand hand);

    public abstract BlockState getActualState(IWorldReader world, BlockPos pos, BlockState state);

    public BlockState getExtendedState(IWorldReader world, BlockPos pos, BlockState state) {
        return state;
    }

    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID;
    }

    public boolean cacheModels() {
        return true;
    }

    public Optional<IMicroMaterialDelegate> getDelegate(MicroblockType type) {
        return delegate;
    }

}
