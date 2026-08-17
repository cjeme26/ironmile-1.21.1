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
	private static double shifterVelocityX = 0.0;
	private static double shifterVelocityY = 0.0;
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

				// Temporary diagnostic so we can verify that Minecraft's Use key is
				// actually reaching the shifter on the user's machine.
				if (mouseShifterActive || client.options.useKey.isPressed()) {
					String inputText = "SHIFT INPUT";
					int inputX = (client.getWindow().getScaledWidth()
							- client.textRenderer.getWidth(inputText)) / 2;
					drawContext.drawTextWithShadow(
							client.textRenderer, Text.literal(inputText), inputX, 28, 0xFFFFD34E);
				}
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
		drawContext.fill(right + 1, neutral, right + 8, neutral + 2, line);
		drawContext.fill(right + 7, neutral, right + 9, bottom + 1, line);

		drawContext.drawText(client.textRenderer, Text.literal("1"), left - 3, y + 3, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("3"), middle - 3, y + 3, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("5"), right - 3, y + 3, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("2"), left - 3, y + 44, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("4"), middle - 3, y + 44, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("6"), right - 3, y + 44, label, false);
		drawContext.drawText(client.textRenderer, Text.literal("R"), right + 6, y + 44, 0xFFFF5555, false);

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

		int knobX = px > 1.20
				? right + 8
				: (int) Math.round(middle + px * (right - middle));
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
		 * Mouse movement is intentionally not mapped 1:1 to the lever.
		 * Velocity, damping and gate resistance make the selector feel heavier
		 * without turning it into obvious input lag.
		 */
		double dx = Math.max(-24.0, Math.min(24.0, rawDx));
		double dy = Math.max(-24.0, Math.min(24.0, rawDy));

		boolean inGate = Math.abs(shifterY) > 0.40;
		double horizontalResistance = inGate ? 0.10 : 0.66;
		double verticalResistance = Math.abs(shifterY) < 0.25 ? 0.52 : 0.70;

		shifterVelocityX = shifterVelocityX * 0.38 + dx * 0.012 * horizontalResistance;
		shifterVelocityY = shifterVelocityY * 0.38 + dy * 0.012 * verticalResistance;

		shifterX += shifterVelocityX;
		shifterY += shifterVelocityY;

		constrainWeightedShifter();
	}

	private static void beginMouseShifter(CarEntity car) {
		mouseShifterActive = true;
		setShifterPositionFromGear(car.getSelectedGearDisplay());
		shifterVelocityX = 0.0;
		shifterVelocityY = 0.0;
		gearAtShiftStart = gearNumberFromDisplay(car.getSelectedGearDisplay());
	}

	private static void finishMouseShifter() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null
				&& client.player.getVehicle() instanceof CarEntity car
				&& car.isManualTransmission()) {
			int selectedGear = resolveWeightedShifterGear();
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
		shifterVelocityX = 0.0;
		shifterVelocityY = 0.0;
	}

	private static void playGearEngageSound(MinecraftClient client) {
		if (client.player != null) {
			// A low-pitched vanilla lever click gives a small, dry mechanical clack.
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
			case "1" -> new double[] {-1.0, -1.0};
			case "2" -> new double[] {-1.0, 1.0};
			case "3" -> new double[] {0.0, -1.0};
			case "4" -> new double[] {0.0, 1.0};
			case "5" -> new double[] {1.0, -1.0};
			case "6" -> new double[] {1.0, 1.0};
			case "R" -> new double[] {1.50, 1.0};
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

	private static void constrainWeightedShifter() {
		shifterX = Math.max(-1.0, Math.min(1.50, shifterX));
		shifterY = Math.max(-1.0, Math.min(1.0, shifterY));

		// The neutral channel has a gentle centering tendency.
		if (Math.abs(shifterY) < 0.28) {
			shifterY *= 0.72;
		}

		/*
		 * Once the lever is entering a gate, sideways movement becomes difficult
		 * and the lever settles toward that gate's rail. To change columns the
		 * player must deliberately return toward neutral first.
		 */
		if (Math.abs(shifterY) > 0.48) {
			double rail;
			if (shifterX > 1.22 && shifterY > 0.0) {
				rail = 1.50; // reverse
			} else if (shifterX < -0.50) {
				rail = -1.0;
			} else if (shifterX < 0.50) {
				rail = 0.0;
			} else {
				rail = 1.0;
			}
			shifterX += (rail - shifterX) * 0.28;
			shifterVelocityX *= 0.32;
		}

		// Soft end-stop resistance.
		if (Math.abs(shifterY) > 0.88) {
			shifterVelocityY *= 0.28;
		}
	}

	private static int resolveWeightedShifterGear() {
		// Releasing in the middle of the pattern explicitly selects neutral.
		if (Math.abs(shifterY) < 0.66) {
			return 0;
		}

		if (shifterX > 1.22 && shifterY > 0.66) {
			return -1;
		}

		int column = shifterX < -0.50 ? 0 : (shifterX < 0.50 ? 1 : 2);
		boolean lower = shifterY > 0.0;
		return switch (column) {
			case 0 -> lower ? 2 : 1;
			case 1 -> lower ? 4 : 3;
			default -> lower ? 6 : 5;
		};
	}

}
