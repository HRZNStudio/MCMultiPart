package mcmultipart._test;

import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.item.ItemBlockMultipart;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.EnumFaceSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.*;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

@MCMPAddon
public class TestMCMPAddon implements IMCMPAddon {
    @Override
    public void registerParts(IMultipartRegistry registry) {
        ForgeRegistries.BLOCKS.getValues().forEach(block -> {
            if (block instanceof SlabBlock) {
                registry.registerPartWrapper(block, new IMultipart() {
                    @Override
                    public Block asBlock() {
                        return block;
                    }

                    @Override
                    public IPartSlot getSlotForPlacement(BlockItemUseContext context, BlockState state) {
                        return state.get(SlabBlock.TYPE) == SlabType.TOP ? EnumFaceSlot.UP.getSlot() : EnumFaceSlot.DOWN.getSlot();
                    }

                    @Override
                    public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, BlockState state) {
                        return state.get(SlabBlock.TYPE) == SlabType.TOP ? EnumFaceSlot.UP.getSlot() : EnumFaceSlot.DOWN.getSlot();
                    }
                });
                registry.registerStackWrapper(block).setPlacementInfo(new ItemBlockMultipart.IBlockPlacementInfo() {
                    @Override
                    public BlockState getStateForPlacement(BlockItemUseContext context) {
                        Direction facing = context.getFace();
                        BlockPos pos = !context.replacingClickedOnBlock() ? context.getPos().offset(context.getFace().getOpposite()) : context.getPos();
                        double hitX = context.func_221532_j().getX();
                        double hitY = context.func_221532_j().getY() - pos.getY();
                        double hitZ = context.func_221532_j().getZ();
                        BlockState state = block.getStateForPlacement(context);
                        double x = Math.abs(hitY * facing.getYOffset());
                        boolean top = state.get(SlabBlock.TYPE) == SlabType.TOP;
                        return facing.getAxis() == Direction.Axis.Y && x == 0.5D ? state.with(SlabBlock.TYPE, top ? SlabType.BOTTOM : SlabType.TOP) : state;
                    }
                }).setPartPlacementLogic((context, multipartBlock, state) -> {
                    IPartSlot slot = multipartBlock.getSlotForPlacement(context, state);
                    Direction facing = context.getFace();
                    double hitX = context.func_221532_j().getX() % 1;
                    double hitY = context.func_221532_j().getY() % 1;
                    double hitZ = context.func_221532_j().getZ() % 1;
                    double x = Math.abs((hitX * facing.getXOffset()) + (hitY * facing.getYOffset()) + (hitZ * facing.getZOffset()));
                    if (MultipartHelper.addPart(context.getWorld(), !context.replacingClickedOnBlock() && x != 0 ? context.getPos().offset(context.getFace().getOpposite()) : context.getPos(), slot, state, context.getWorld().isRemote)) {
                        SoundType soundtype = state.getSoundType(context.getWorld(), context.getPos(), context.getPlayer());
                        context.getWorld().playSound(context.getPlayer(), context.getPos(), soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                        return true;
                    }
                    return false;
                });
            }
        });
        registry.registerPartWrapper(Blocks.TORCH, new IMultipart() {
            @Override
            public Block asBlock() {
                return Blocks.TORCH;
            }

            @Override
            public IPartSlot getSlotForPlacement(BlockItemUseContext context, BlockState state) {
                return EnumFaceSlot.DOWN.getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, BlockState state) {
                return EnumFaceSlot.DOWN.getSlot();
            }
        });
        registry.registerPartWrapper(Blocks.WALL_TORCH, new IMultipart() {
            @Override
            public Block asBlock() {
                return Blocks.WALL_TORCH;
            }

            @Override
            public IPartSlot getSlotForPlacement(BlockItemUseContext context, BlockState state) {
                return EnumFaceSlot.fromFace(state.get(WallTorchBlock.HORIZONTAL_FACING)).getSlot();
            }

            @Override
            public IPartSlot getSlotFromWorld(IBlockReader world, BlockPos pos, BlockState state) {
                return EnumFaceSlot.fromFace(state.get(WallTorchBlock.HORIZONTAL_FACING)).getSlot();
            }
        });
        registry.registerStackWrapper(Blocks.TORCH.asItem(), Blocks.WALL_TORCH).setPlacementInfo(context -> {
            BlockState wall_state = Blocks.WALL_TORCH.getStateForPlacement(context);
            BlockState finalState = null;
            World world = context.getWorld();
            BlockPos pos = context.getPos();
            Direction[] directions = context.getNearestLookingDirections();

            for (Direction facing : directions) {
                if (facing != Direction.UP) {
                    BlockState lvt_10_1_ = facing == Direction.DOWN ? Blocks.TORCH.getStateForPlacement(context) : wall_state;
                    if (lvt_10_1_ != null && lvt_10_1_.isValidPosition(world, pos)) {
                        finalState = lvt_10_1_;
                        break;
                    }
                }
            }

            return finalState;
        });
    }
}