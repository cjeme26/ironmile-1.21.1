package com.cjeme26.ironmile.client.mixin;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides a constrained front-mounted first-person camera for Iron Mile cars.
 * Third-person cameras keep Minecraft's normal behavior.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    /** Keeps the camera near the front of the hatchback without exposing too much bonnet. */
    @Unique
    private static final double IRONMILE_CAMERA_FORWARD_OFFSET = 1.78;

    /** Raised enough to give a useful view of the road. */
    @Unique
    private static final double IRONMILE_CAMERA_HEIGHT = 1.30;

    /** Keeps the camera a small distance away from a wall after ray clipping. */
    @Unique
    private static final double IRONMILE_CAMERA_WALL_MARGIN = 0.10;

    /** Maximum amount the driver can look to either side of the car's heading. */
    @Unique
    private static final float IRONMILE_MAX_SIDE_LOOK_DEGREES = 85.0F;

    /** Maximum upward look. Minecraft uses negative pitch for looking upward. */
    @Unique
    private static final float IRONMILE_MAX_UP_LOOK_DEGREES = 20.0F;

    /** Looking downward is disabled while using the driving camera. */
    @Unique
    private static final float IRONMILE_LOWEST_PITCH = 0.0F;

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void ironmile$applyConstrainedFirstPersonDrivingCamera(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (thirdPerson || !(focusedEntity.getVehicle() instanceof CarEntity car)) {
            return;
        }

        float currentCarYaw = car.getYaw();
        float renderedCarYaw = car.getYaw(tickDelta);
        float requestedYawOffset = MathHelper.wrapDegrees(focusedEntity.getYaw() - currentCarYaw);
        float yawOffset = MathHelper.clamp(
                requestedYawOffset,
                -IRONMILE_MAX_SIDE_LOOK_DEGREES,
                IRONMILE_MAX_SIDE_LOOK_DEGREES
        );
        float playerYaw = currentCarYaw + yawOffset;
        float cameraYaw = renderedCarYaw + yawOffset;

        float cameraPitch = MathHelper.clamp(
                focusedEntity.getPitch(),
                -IRONMILE_MAX_UP_LOOK_DEGREES,
                IRONMILE_LOWEST_PITCH
        );

        /*
         * Clamp the player as well as the camera. This prevents mouse movement
         * from accumulating invisibly beyond the allowed viewing cone, so the
         * view responds immediately when the mouse is moved back toward center.
         */
        focusedEntity.setYaw(playerYaw);
        focusedEntity.setHeadYaw(playerYaw);
        focusedEntity.setPitch(cameraPitch);
        this.setRotation(cameraYaw, cameraPitch);

        Vec3d carPos = car.getLerpedPos(tickDelta);
        float carYawRadians = renderedCarYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(
                -MathHelper.sin(carYawRadians),
                0.0,
                MathHelper.cos(carYawRadians)
        );
        Vec3d anchor = carPos.add(0.0, IRONMILE_CAMERA_HEIGHT, 0.0);
        Vec3d desiredPos = anchor.add(forward.multiply(IRONMILE_CAMERA_FORWARD_OFFSET));

        /*
         * Pull the front-mounted camera back toward the car when a solid block
         * lies between its anchor and desired position.
         */
        HitResult hit = car.getWorld().raycast(new RaycastContext(
                anchor,
                desiredPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                focusedEntity
        ));
        if (hit.getType() != HitResult.Type.MISS) {
            double clearDistance = Math.max(
                    0.0,
                    anchor.distanceTo(hit.getPos()) - IRONMILE_CAMERA_WALL_MARGIN
            );
            desiredPos = anchor.add(forward.multiply(
                    Math.min(IRONMILE_CAMERA_FORWARD_OFFSET, clearDistance)
            ));
        }

        this.setPos(desiredPos);
    }
}
