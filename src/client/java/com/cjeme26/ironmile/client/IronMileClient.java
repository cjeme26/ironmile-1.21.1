package com.cjeme26.ironmile.client;

import com.cjeme26.ironmile.client.render.CarEntityRenderer;
import com.cjeme26.ironmile.client.sound.EngineSoundManager;
import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.network.HeadlightTogglePayload;
import com.cjeme26.ironmile.network.CarInputPayload;
import com.cjeme26.ironmile.network.GearSelectPayload;
import com.cjeme26.ironmile.network.GearShiftPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
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
	private static int lastControlledCarId = -1;
	private static boolean mouseShifterActive = false;
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
		EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEADLIGHT_MARKER, EmptyEntityRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(EngineSoundManager::tick);
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

				/* Apply input before the client world tick for immediate local prediction. */
				car.setInputs(
						(inputMask & CarInputPayload.LEFT) != 0,
						(inputMask & CarInputPayload.RIGHT) != 0,
						(inputMask & CarInputPayload.FORWARD) != 0,
						(inputMask & CarInputPayload.BACK) != 0
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
					&& car.isManualTransmission()
					&& car.getControllingPassenger() == client.player) {

				// Permanent fallback controls while the mouse shifter is tested.
				while (shiftUpKey.wasPressed()) {
					car.manualShift(1);
					ClientPlayNetworking.send(new GearShiftPayload(car.getId(), 1));
				}
				while (shiftDownKey.wasPressed()) {
					car.manualShift(-1);
					ClientPlayNetworking.send(new GearShiftPayload(car.getId(), -1));
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

			String speed = String.format(Locale.ROOT, "%.0f km/h", Math.abs(car.getHorizontalSpeedKmh()));
			String status = car.isManualTransmission()
					? speed
					: speed + "     " + car.getSelectedGearDisplay();

			float scale = 1.35F;
			drawContext.getMatrices().push();
			drawContext.getMatrices().scale(scale, scale, 1.0F);
			int logicalWidth = (int) (client.getWindow().getScaledWidth() / scale);
			int x = (logicalWidth - client.textRenderer.getWidth(status)) / 2;
			drawContext.drawTextWithShadow(client.textRenderer, Text.literal(status), x, 8, 0xFFFFFF);
			drawContext.getMatrices().pop();

			if (car.isManualTransmission()) {
				drawManualGearStick(drawContext, client, car.getSelectedGearDisplay());

			}
		});
	}

	private static void drawManualGearStick(net.minecraft.client.gui.DrawContext drawContext,
			MinecraftClient client, String gear) {
		int width = 64;
		int height = 56;
		int x = client.getWindow().getScaledWidth() - width - 12;
		int y = client.getWindow().getScaledHeight() - height - 12;

		int panelOuter = mouseShifterActive ? 0x92101010 : 0x74101010;
		int panelInner = mouseShifterActive ? 0x7D202020 : 0x5D202020;
		int line = mouseShifterActive ? 0xFFD4D4D4 : 0xA69A9A9A;
		int label = mouseShifterActive ? 0xFFFFFFFF : 0xFFC2C2C2;

		drawContext.fill(x, y, x + width, y + height, panelOuter);
		drawContext.fill(x + 1, y + 1, x + width - 1, y + height - 1, panelInner);

		int left = x + 15;
		int middle = x + 31;
		int right = x + 47;
		int top = y + 15;
		int neutral = y + 28;
		int bottom = y + 41;

		// Deliberately simple, hard-pixel Minecraft-style H pattern.
		drawContext.fill(left, top, left + 2, bottom + 1, line);
		drawContext.fill(middle, top, middle + 2, bottom + 1, line);
		drawContext.fill(right, top, right + 2, bottom + 1, line);
		drawContext.fill(left, neutral, right + 2, neutral + 2, line);

		boolean reverseLockout = reverseLockoutLatched;
		boolean showingReverse = mouseShifterActive ? reverseLockout : "R".equals(gear);
		String upperLeftLabel = showingReverse ? "R" : "1";
		int upperLeftColor = showingReverse ? 0xFFFF5555 : label;

		drawContext.drawText(client.textRenderer, Text.literal(upperLeftLabel), left - 3, y + 3, upperLeftColor, false);
		drawContext.drawText(client.textRenderer, Text.literal("3"), middle - 3, y + 3, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("5"), right - 3, y + 3, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("2"), left - 3, y + 44, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("4"), middle - 3, y + 44, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("6"), right - 3, y + 44, label, false);

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
		drawContext.fill(knobX - 3, knobY - 3, knobX + 4, knobY + 4,
				mouseShifterActive ? 0xFFFFFFFF : 0xFFE0E0E0);
		drawContext.fill(knobX - 1, knobY - 1, knobX + 2, knobY + 2, 0xFF666666);
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
