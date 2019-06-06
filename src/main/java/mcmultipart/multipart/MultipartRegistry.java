package mcmultipart.multipart;

import com.google.common.base.Preconditions;
import mcmultipart.api.addon.IWrappedBlock;
import mcmultipart.api.item.ItemBlockMultipart;
import mcmultipart.api.item.ItemBlockMultipart.IBlockPlacementInfo;
import mcmultipart.api.item.ItemBlockMultipart.IBlockPlacementLogic;
import mcmultipart.api.item.ItemBlockMultipart.IExtendedBlockPlacementInfo;
import mcmultipart.api.item.ItemBlockMultipart.IPartPlacementLogic;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public enum MultipartRegistry implements IMultipartRegistry {
    INSTANCE;

    private final Map<Block, IMultipart> PART_MAP = new HashMap<>();
    private final Map<Item, Pair<Predicate<ItemStack>, Pair<WrappedBlock, IMultipart>>> STACK_WRAPPING_MAP = new HashMap<>();

    public void computeBlocks() {
        GameRegistry.findRegistry(Block.class).getValues()//
                .stream()//
                .filter(b -> b instanceof IMultipart)//
                .forEach(b -> registerPartWrapper(b, (IMultipart) b));
    }

    @Override
    public void registerPartWrapper(Block block, IMultipart part) {
        PART_MAP.putIfAbsent(block, part);
    }

    @Override
    public IWrappedBlock registerStackWrapper(Item item, Predicate<ItemStack> predicate, Block block) {
        IMultipart part = getPart(block);
        Preconditions.checkState(part != null, "Attempted to wrap the placement of a block that's not registered as a multipart!");
        WrappedBlock wrappedBlock = new WrappedBlock();
        wrappedBlock.setPlacementInfo(block::getStateForPlacement);
        if (item instanceof ItemBlock) {
            wrappedBlock.setBlockPlacementLogic(
                    (context, newState) -> context.getPlayer().canPlayerEdit(context.getPos(), context.getFace(), context.getItem())
                            && context.getWorld().getBlockState(context.getPos()).getBlock().isReplaceable(context.getWorld().getBlockState(context.getPos()), context)
//                            && block.canPlaceBlockAt(world, pos) && block.canPlaceBlockOnSide(world, pos, facing)
                            && context.getWorld().setBlockState(context.getPos(), newState, 11));
        }
        STACK_WRAPPING_MAP.putIfAbsent(item, Pair.of(predicate, Pair.of(wrappedBlock, part)));
        return wrappedBlock;
    }

    public IMultipart getPart(Block block) {
        return PART_MAP.get(block);
    }

    public Pair<WrappedBlock, IMultipart> wrapPlacement(ItemStack stack) {
        Pair<Predicate<ItemStack>, Pair<WrappedBlock, IMultipart>> pair = STACK_WRAPPING_MAP.get(stack.getItem());
        return pair != null && pair.getKey().test(stack) ? pair.getValue() : null;
    }

    public final class WrappedBlock implements IWrappedBlock {

        private IBlockPlacementLogic blockPlacementLogic;
        private IPartPlacementLogic partPlacementLogic = new IPartPlacementLogic() {
            @Override
            public boolean placePart(BlockItemUseContext context, IMultipart multipartBlock, IBlockState state) {
                System.out.println("test placement");
                return false;
            }
        };// = ItemBlockMultipart::placePartAt;
        private IBlockPlacementInfo placementInfo;

        @Override
        public IWrappedBlock setBlockPlacementLogic(IBlockPlacementLogic logic) {
            this.blockPlacementLogic = logic;
            return this;
        }

        @Override
        public IWrappedBlock setPartPlacementLogic(IPartPlacementLogic logic) {
            this.partPlacementLogic = logic;
            return this;
        }

        @Override
        public IWrappedBlock setPlacementInfo(IBlockPlacementInfo info) {
            this.placementInfo = info;
            return this;
        }

        @Override
        public IWrappedBlock setPlacementInfo(IExtendedBlockPlacementInfo info) {
            IBlockPlacementInfo prevInfo = this.placementInfo;
            this.placementInfo = (context) -> info.getStateForPlacement(context.getWorld(), context.getPos(), context.getFace(),
                    context.getHitX(), context.getHitY(), context.getHitZ(), context.getPlayer(),
                    prevInfo.getStateForPlacement(context));
            return this;
        }

        public IBlockPlacementLogic getBlockPlacementLogic() {
            return this.blockPlacementLogic;
        }

        public IPartPlacementLogic getPartPlacementLogic() {
            return partPlacementLogic;
        }

        public IBlockPlacementInfo getPlacementInfo() {
            return this.placementInfo;
        }

    }

}
