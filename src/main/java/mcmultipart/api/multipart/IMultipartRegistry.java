package mcmultipart.api.multipart;

import mcmultipart.api.addon.IWrappedBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public interface IMultipartRegistry {

    void registerPartWrapper(Block block, IMultipart part);

    IWrappedBlock registerStackWrapper(Item item, Predicate<ItemStack> predicate, Block block);

    default IWrappedBlock registerStackWrapper(Item item, Block block) {
        return registerStackWrapper(item, s -> true, block);
    }

    default IWrappedBlock registerStackWrapper(Block block) {
        return registerStackWrapper(Item.getItemFromBlock(block), block);
    }

}
