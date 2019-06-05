package mcmultipart.api.event;

import mcmultipart.api.container.IPartInfo;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

public class DrawMultipartHighlightEvent extends DrawBlockHighlightEvent {

    private final IPartInfo partInfo;

    public DrawMultipartHighlightEvent(WorldRenderer context, EntityPlayer player, RayTraceResult target, int subID, float partialTicks, IPartInfo partInfo) {
        super(context, player, target, subID, partialTicks);
        this.partInfo = partInfo;
    }

    public IPartInfo getPartInfo() {
        return partInfo;
    }

}
