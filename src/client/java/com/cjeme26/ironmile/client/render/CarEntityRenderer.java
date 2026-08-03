package com.cjeme26.ironmile.client.render;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.Map;
import java.util.WeakHashMap;

/** Renders Iron Mile's first animated low-poly hatchback. */
public class CarEntityRenderer extends EntityRenderer<CarEntity> {
	private static final Identifier BODY_TEXTURE = Identifier.of("ironmile", "textures/entity/hatchback_yellow.png");
	private static final Identifier WHEEL_TEXTURE = Identifier.of("ironmile", "textures/entity/wheel.png");
	private static final ObjMesh BODY_MESH = ObjMesh.load("/assets/ironmile/models/entity/hatchback_body.obj");
	private static final ObjMesh WHEEL_MESH = ObjMesh.load("/assets/ironmile/models/entity/wheel.obj");
	private static final float MODEL_SCALE = 0.699F;
	private static final float MODEL_BASE_HEIGHT = 0.05F;
	private static final float MODEL_WHEEL_RADIUS = 0.458577F;
	private static final float WHEEL_RADIUS_BLOCKS = MODEL_WHEEL_RADIUS * MODEL_SCALE;
	private static final float WHEEL_SIDE = 0.977F;
	private static final float FRONT_WHEEL_Z = -1.753F;
	private static final float REAR_WHEEL_Z = 1.836F;
	private static final float WHEEL_Y = 0.423F;
	private static final float MAX_VISUAL_STEERING = 28.0F;
	private static final double WHEEL_FORWARD_OFFSET = 0.95;
	private static final double WHEEL_SIDE_OFFSET = 0.65;
	private static final float SUSPENSION_RESPONSE = 0.22F;
	private static final float MAX_TERRAIN_PITCH = 12.0F;
	private static final float MAX_BODY_ROLL = 10.0F;
	private static final float MAX_SUSPENSION_TRAVEL = 0.35F;

	private final BlockRenderManager blockRenderManager;
	private final Map<CarEntity, SuspensionState> suspensionStates = new WeakHashMap<>();

