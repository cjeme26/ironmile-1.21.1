package com.cjeme26.ironmile.client;

import com.cjeme26.ironmile.client.render.CarEntityRenderer;
import com.cjeme26.ironmile.client.sound.EngineSoundManager;
import com.cjeme26.ironmile.client.screen.FuelScreen;
import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.network.HeadlightTogglePayload;
import com.cjeme26.ironmile.network.CarInputPayload;
import com.cjeme26.ironmile.network.GearSelectPayload;
import com.cjeme26.ironmile.network.GearShiftPayload;
import com.cjeme26.ironmile.network.IgnitionTogglePayload;
import com.cjeme26.ironmile.network.ExitVehiclePayload;
import com.cjeme26.ironmile.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class IronMileClient implements ClientModInitializer {
	private static KeyBinding toggleHeadlightsKey;
	private static KeyBinding shiftUpKey;
	private static KeyBinding shiftDownKey;
	private static KeyBinding clutchKey;
	private static KeyBinding exitVehicleKey;
	private static int lastControlledCarId = -1;
	private static boolean mouseShifterActive = false;
	private static boolean jumpWasDownForIgnition = false;
	private static double shifterX = 0.0;
	private static double shifterY = 0.0;
	private static boolean reverseLockoutLatched = false;
	private static boolean reverseToggleWasDown = false;
	private static boolean shifterInVerticalLane = false;
	private static int neutralSlot = 0; // -1 = 1/2, 0 = 3/4, 1 = 5/6
	private static int gearAtShiftStart = 0;

	@Override
	public void onInitializeClient() {
		toggleHeadlightsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.toggle_headlights",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"key.category.ironmile"
		));
		shiftUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.shift_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.ironmile"));
		shiftDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.shift_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "key.category.ironmile"));
		clutchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.clutch", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.category.ironmile"));
		exitVehicleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.exit_vehicle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.category.ironmile"));
		EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEADLIGHT_MARKER, EmptyEntityRenderer::new);
		HandledScreens.register(ModScreenHandlers.FUEL, FuelScreen::new);
		ClientTickEvents.END_CLIENT_TICK.register(EngineSoundManager::tick);

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			boolean drivingCar = client.currentScreen == null
					&& client.player != null
					&& client.player.getVehicle() instanceof CarEntity car
					&& car.getControllingPassenger() == client.player;

			if (!drivingCar) {
				while (exitVehicleKey.wasPressed()) {}
				jumpWasDownForIgnition = false;
				return;
			}

			CarEntity car = (CarEntity) client.player.getVehicle();

			while (exitVehicleKey.wasPressed()) {
				int carId = car.getId();
				client.player.stopRiding();
				ClientPlayNetworking.send(new ExitVehiclePayload(carId));
				return;
			}

			if (car.isManualTransmission()) {
				/*
				 * Left Shift is the clutch while driving a manual car.
				 * Vanilla Sneak/dismount is consumed; X exits the vehicle.
				 */
				client.options.sneakKey.setPressed(false);
				while (client.options.sneakKey.wasPressed()) {}
			}

			/*
			 * Space reuses Minecraft's actual Jump key. Inside the driver seat it
			 * becomes a single, easy-to-remember ignition control; outside the car
			 * it immediately returns to normal jumping.
			 */
			boolean jumpDown = client.options.jumpKey.isPressed();
			if (jumpDown && !jumpWasDownForIgnition) {
				car.toggleIgnition();
				ClientPlayNetworking.send(new IgnitionTogglePayload(car.getId()));
				client.player.playSound(
						SoundEvents.BLOCK_LEVER_CLICK,
						0.45F,
						car.isEngineStarting() ? 0.92F : 0.62F
				);
			}
			jumpWasDownForIgnition = jumpDown;

			// Do not allow Minecraft's normal jump/mount action while driving.
			client.options.jumpKey.setPressed(false);
			while (client.options.jumpKey.wasPressed()) {}
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				lastControlledCarId = -1;
				return;
			}

			if (client.player.getVehicle() instanceof CarEntity car) {
				int inputMask = 0;
				if (client.options.leftKey.isPressed()) inputMask |= CarInputPayload.LEFT;
				if (client.options.rightKey.isPressed()) inputMask |= CarInputPayload.RIGHT;
				if (client.options.forwardKey.isPressed()) inputMask |= CarInputPayload.FORWARD;
				if (client.options.backKey.isPressed()) inputMask |= CarInputPayload.BACK;
				if (car.isManualTransmission() && clutchKey.isPressed()) inputMask |= CarInputPayload.CLUTCH;

				/* Apply input before the client world tick for immediate local prediction. */
				car.setInputs(
						(inputMask & CarInputPayload.LEFT) != 0,
						(inputMask & CarInputPayload.RIGHT) != 0,
						(inputMask & CarInputPayload.FORWARD) != 0,
						(inputMask & CarInputPayload.BACK) != 0,
						(inputMask & CarInputPayload.CLUTCH) != 0
				);
				lastControlledCarId = car.getId();
				ClientPlayNetworking.send(new CarInputPayload(car.getId(), inputMask));
				return;
			}

			/*
			 * Send one explicit neutral state on the first tick after dismounting.
			 * Without this, releasing control while A or D is held can leave the
			 * server with the final steering packet indefinitely.
			 */
			if (lastControlledCarId != -1) {
				ClientPlayNetworking.send(new CarInputPayload(lastControlledCarId, 0));
				lastControlledCarId = -1;
			}
		});
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			boolean drivingManual = client.player != null
					&& client.player.getVehicle() instanceof CarEntity car
					&& car.isManualTransmission()
					&& car.getControllingPassenger() == client.player;

			if (!drivingManual) {
				if (mouseShifterActive) {
					cancelMouseShifter();
				}
				return;
			}

			CarEntity car = (CarEntity) client.player.getVehicle();

			/*
			 * Read Minecraft's actual "Use Item / Place Block" keybinding.
			 * This is much more reliable than assuming a particular GLFW mouse
			 * button, and it also respects a player's remapped controls.
			 */
			boolean useHeld = client.options.useKey.isPressed();

			if (useHeld) {
				if (!mouseShifterActive) {
					beginMouseShifter(car);
				}
				boolean reverseToggleDown = client.options.attackKey.isPressed();
				if (reverseToggleDown && !reverseToggleWasDown) {
					reverseLockoutLatched = !reverseLockoutLatched;
				}
				reverseToggleWasDown = reverseToggleDown;
			} else if (mouseShifterActive) {
				finishMouseShifter();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null
					&& client.player.getVehicle() instanceof CarEntity car
					&& car.getControllingPassenger() == client.player) {

				while (shiftUpKey.wasPressed()) {
					if (car.isManualTransmission()) {
						// Permanent fallback for the mouse H-pattern.
						car.manualShift(1);
						ClientPlayNetworking.send(new GearShiftPayload(car.getId(), 1));
					} else if (car.automaticSelectorStep(-1)) {
						// R: R -> D -> P
						ClientPlayNetworking.send(new GearShiftPayload(car.getId(), 1));
						client.player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.45F, 0.88F);
					}
				}

				while (shiftDownKey.wasPressed()) {
					if (car.isManualTransmission()) {
						car.manualShift(-1);
						ClientPlayNetworking.send(new GearShiftPayload(car.getId(), -1));
					} else if (car.automaticSelectorStep(1)) {
						// F: P -> D -> R
						ClientPlayNetworking.send(new GearShiftPayload(car.getId(), -1));
						client.player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.45F, 1.05F);
					}
				}

			} else {
				while (shiftUpKey.wasPressed()) {}
				while (shiftDownKey.wasPressed()) {}
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleHeadlightsKey.wasPressed()) {
				if (client.player != null && client.player.getVehicle() instanceof CarEntity car) {
					ClientPlayNetworking.send(new HeadlightTogglePayload(car.getId()));
				}
			}
		});
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || !(client.player.getVehicle() instanceof CarEntity car)) {
				return;
			}

			int screenWidth = client.getWindow().getScaledWidth();
			int centerX = screenWidth / 2;

			drawDrivingSpeedAndRpm(drawContext, client, car);
			drawFuelGauge(drawContext, client, car.getFuelFraction(), 14, 14);

			if (car.isAutomaticTransmission()) {
				drawAutomaticSelector(
						drawContext,
						client,
						car.getAutomaticSelectorDisplay(),
						screenWidth - 66,
						12
				);
			}

			drawDashboardIndicators(drawContext, client, car);

			/*
			 * Engine-off is intentionally silent in the HUD. Only states that
			 * require immediate attention interrupt the normal dashboard.
			 */
			String vehicleState = "";
			int stateColor = 0xFFFFC857;
			if (car.isEngineStarting()) {
				vehicleState = "STARTING...";
			} else if (car.isOutOfFuel()) {
				vehicleState = "OUT OF FUEL";
			} else if (car.isEngineStalled()) {
				vehicleState = "STALLED";
				stateColor = 0xFFFF6262;
			}

			if (!vehicleState.isEmpty()) {
				int stateX = (screenWidth - client.textRenderer.getWidth(vehicleState)) / 2;
				drawContext.drawTextWithShadow(
						client.textRenderer,
						Text.literal(vehicleState),
						stateX,
						55,
						stateColor
				);
			}

			if (car.isManualTransmission()) {
				drawManualGearStick(drawContext, client, car.getSelectedGearDisplay());
			}
		});
	}

	private static void drawDrivingSpeedAndRpm(
			net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client,
			CarEntity car
	) {
		String speed = String.format(Locale.ROOT, "%.0f km/h", Math.abs(car.getHorizontalSpeedKmh()));
		float speedScale = 1.35F;

		drawContext.getMatrices().push();
		drawContext.getMatrices().scale(speedScale, speedScale, 1.0F);
		int logicalWidth = (int) (client.getWindow().getScaledWidth() / speedScale);
		int speedX = (logicalWidth - client.textRenderer.getWidth(speed)) / 2;
		drawContext.drawTextWithShadow(
				client.textRenderer,
				Text.literal(speed),
				speedX,
				6,
				0xFFFFFFFF
		);
		drawContext.getMatrices().pop();

		String rpm = car.getEngineRpm() + " RPM";
		int rpmColor = car.isManualTransmission()
				? getManualRpmColor(car)
				: 0xFFD8D8D8;
		int rpmWidth = client.textRenderer.getWidth(rpm);
		int rpmX = (client.getWindow().getScaledWidth() - rpmWidth) / 2;

		drawContext.drawTextWithShadow(
				client.textRenderer,
				Text.literal(rpm),
				rpmX,
				27,
				rpmColor
		);

		if (car.isManualTransmission() && clutchKey.isPressed()) {
			/*
			 * Clutch state appears only while the pedal is actually held.
			 * No box or warning background: it is an operating-state hint,
			 * not a fault lamp.
			 */
			drawContext.drawTextWithShadow(
					client.textRenderer,
					Text.literal("C"),
					rpmX + rpmWidth + 6,
					27,
					0xFFFFC857
			);
		}
	}

	private static int getManualRpmColor(CarEntity car) {
		if (!car.isEngineRunning() || car.isEngineStarting()) {
			return 0xFFBDBDBD;
		}

		int rpm = car.getEngineRpm();
		double limiter = Math.max(1.0, car.getVehicleSpec().revLimiterRpm());

		/*
		 * High-RPM guidance always applies, but the thresholds are deliberately
		 * generous so the HUD does not nag the player during ordinary driving.
		 */
		if (rpm >= limiter * 0.96) {
			return 0xFFFF6262;
		}
		if (rpm >= limiter * 0.84) {
			return 0xFFFFC857;
		}

		String gear = car.getSelectedGearDisplay();

		/*
		 * Clutch down or Neutral means the wheels are intentionally disconnected
		 * from the engine. Low RPM is therefore normal and should never look like
		 * a fault. Idle RPM stays green.
		 */
		if (clutchKey.isPressed() || "N".equals(gear)) {
			return 0xFF66D17A;
		}

		double speedKmh = Math.abs(car.getHorizontalSpeedKmh());

		/*
		 * First gear at a standstill is a normal place to be at idle. Only warn
		 * about low RPM when the engine is actually under load and the car is
		 * already moving enough for "lugging" to be meaningful.
		 */
		boolean loaded = car.hasThrottleInput() && speedKmh > 3.0;
		if (loaded) {
			if (rpm < 650) {
				return 0xFFFF6262;
			}
			if (rpm < 1000) {
				return 0xFFFFC857;
			}
		}

		return 0xFF66D17A;
	}


	private static void drawFuelGauge(
			net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client,
			double fuelFraction,
			int x,
			int y
	) {
		int segments = 8;
		int segmentWidth = 4;
		int segmentHeight = 5;
		int gap = 1;
		int filled = (int) Math.round(
				Math.max(0.0, Math.min(1.0, fuelFraction)) * segments
		);

		int width = 62;
		int height = 16;
		// Extra 2 px above F/E; bottom edge stays where it was.
		drawCutCornerPanel(drawContext, x - 3, y - 5, width, height);

		drawContext.drawText(
				client.textRenderer,
				Text.literal("F"),
				x,
				y - 1,
				0xFFE2E2E2,
				false
		);

		int barX = x + 9;
		for (int i = 0; i < segments; i++) {
			int sx = barX + i * (segmentWidth + gap);
			drawContext.fill(
					sx,
					y,
					sx + segmentWidth,
					y + segmentHeight,
					i < filled ? 0xFFFFB41F : 0xFF4C4C4C
			);
		}

		int eX = barX + segments * (segmentWidth + gap) + 2;
		drawContext.drawText(
				client.textRenderer,
				Text.literal("E"),
				eX,
				y - 1,
				0xFFE2E2E2,
				false
		);
	}


	private static void drawAutomaticSelector(
			net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client,
			String selected,
			int x,
			int y
	) {
		int width = 54;
		int height = 18;
		drawCutCornerPanel(drawContext, x, y, width, height);

		String[] options = {"P", "D", "R"};
		int[] positions = {9, 27, 45};

		for (int i = 0; i < options.length; i++) {
			boolean active = options[i].equals(selected);
			int center = x + positions[i];
			int textWidth = client.textRenderer.getWidth(options[i]);

			if (active) {
				// Amber selected tile mirrors the selected manual gear.
				drawContext.fill(
						center - 6,
						y + 3,
						center + 6,
						y + 15,
						0xFFFFB41F
				);
			}

			drawContext.drawText(
					client.textRenderer,
					Text.literal(options[i]),
					center - textWidth / 2,
					y + 5,
					active ? 0xFF202020 : 0xFFB8B8B8,
					false
			);
		}
	}


	private static void drawDashboardIndicators(
			net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client,
			CarEntity car
	) {
		boolean headlights = car.areHeadlightsOn();
		boolean lowFuel = car.getFuelFraction() <= 0.15;
		boolean engineWarning = car.isEngineStalled() || car.isOutOfFuel();

		int inactive = 0x70454545;
		int x = 14;
		int y = client.getWindow().getScaledHeight() - 24;
		int spacing = 18;

		drawContext.drawTextWithShadow(
				client.textRenderer,
				Text.literal("H"),
				x,
				y,
				headlights ? 0xFF62C84B : inactive
		);
		drawContext.drawTextWithShadow(
				client.textRenderer,
				Text.literal("F"),
				x + spacing,
				y,
				lowFuel ? 0xFFFFB41F : inactive
		);
		drawContext.drawTextWithShadow(
				client.textRenderer,
				Text.literal("E"),
				x + spacing * 2,
				y,
				engineWarning ? 0xFFFF4141 : inactive
		);
	}


	private static void drawCutCornerPanel(
			net.minecraft.client.gui.DrawContext drawContext,
			int x,
			int y,
			int width,
			int height
	) {
		int border = 0x9A56616B;
		int inside = 0xB52A3036;

		// 2-pixel cut corners give the compact instruments softer Minecraft edges.
		drawContext.fill(x + 2, y, x + width - 2, y + height, border);
		drawContext.fill(x, y + 2, x + width, y + height - 2, border);

		drawContext.fill(x + 2, y + 1, x + width - 2, y + height - 1, inside);
		drawContext.fill(x + 1, y + 2, x + width - 1, y + height - 2, inside);
	}


	private static void drawManualGearStick(net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client, String gear) {
		int width = 58;
		int height = 58;
		int x = client.getWindow().getScaledWidth() - width - 12;
		int y = 10;

		int line = mouseShifterActive ? 0xFFE1E1E1 : 0xFF888888;
		int label = mouseShifterActive ? 0xFFE8E8E8 : 0xFFB0B0B0;

		/*
		 * The gate/labels keep their existing coordinates, while the panel gains
		 * two pixels above 1/3/5 and two pixels below 2/4/6.
		 */
		drawCutCornerPanel(drawContext, x, y - 2, width, height);

		int left = x + 13;
		int middle = x + 28;
		int right = x + 43;
		int top = y + 14;
		int neutral = y + 27;
		int bottom = y + 40;

		// Deliberately simple, hard-pixel Minecraft-style H pattern.
		drawContext.fill(left, top, left + 2, bottom + 1, line);
		drawContext.fill(middle, top, middle + 2, bottom + 1, line);
		drawContext.fill(right, top, right + 2, bottom + 1, line);
		drawContext.fill(left, neutral, right + 2, neutral + 2, line);

		boolean reverseLockout = reverseLockoutLatched;
		boolean showingReverse = mouseShifterActive ? reverseLockout : "R".equals(gear);
		String upperLeftLabel = showingReverse ? "R" : "1";

		drawGearLabel(
				drawContext,
				client,
				upperLeftLabel,
				left - 3,
				y + 3,
				upperLeftLabel.equals(gear),
				showingReverse ? 0xFFFF6262 : 0xFFFFC857,
				label
		);
		drawGearLabel(drawContext, client, "3", middle - 3, y + 3, "3".equals(gear), 0xFFFFC857, label);
		drawGearLabel(drawContext, client, "5", right - 3, y + 3, "5".equals(gear), 0xFFFFC857, label);
		drawGearLabel(drawContext, client, "2", left - 3, y + 44, "2".equals(gear), 0xFFFFC857, label);
		drawGearLabel(drawContext, client, "4", middle - 3, y + 43, "4".equals(gear), 0xFFFFC857, label);
		drawGearLabel(drawContext, client, "6", right - 3, y + 43, "6".equals(gear), 0xFFFFC857, label);

		double px;
		double py;
		if (mouseShifterActive) {
			px = shifterX;
			py = shifterY;
		} else {
			double[] selected = shifterPositionForGear(gear);
			px = selected[0];
			py = selected[1];
		}

		int knobX = (int) Math.round(middle + px * (right - middle));
		int knobY = (int) Math.round(neutral + py * (bottom - neutral));

		// No diagonal "debug" shaft: only the selector itself moves.
		drawContext.fill(knobX - 4, knobY - 4, knobX + 5, knobY + 5, 0xFF151515);
		drawContext.fill(
				knobX - 3,
				knobY - 3,
				knobX + 4,
				knobY + 4,
				mouseShifterActive ? 0xFFFFFFFF : 0xFFFFC857
		);
		drawContext.fill(knobX - 1, knobY - 1, knobX + 2, knobY + 2, 0xFF666666);
	}

	private static void drawGearLabel(
			net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client,
			String text,
			int x,
			int y,
			boolean selected,
			int selectedColor,
			int inactiveColor
	) {
		if (selected) {
			int width = client.textRenderer.getWidth(text);
			drawContext.fill(x - 2, y - 1, x + width + 2, y + 9, 0xFFFFB41F);
			drawContext.drawText(
					client.textRenderer,
					Text.literal(text),
					x,
					y,
					0xFF202020,
					false
			);
			return;
		}

		drawContext.drawText(
				client.textRenderer,
				Text.literal(text),
				x,
				y,
				inactiveColor,
				false
		);
	}

	public static boolean handleShifterMouseButton(int button, int action) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (action == GLFW.GLFW_PRESS) {
			if (client.currentScreen != null
					|| client.player == null
					|| !(client.player.getVehicle() instanceof CarEntity car)
					|| !car.isManualTransmission()
					|| car.getControllingPassenger() != client.player) {
				return false;
			}

			beginMouseShifter(car);
			return true;
		}

		if (action == GLFW.GLFW_RELEASE && mouseShifterActive) {
			finishMouseShifter();
			return true;
		}

		return mouseShifterActive;
	}

	public static void handleCarAttackInput() {
		// Reverse lockout toggling is handled from the attack-key press edge in
		// the client tick. This method remains as the left-click suppression hook.
	}


	public static boolean shouldSuppressCarAttack() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.currentScreen == null
				&& client.player != null
				&& client.player.getVehicle() instanceof CarEntity car
				&& car.getControllingPassenger() == client.player;
	}

	public static boolean shouldSuppressManualUse() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.currentScreen == null
				&& client.player != null
				&& client.player.getVehicle() instanceof CarEntity car
				&& car.isManualTransmission()
				&& car.getControllingPassenger() == client.player
				&& (mouseShifterActive || client.options.useKey.isPressed());
	}

	public static boolean shouldCaptureMouseForShifter() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null
				|| client.player == null
				|| !(client.player.getVehicle() instanceof CarEntity car)
				|| !car.isManualTransmission()
				|| car.getControllingPassenger() != client.player) {
			return false;
		}

		if (!mouseShifterActive && client.options.useKey.isPressed()) {
			beginMouseShifter(car);
		}

		return mouseShifterActive;
	}

	public static boolean isMouseShifterActive() {
		return mouseShifterActive;
	}

	public static void handleShifterMouseDelta(double rawDx, double rawDy) {
		if (!mouseShifterActive) {
			return;
		}

		/*
		 * Pure H-track movement.
		 *
		 * There are no springs, magnets, resistance forces, or intent guessing.
		 * The knob simply obeys the geometry visible on screen:
		 *
		 * - inside a vertical lane: only up/down movement is legal;
		 * - once the knob reaches the neutral bar: left/right movement is legal;
		 * - while on neutral, up/down becomes legal whenever the knob is close
		 *   enough to one of the three gear columns.
		 */
		// Horizontal movement along Neutral is intentionally slower than
		// vertical gate movement so crossing neutral slots takes a deliberate
		// sideways sweep instead of racing across the H-pattern.
		double moveX = Math.max(-30.0, Math.min(30.0, rawDx)) * 0.0065;
		double moveY = Math.max(-30.0, Math.min(30.0, rawDy)) * 0.012;

		/*
		 * On the user's input path, positive raw Y corresponds to moving the
		 * mouse downward on screen, so do not invert this value.
		 */
		int steps = Math.max(1, Math.min(20,
				(int) Math.ceil(Math.max(Math.abs(moveX), Math.abs(moveY)) / 0.035)));

		double stepX = moveX / steps;
		double stepY = moveY / steps;

		for (int i = 0; i < steps; i++) {
			applyPureTrackStep(stepX, stepY);
		}
	}

	private static void applyPureTrackStep(double dx, double dy) {
		final double DETENT_CAPTURE = 0.22;
		final double DETENT_RELEASE = 0.34;
		final double SIDE_ENTRY_Y = 0.46;

		double absX = Math.abs(dx);
		double absY = Math.abs(dy);

		if (!shifterInVerticalLane) {
			/*
			 * Neutral is now continuous again: the knob visibly DRAGS across the
			 * horizontal bar instead of teleporting between three checkpoints.
			 *
			 * The three neutral positions (-1, 0, +1) are strong soft detents.
			 * When the player's horizontal movement slows near one, the knob
			 * settles into it. Continued deliberate movement pushes through.
			 */
			shifterY = 0.0;

			boolean clearlyVertical =
					absY > 0.0008
					&& absY > absX * 1.35;

			double nearest = nearestColumn(shifterX);
			double distanceToNearest = Math.abs(shifterX - nearest);

			if (clearlyVertical && distanceToNearest <= DETENT_CAPTURE) {
				shifterX = nearest;
				neutralSlot = (int) nearest;
				shifterInVerticalLane = true;
				shifterY = Math.max(-1.0, Math.min(1.0, dy));
				return;
			}

			if (absX > 0.00025) {
				/*
				 * Continuous travel. Near a detent, slow the knob somewhat so the
				 * player can feel/see the neutral position without turning it into
				 * a hard wall.
				 */
				double speedScale = distanceToNearest <= DETENT_RELEASE ? 0.62 : 1.0;
				shifterX = Math.max(-1.0, Math.min(1.0, shifterX + dx * speedScale));

				double afterNearest = nearestColumn(shifterX);
				double afterDistance = Math.abs(shifterX - afterNearest);

				/*
				 * If movement is small and we're close to a detent, settle there.
				 * With stronger continued input the knob remains visibly dragged
				 * through and can continue toward the next neutral position.
				 */
				if (afterDistance <= DETENT_CAPTURE && absX < 0.010) {
					shifterX = afterNearest;
				}
				neutralSlot = (int) nearestColumn(shifterX);
			}
			return;
		}

		/*
		 * Inside a vertical gear rail, X remains fixed. But close to Neutral,
		 * a clear sideways gesture can release the stick into the horizontal
		 * bar early, preserving the forgiving side-entry behavior.
		 */
		shifterX = neutralSlot;

		boolean closeToNeutral = Math.abs(shifterY) <= SIDE_ENTRY_Y;
		boolean clearlySideways =
				absX > 0.0007
				&& absX >= absY * 0.72;

		if (closeToNeutral && clearlySideways) {
			shifterY = 0.0;
			shifterInVerticalLane = false;

			// Preserve the same sideways gesture so the knob visibly begins
			// sliding across Neutral immediately.
			shifterX = Math.max(-1.0, Math.min(1.0, shifterX + dx * 0.75));
			neutralSlot = (int) nearestColumn(shifterX);
			return;
		}

		double previousY = shifterY;
		double nextY = Math.max(-1.0, Math.min(1.0, shifterY + dy));

		boolean reachedNeutral =
				(previousY < 0.0 && nextY >= 0.0)
				|| (previousY > 0.0 && nextY <= 0.0)
				|| Math.abs(nextY) < 0.015;

		if (reachedNeutral) {
			shifterY = 0.0;
			shifterInVerticalLane = false;

			// Begin continuous horizontal drag with any X from the same sample.
			shifterX = Math.max(-1.0, Math.min(1.0, shifterX + dx * 0.75));
			neutralSlot = (int) nearestColumn(shifterX);
			return;
		}

		shifterY = nextY;
	}

	private static double alignedColumn(double x, double threshold) {
		double[] columns = {-1.0, 0.0, 1.0};
		double best = Double.NaN;
		double distance = Double.MAX_VALUE;

		for (double column : columns) {
			double d = Math.abs(x - column);
			if (d <= threshold && d < distance) {
				best = column;
				distance = d;
			}
		}
		return best;
	}

	private static double nearestColumn(double x) {
		if (x < -0.5) return -1.0;
		if (x > 0.5) return 1.0;
		return 0.0;
	}

	private static void beginMouseShifter(CarEntity car) {
		mouseShifterActive = true;
		gearAtShiftStart = gearNumberFromDisplay(car.getSelectedGearDisplay());
		reverseLockoutLatched = gearAtShiftStart == -1;
		reverseToggleWasDown = false;
		setShifterPositionFromGear(car.getSelectedGearDisplay());
		shifterInVerticalLane = Math.abs(shifterY) > 0.01;
		neutralSlot = (int) nearestColumn(shifterX);
		if (!shifterInVerticalLane) {
			shifterY = 0.0;
		}
	}

	private static void finishMouseShifter() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null
				&& client.player.getVehicle() instanceof CarEntity car
				&& car.isManualTransmission()) {
			int selectedGear = resolvePureTrackGear();
			String before = car.getSelectedGearDisplay();

			car.selectManualGear(selectedGear);
			String after = car.getSelectedGearDisplay();

			if (!after.equals(before)) {
				ClientPlayNetworking.send(new GearSelectPayload(car.getId(), selectedGear));
				playGearEngageSound(client);
			}
		}
		cancelMouseShifter();
	}

	private static void cancelMouseShifter() {
		mouseShifterActive = false;
		reverseLockoutLatched = false;
		reverseToggleWasDown = false;
		shifterInVerticalLane = false;
		neutralSlot = 0;
		shifterX = 0.0;
		shifterY = 0.0;
	}

	private static void playGearEngageSound(MinecraftClient client) {
		if (client.player != null) {
			client.player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.55F, 0.72F);
		}
	}

	private static void setShifterPositionFromGear(String gear) {
		double[] pos = shifterPositionForGear(gear);
		shifterX = pos[0];
		shifterY = pos[1];
	}

	private static double[] shifterPositionForGear(String gear) {
		return switch (gear) {
			case "1", "R" -> new double[] {-1.0, -1.0};
			case "2" -> new double[] {-1.0, 1.0};
			case "3" -> new double[] {0.0, -1.0};
			case "4" -> new double[] {0.0, 1.0};
			case "5" -> new double[] {1.0, -1.0};
			case "6" -> new double[] {1.0, 1.0};
			default -> new double[] {0.0, 0.0};
		};
	}

	private static int gearNumberFromDisplay(String gear) {
		if ("R".equals(gear)) return -1;
		if ("N".equals(gear)) return 0;
		try {
			return Integer.parseInt(gear);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private static int resolvePureTrackGear() {
		final double NEUTRAL_RELEASE = 0.28;
		final double COLUMN_RELEASE = 0.40;

		// Anywhere around the whole middle bar is Neutral.
		if (Math.abs(shifterY) <= NEUTRAL_RELEASE) {
			return 0;
		}

		double column = alignedColumn(shifterX, COLUMN_RELEASE);
		if (Double.isNaN(column)) {
			return 0;
		}

		boolean lower = shifterY > 0.0;

		/*
		 * Reverse shares the upper-left physical gate with 1st. Left-click
		 * toggles the push-down lockout while right-click is holding the stick:
		 * press once = R, press again = 1.
		 */
		if (!lower && column == -1.0 && reverseLockoutLatched) {
			shifterX = -1.0;
			shifterY = -1.0;
			return -1;
		}

		shifterX = column;
		shifterY = lower ? 1.0 : -1.0;

		if (column == -1.0) return lower ? 2 : 1;
		if (column == 0.0) return lower ? 4 : 3;
		return lower ? 6 : 5;
	}

}
