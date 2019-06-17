package mcmultipart.client;

import mcmultipart.block.BlockMultipartContainer;
import mcmultipart.multipart.PartInfo;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        List<PartInfo.ClientInfo> info = extraData.getData(BlockMultipartContainer.PROPERTY_INFO);
        BlockRendererDispatcher brd = Minecraft.getInstance().getBlockRendererDispatcher();
        if (info != null) {
            BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            return info
                    .stream()
                    .filter(i -> i.canRenderInLayer(layer)) // Make sure it can render in this layer
                    .flatMap(i -> brd.getModelForState(i.getActualState()) // Get model
                            .getQuads(i.getExtendedState(), side, rand).stream() // Stream quads
                            .map(q -> tint(i, q))) // Tint quads
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
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
        return Minecraft.getInstance().getAtlasTexture().getAtlasSprite("minecraft:blocks/stone");
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