	public CarEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.blockRenderManager = context.getBlockRenderManager();
		this.shadowRadius = 1.35F;
	}

	@Override
	public void render(
			CarEntity car,
			float yaw,
			float tickDelta,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light
	) {
		SuspensionState suspension = this.updateSuspension(car);

		matrices.push();
		matrices.translate(0.0, MODEL_BASE_HEIGHT + suspension.verticalOffset, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(suspension.pitch));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(suspension.roll));

		matrices.push();
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
		this.renderMesh(BODY_MESH, BODY_TEXTURE, matrices, vertexConsumers, light);

		float frameWheelRotation = suspension.wheelRotation
				- (float) Math.toDegrees(suspension.lastForwardSpeed * tickDelta / WHEEL_RADIUS_BLOCKS);
		this.renderWheel(-WHEEL_SIDE, WHEEL_Y, FRONT_WHEEL_Z, suspension.steeringAngle, frameWheelRotation,
				matrices, vertexConsumers, light);
		this.renderWheel(WHEEL_SIDE, WHEEL_Y, FRONT_WHEEL_Z, suspension.steeringAngle, frameWheelRotation,
				matrices, vertexConsumers, light);
		this.renderWheel(-WHEEL_SIDE, WHEEL_Y, REAR_WHEEL_Z, 0.0F, frameWheelRotation,
				matrices, vertexConsumers, light);
		this.renderWheel(WHEEL_SIDE, WHEEL_Y, REAR_WHEEL_Z, 0.0F, frameWheelRotation,
				matrices, vertexConsumers, light);
		matrices.pop();

		BlockState headlight = car.areHeadlightsOn()
				? Blocks.SEA_LANTERN.getDefaultState()
				: Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
		BlockState brakeLight = car.isBrakeInputActive()
				? Blocks.REDSTONE_BLOCK.getDefaultState()
				: Blocks.RED_CONCRETE.getDefaultState();
		BlockState reverseLight = car.isReverseEngaged()
				? Blocks.SEA_LANTERN.getDefaultState()
				: Blocks.GRAY_CONCRETE.getDefaultState();

		// Local -Z is the front; local +Z is the rear.
		this.renderBlockPart(headlight, matrices, vertexConsumers, light, -0.61, 0.49, -1.965, 0.17F, 0.095F, 0.03F);
		this.renderBlockPart(headlight, matrices, vertexConsumers, light, 0.61, 0.49, -1.965, 0.17F, 0.095F, 0.03F);
		this.renderBlockPart(brakeLight, matrices, vertexConsumers, light, -0.67, 0.52, 1.935, 0.16F, 0.09F, 0.03F);
		this.renderBlockPart(brakeLight, matrices, vertexConsumers, light, 0.67, 0.52, 1.935, 0.16F, 0.09F, 0.03F);
		this.renderBlockPart(reverseLight, matrices, vertexConsumers, light, -0.40, 0.52, 1.94, 0.075F, 0.07F, 0.032F);
		this.renderBlockPart(reverseLight, matrices, vertexConsumers, light, 0.40, 0.52, 1.94, 0.075F, 0.07F, 0.032F);
		matrices.pop();
		super.render(car, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	private void renderMesh(
			ObjMesh mesh,
			Identifier texture,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light
	) {
		VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
		mesh.render(matrices.peek(), consumer, light);
	}

	private void renderWheel(
			float x,
			float y,
			float z,
			float steering,
			float rotation,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light
	) {
		matrices.push();
		matrices.translate(x, y, z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(steering));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation));
		this.renderMesh(WHEEL_MESH, WHEEL_TEXTURE, matrices, vertexConsumers, light);
		matrices.pop();
	}

	private void renderBlockPart(
			BlockState state,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light,
			double x,
			double y,
			double z,
			float scaleX,
			float scaleY,
			float scaleZ
	) {
		matrices.push();
		matrices.translate(x, y, z);
		matrices.scale(scaleX, scaleY, scaleZ);
		matrices.translate(-0.5, -0.5, -0.5);
		this.blockRenderManager.renderBlockAsEntity(
				state,
				matrices,
				vertexConsumers,
				light,
				OverlayTexture.DEFAULT_UV
		);
		matrices.pop();
	}

	private SuspensionState updateSuspension(CarEntity car) {
		SuspensionState state = this.suspensionStates.computeIfAbsent(car, ignored -> new SuspensionState(car));
		long worldTime = car.getWorld().getTime();
		if (state.lastUpdateTime == worldTime) {
			return state;
		}

		float yaw = car.getYaw();
		float yawRadians = yaw * MathHelper.RADIANS_PER_DEGREE;
		Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);

		double frontLeft = this.getSupportHeight(car, forward, right, WHEEL_FORWARD_OFFSET, -WHEEL_SIDE_OFFSET);
		double frontRight = this.getSupportHeight(car, forward, right, WHEEL_FORWARD_OFFSET, WHEEL_SIDE_OFFSET);
		double rearLeft = this.getSupportHeight(car, forward, right, -WHEEL_FORWARD_OFFSET, -WHEEL_SIDE_OFFSET);
		double rearRight = this.getSupportHeight(car, forward, right, -WHEEL_FORWARD_OFFSET, WHEEL_SIDE_OFFSET);

		double frontHeight = (frontLeft + frontRight) * 0.5;
		double rearHeight = (rearLeft + rearRight) * 0.5;
		double leftHeight = (frontLeft + rearLeft) * 0.5;
		double rightHeight = (frontRight + rearRight) * 0.5;

		float terrainPitch = (float) Math.toDegrees(Math.atan2(frontHeight - rearHeight, WHEEL_FORWARD_OFFSET * 2.0));
		float terrainRoll = (float) Math.toDegrees(Math.atan2(leftHeight - rightHeight, WHEEL_SIDE_OFFSET * 2.0));
		terrainPitch = MathHelper.clamp(terrainPitch, -MAX_TERRAIN_PITCH, MAX_TERRAIN_PITCH);
		terrainRoll = MathHelper.clamp(terrainRoll, -MAX_BODY_ROLL, MAX_BODY_ROLL);

		double signedSpeed = car.getVelocity().x * forward.x + car.getVelocity().z * forward.z;
		double acceleration = signedSpeed - state.lastForwardSpeed;
		float accelerationPitch = (float) MathHelper.clamp(acceleration * 80.0, -3.0, 3.0);
		float yawChange = MathHelper.wrapDegrees(yaw - state.lastYaw);
		double speedAmount = Math.min(Math.abs(signedSpeed) / 0.7, 1.0);
		float corneringRoll = (float) MathHelper.clamp(-yawChange * speedAmount * 1.2, -5.0, 5.0);

		state.pitch += (terrainPitch + accelerationPitch - state.pitch) * SUSPENSION_RESPONSE;
		state.roll += (terrainRoll + corneringRoll - state.roll) * SUSPENSION_RESPONSE;

		double heightChange = car.getY() - state.lastEntityY;
		state.verticalOffset -= (float) MathHelper.clamp(heightChange, -MAX_SUSPENSION_TRAVEL, MAX_SUSPENSION_TRAVEL);
		state.verticalOffset *= 0.72F;
		state.verticalOffset = MathHelper.clamp(
				state.verticalOffset,
				-MAX_SUSPENSION_TRAVEL,
				MAX_SUSPENSION_TRAVEL
		);

		state.lastForwardSpeed = signedSpeed;
		state.wheelRotation = MathHelper.wrapDegrees(
				state.wheelRotation - (float) Math.toDegrees(signedSpeed / WHEEL_RADIUS_BLOCKS)
		);
		float targetSteering = car.getVisualSteeringInput() * MAX_VISUAL_STEERING;
		state.steeringAngle += (targetSteering - state.steeringAngle) * 0.35F;
		state.lastYaw = yaw;
		state.lastEntityY = car.getY();
		state.lastUpdateTime = worldTime;
		return state;
	}

	private double getSupportHeight(
			CarEntity car,
			Vec3d forward,
			Vec3d right,
			double forwardOffset,
			double sideOffset
	) {
		double x = car.getX() + forward.x * forwardOffset + right.x * sideOffset;
		double z = car.getZ() + forward.z * forwardOffset + right.z * sideOffset;
		double bodyBottom = car.getBoundingBox().minY;
		int highestY = MathHelper.floor(bodyBottom + 0.65);
		int lowestY = MathHelper.floor(bodyBottom - 1.25);

		for (int y = highestY; y >= lowestY; y--) {
			BlockPos pos = BlockPos.ofFloored(x, y, z);
			VoxelShape shape = car.getWorld().getBlockState(pos).getCollisionShape(car.getWorld(), pos);
			if (shape.isEmpty()) {
				continue;
			}

			double top = y + shape.getMax(Direction.Axis.Y);
			if (top <= bodyBottom + 0.65) {
				return top;
			}
		}

		return bodyBottom - 0.75;
	}

	@Override
	public Identifier getTexture(CarEntity entity) {
		return BODY_TEXTURE;
	}

	private static final class SuspensionState {
		private long lastUpdateTime = Long.MIN_VALUE;
		private float pitch;
		private float roll;
		private float verticalOffset;
		private float lastYaw;
		private double lastEntityY;
		private double lastForwardSpeed;
		private float wheelRotation;
		private float steeringAngle;

		private SuspensionState(CarEntity car) {
			this.lastYaw = car.getYaw();
			this.lastEntityY = car.getY();
		}
	}
}
