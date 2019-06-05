package mcmultipart.api.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.List;

public abstract class MicroblockType extends ForgeRegistryEntry<MicroblockType> {

    public abstract String getLocalizedName(MicroMaterial material, int size);

    public abstract ItemStack createStack(MicroMaterial material, int size);

    public abstract List<ItemStack> createDrops(MicroMaterial material, int size);

    public abstract MicroMaterial getMaterial(ItemStack stack);

    public abstract int getSize(ItemStack stack);

    public abstract boolean place(World world, EntityPlayer player, ItemStack stack, RayTraceResult hit);

    @OnlyIn(Dist.CLIENT)
    public abstract void drawPlacement(IWorldReader world, EntityPlayer player, ItemStack stack, RayTraceResult hit);

    public int getMinSize() {
        return 1;
    }

    public abstract int getMaxSize();

}
