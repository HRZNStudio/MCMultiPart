package mcmultipart.client;

import mcmultipart.block.BlockMultipartContainer;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ModelMultipartContainer implements IBakedModel {

    private static BakedQuad tint(PartInfo.ClientInfo info, BakedQuad quad) {
        return quad.hasTintIndex() ? new BakedQuad(quad.getVertexData(), info.getTint(quad.getTintIndex()), quad.getFace(),
                quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()) : quad;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, Random rand) {
        List<PartInfo.ClientInfo> info = ((IExtendedBlockState) state).getValue(BlockMultipartContainer.PROPERTY_INFO);
        BlockRendererDispatcher brd = Minecraft.getInstance().getBlockRendererDispatcher();
        if (info != null) {
            BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            return info//
                    .stream()//
                    .filter(i -> i.canRenderInLayer(layer)) // Make sure it can render in this layer
                    .flatMap(i -> brd.getModelForState(i.getActualState()) // Get model
                            .getQuads(i.getExtendedState(), side, rand).stream() // Stream quads
                            .map(q -> tint(i, q))) // Tint quads
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return Minecraft.getInstance().getTextureMap().getAtlasSprite("minecraft:blocks/stone");
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.EMPTY;
    }

}
