package mcmultipart.api.microblock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class MicroMaterialBlock extends MicroMaterial {

    private final BlockState state;
    private final Item item;
    private final float hardness;

    public MicroMaterialBlock(BlockState state) {
        this(state, ObfuscationReflectionHelper.getPrivateValue(Block.class, state.getBlock(), "field_149782_v"));
    }

    public MicroMaterialBlock(BlockState state, float hardness) {
        this(state, Item.getItemFromBlock(state.getBlock()), hardness);
    }

    public MicroMaterialBlock(BlockState state, Item item, float hardness) {
        super(false);
        this.state = state;
        this.item = item;
        this.hardness = hardness;
        this.delegate = this instanceof IMicroMaterialDelegate ? Optional.of((IMicroMaterialDelegate) this)
                : state instanceof IMicroMaterialDelegate ? Optional.of((IMicroMaterialDelegate) state)
                : state.getBlock() instanceof IMicroMaterialDelegate ? Optional.of((IMicroMaterialDelegate) state.getBlock())
                : Optional.empty();
        ResourceLocation blockName = state.getBlock().getRegistryName();
        setRegistryName(new ResourceLocation(blockName.getNamespace(), blockName.getPath()));
    }

    @Override
    public String getLocalizedName() {
        return item != null ? item.getDisplayName(new ItemStack(item, 1)).getFormattedText()
                : state.getBlock().getTranslationKey(); //TODO: Local
    }

    @Override
    public boolean isSolid() {
        return state.isSolid();
    }

    @Override
    public int getLightValue() {
        return state.getLightValue();
    }

    @Override
    public float getHardness() {
        return hardness;
    }

    @Override
    public int getCuttingStrength() {
        return Math.max(0, state.getBlock().getHarvestLevel(state));
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(item, 1);
    }

    @Override
    public SoundType getSound(BlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return state.getBlock().getSoundType(state, world, pos, entity);
    }

    @Override
    public BlockState getDefaultState() {
        return state;
    }

    @Override
    public BlockState getStateForPlacement(World world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, LivingEntity placer, Hand hand) {
        return state.getBlock().getStateForPlacement(new BlockItemUseContext(new ItemUseContext(world, placer instanceof PlayerEntity ? (PlayerEntity) placer : null, placer.getHeldItem(hand), pos, facing, hitX, hitY, hitZ)));
    }

    @Override
    public BlockState getActualState(IWorldReader world, BlockPos pos, BlockState state) {
        return state;
    }

    @Override
    public BlockState getExtendedState(IWorldReader world, BlockPos pos, BlockState state) {
        return state.getExtendedState(world, pos);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
        return state.getBlock().canRenderInLayer(state, layer);
    }

}