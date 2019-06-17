package mcmultipart;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

public class RayTraceHelper {

    public static Pair<Vec3d, Vec3d> getRayTraceVectors(PlayerEntity player) {
        float pitch = player.rotationPitch;
        float yaw = player.rotationYaw;
        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        float f1 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = -MathHelper.cos(-pitch * 0.017453292F);
        float f4 = MathHelper.sin(-pitch * 0.017453292F);
        float f5 = f2 * f3;
        float f6 = f1 * f3;
        double d3 = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue() + 1;
        Vec3d end = start.add(f5 * d3, f4 * d3, f6 * d3);
        return Pair.of(start, end);
    }
}
