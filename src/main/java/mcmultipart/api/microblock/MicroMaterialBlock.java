package mcmultipart.api.microblock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class MicroMaterialBlock extends MicroMaterial {

    private final IBlockState state;
    private final Item item;
    private final float hardness;

    public MicroMaterialBlock(IBlockState state) {
        this(state, ObfuscationReflectionHelper.getPrivateValue(Block.class, state.getBlock(), "field_149782_v"));
    }

    public MicroMaterialBlock(IBlockState state, float hardness) {
        this(state, Item.getItemFromBlock(state.getBlock()), hardness);
    }

    public MicroMaterialBlock(IBlockState state, Item item, float hardness) {
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
        return state.isFullCube();
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
    public SoundType getSound(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return state.getBlock().getSoundType(state, world, pos, entity);
    }

    @Override
    public IBlockState getDefaultState() {
        return state;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer, EnumHand hand) {
        return state.getBlock().getStateForPlacement(new BlockItemUseContext(world, placer instanceof EntityPlayer ? (EntityPlayer) placer : null, placer.getHeldItem(hand), pos, facing, hitX, hitY, hitZ));
    }

    @Override
    public IBlockState getActualState(IWorldReader world, BlockPos pos, IBlockState state) {
        return state;
    }

    @Override
    public IBlockState getExtendedState(IWorldReader world, BlockPos pos, IBlockState state) {
        return state.getExtendedState(world, pos);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return state.getBlock().canRenderInLayer(state, layer);
    }

}