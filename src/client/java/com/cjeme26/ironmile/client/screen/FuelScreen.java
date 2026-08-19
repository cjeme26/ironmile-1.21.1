package com.cjeme26.ironmile.client.screen;

import com.cjeme26.ironmile.screen.FuelScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.Locale;

public class FuelScreen extends HandledScreen<FuelScreenHandler> {
	private static final int VANILLA_BG = 0xFFC6C6C6;
	private static final int VANILLA_LIGHT = 0xFFE6E6E6;
	private static final int VANILLA_MID = 0xFF8B8B8B;
	private static final int VANILLA_DARK = 0xFF373737;
	private static final int VANILLA_TEXT = 0xFF404040;
	private static final int FUEL_BAR = 0xFFFFC857;
	private static final int FUEL_BAR_EMPTY = 0xFF707070;

	public FuelScreen(FuelScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 205;
		this.titleX = 8;
		this.titleY = 7;
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = 111;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;

		drawVanillaPanel(context, left, top, this.backgroundWidth, this.backgroundHeight);

		// Tank information is one clear section.
		drawRecessedPanel(context, left + 7, top + 20, 162, 49);

		int gaugeX = left + 53;
		int gaugeY = top + 38;
		int segmentWidth = 9;
		int gap = 2;
		int segments = 8;
		int filled = (int) Math.round(this.handler.getFuelFraction() * segments);

		for (int i = 0; i < segments; i++) {
			int sx = gaugeX + i * (segmentWidth + gap);
			context.fill(
					sx,
					gaugeY,
					sx + segmentWidth,
					gaugeY + 9,
					i < filled ? FUEL_BAR : FUEL_BAR_EMPTY
			);
		}

		// Fuel item input is a separate section beneath the tank.
		drawVanillaSlot(context, left + 8, top + 83);

		// Vanilla-style player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawVanillaSlot(context, left + 8 + column * 18, top + 123 + row * 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawVanillaSlot(context, left + 8 + column * 18, top + 181);
		}
	}

	private static void drawVanillaPanel(
			DrawContext context,
			int x,
			int y,
			int width,
			int height
	) {
		context.fill(x, y, x + width, y + height, VANILLA_BG);
		context.fill(x, y, x + width, y + 1, VANILLA_LIGHT);
		context.fill(x, y, x + 1, y + height, VANILLA_LIGHT);
		context.fill(x, y + height - 1, x + width, y + height, VANILLA_DARK);
		context.fill(x + width - 1, y, x + width, y + height, VANILLA_DARK);
	}

	private static void drawRecessedPanel(
			DrawContext context,
			int x,
			int y,
			int width,
			int height
	) {
		context.fill(x, y, x + width, y + height, VANILLA_MID);
		context.fill(x, y, x + width, y + 1, VANILLA_DARK);
		context.fill(x, y, x + 1, y + height, VANILLA_DARK);
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, VANILLA_BG);
		context.fill(x + 1, y + height - 1, x + width, y + height, VANILLA_LIGHT);
		context.fill(x + width - 1, y + 1, x + width, y + height, VANILLA_LIGHT);
	}

	private static void drawVanillaSlot(DrawContext context, int x, int y) {
		context.fill(x - 1, y - 1, x + 17, y + 17, VANILLA_DARK);
		context.fill(x, y, x + 16, y + 16, VANILLA_MID);
		context.fill(x + 16, y, x + 17, y + 17, VANILLA_LIGHT);
		context.fill(x, y + 16, x + 17, y + 17, VANILLA_LIGHT);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(
				this.textRenderer,
				this.title,
				this.titleX,
				this.titleY,
				VANILLA_TEXT,
				false
		);

		context.drawText(
				this.textRenderer,
				Text.translatable("gui.ironmile.tank"),
				9,
				23,
				VANILLA_TEXT,
				false
		);

		context.drawText(
				this.textRenderer,
				Text.literal("F"),
				39,
				38,
				VANILLA_TEXT,
				false
		);
		context.drawText(
				this.textRenderer,
				Text.literal("E"),
				148,
				38,
				VANILLA_TEXT,
				false
		);

		double liters = this.handler.getFuelMilliliters() / 1000.0;
		double capacity = this.handler.getFuelCapacityMilliliters() / 1000.0;
		String amount = String.format(Locale.ROOT, "%.1f / %.1f L", liters, capacity);
		int amountX = (this.backgroundWidth - this.textRenderer.getWidth(amount)) / 2;
		context.drawText(
				this.textRenderer,
				Text.literal(amount),
				amountX,
				54,
				VANILLA_TEXT,
				false
		);

		context.drawText(
				this.textRenderer,
				Text.translatable("gui.ironmile.fuel_input"),
				8,
				73,
				VANILLA_TEXT,
				false
		);

		context.drawText(
				this.textRenderer,
				this.playerInventoryTitle,
				this.playerInventoryTitleX,
				this.playerInventoryTitleY,
				VANILLA_TEXT,
				false
		);
	}
}
