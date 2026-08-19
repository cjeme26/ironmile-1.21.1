package com.cjeme26.ironmile.client.screen;

import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler;
import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler.Page;
import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler.RecipeBookEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Vanilla-style Alpha 2 interface for the Mechanic's Workbench.
 * The right-hand recipe book is always populated; there is no discovery system.
 */
public final class MechanicsWorkbenchScreen extends HandledScreen<MechanicsWorkbenchScreenHandler> {
	private static final int BG = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFE6E6E6;
	private static final int MID = 0xFF8B8B8B;
	private static final int DARK = 0xFF373737;
	private static final int TEXT = 0xFF404040;
	private static final int TAB_SELECTED = 0xFFE0E0E0;
	private static final int TAB_IDLE = 0xFFAFAFAF;
	private static final int RECIPE_AVAILABLE = 0xFFD7D7D7;
	private static final int RECIPE_UNAVAILABLE = 0xFF8F8F8F;
	private static final int RECIPE_SELECTED = 0xFFF1C75B;

	private static final int PANEL_WIDTH = 286;
	private static final int PANEL_HEIGHT = 220;
	private static final int RECIPE_X = 181;
	private static final int RECIPE_Y = 22;
	private static final int RECIPE_W = 98;
	private static final int RECIPE_H = 190;

	private int selectedRecipeId = MechanicsWorkbenchScreenHandler.BODY_HATCHBACK;

	public MechanicsWorkbenchScreen(
			MechanicsWorkbenchScreenHandler handler,
			PlayerInventory inventory,
			Text title
	) {
		super(handler, inventory, title);
		this.backgroundWidth = PANEL_WIDTH;
		this.backgroundHeight = PANEL_HEIGHT;
		this.titleX = 8;
		this.titleY = 7;
		this.playerInventoryTitleX = 8;
		this.playerInventoryTitleY = 128;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawRecipeTooltip(context, mouseX, mouseY);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;
		drawVanillaPanel(context, left, top, this.backgroundWidth, this.backgroundHeight);

		// Visually separate the vehicle work area from the always-visible recipe book.
		drawRecessedPanel(context, left + RECIPE_X, top + RECIPE_Y, RECIPE_W, RECIPE_H);

		Page page = this.handler.getPage();
		if (page == Page.BODY) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 4; column++) {
					drawVanillaSlot(context, left + 34 + column * 18, top + 42 + row * 18);
				}
			}
		} else if (page == Page.PARTS) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					drawVanillaSlot(context, left + 43 + column * 18, top + 42 + row * 18);
				}
			}
		} else {
			drawVanillaSlot(context, left + 23, top + 58);
			drawVanillaSlot(context, left + 65, top + 58);
			drawVanillaSlot(context, left + 107, top + 58);
		}

		// Shared output slot + simple assembly/crafting arrow.
		drawVanillaSlot(context, left + 148, top + 58);
		context.fill(left + 126, top + 64, left + 141, top + 67, DARK);
		context.fill(left + 137, top + 61, left + 141, top + 70, DARK);

		// Player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawVanillaSlot(context, left + 8 + column * 18, top + 139 + row * 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawVanillaSlot(context, left + 8 + column * 18, top + 197);
		}

		this.drawRecipeBook(context, mouseX, mouseY);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TEXT, false);
		context.drawText(
				this.textRenderer,
				this.playerInventoryTitle,
				this.playerInventoryTitleX,
				this.playerInventoryTitleY,
				TEXT,
				false
		);

		this.drawTab(context, 8, 21, 50, "gui.ironmile.workbench.body", Page.BODY);
		this.drawTab(context, 60, 21, 50, "gui.ironmile.workbench.parts", Page.PARTS);
		this.drawTab(context, 112, 21, 61, "gui.ironmile.workbench.assembly", Page.ASSEMBLY);

		if (this.handler.getPage() == Page.ASSEMBLY) {
			context.drawText(this.textRenderer, Text.translatable("gui.ironmile.workbench.body_slot"), 20, 82, TEXT, false);
			context.drawText(this.textRenderer, Text.translatable("gui.ironmile.workbench.transmission_slot"), 51, 92, TEXT, false);
			context.drawText(this.textRenderer, Text.translatable("gui.ironmile.workbench.tires_slot"), 106, 82, TEXT, false);
		}

		context.drawText(
				this.textRenderer,
				Text.translatable("gui.ironmile.workbench.recipes"),
				RECIPE_X + 7,
				RECIPE_Y + 6,
				TEXT,
				false
		);

		RecipeBookEntry selected = this.findRecipe(this.selectedRecipeId);
		if (selected != null && selected.page() == this.handler.getPage()) {
			int detailY = RECIPE_Y + 102;
			context.drawTextWrapped(this.textRenderer, selected.name(), RECIPE_X + 7, detailY, 84, TEXT);
			context.drawTextWrapped(
					this.textRenderer,
					selected.detail(),
					RECIPE_X + 7,
					detailY + 23,
					84,
					0xFF666666
			);
		}
	}

	private void drawTab(DrawContext context, int x, int y, int width, String key, Page page) {
		boolean selected = this.handler.getPage() == page;
		context.fill(x, y, x + width, y + 14, selected ? TAB_SELECTED : TAB_IDLE);
		context.fill(x, y, x + width, y + 1, LIGHT);
		context.fill(x, y, x + 1, y + 14, LIGHT);
		context.fill(x, y + 13, x + width, y + 14, DARK);
		context.fill(x + width - 1, y, x + width, y + 14, DARK);
		Text label = Text.translatable(key);
		int labelX = x + (width - this.textRenderer.getWidth(label)) / 2;
		context.drawText(this.textRenderer, label, labelX, y + 3, TEXT, false);
	}

	private void drawRecipeBook(DrawContext context, int mouseX, int mouseY) {
		List<RecipeBookEntry> entries = MechanicsWorkbenchScreenHandler.getRecipeBookEntries(this.handler.getPage());
		int startX = this.x + RECIPE_X + 6;
		int startY = this.y + RECIPE_Y + 21;
		for (int index = 0; index < entries.size(); index++) {
			RecipeBookEntry entry = entries.get(index);
			int column = index % 4;
			int row = index / 4;
			int cellX = startX + column * 22;
			int cellY = startY + row * 22;
			boolean informational = entry.page() == Page.ASSEMBLY;
			boolean available = informational
					|| (this.client != null && this.client.player != null
					&& this.handler.canAutofillRecipe(this.client.player, entry.id()));
			int cellColor = entry.id() == this.selectedRecipeId
					? RECIPE_SELECTED
					: (available ? RECIPE_AVAILABLE : RECIPE_UNAVAILABLE);
			context.fill(cellX, cellY, cellX + 20, cellY + 20, DARK);
			context.fill(cellX + 1, cellY + 1, cellX + 19, cellY + 19, cellColor);
			context.drawItem(entry.icon(), cellX + 2, cellY + 2);
		}
	}

	private void drawRecipeTooltip(DrawContext context, int mouseX, int mouseY) {
		RecipeBookEntry hovered = this.recipeAt(mouseX, mouseY);
		if (hovered == null) return;
		context.drawTooltip(this.textRenderer, List.of(hovered.name(), hovered.detail()), mouseX, mouseY);
	}

	private RecipeBookEntry recipeAt(double mouseX, double mouseY) {
		List<RecipeBookEntry> entries = MechanicsWorkbenchScreenHandler.getRecipeBookEntries(this.handler.getPage());
		int startX = this.x + RECIPE_X + 6;
		int startY = this.y + RECIPE_Y + 21;
		for (int index = 0; index < entries.size(); index++) {
			int column = index % 4;
			int row = index / 4;
			int cellX = startX + column * 22;
			int cellY = startY + row * 22;
			if (mouseX >= cellX && mouseX < cellX + 20 && mouseY >= cellY && mouseY < cellY + 20) {
				return entries.get(index);
			}
		}
		return null;
	}

	private RecipeBookEntry findRecipe(int recipeId) {
		for (RecipeBookEntry entry : MechanicsWorkbenchScreenHandler.getRecipeBookEntries(this.handler.getPage())) {
			if (entry.id() == recipeId) return entry;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			Page clickedPage = this.tabAt(mouseX, mouseY);
			if (clickedPage != null) {
				this.handler.setPage(clickedPage.id());
				List<RecipeBookEntry> entries = MechanicsWorkbenchScreenHandler.getRecipeBookEntries(clickedPage);
				this.selectedRecipeId = entries.isEmpty() ? -1 : entries.getFirst().id();
				if (this.client != null && this.client.interactionManager != null) {
					this.client.interactionManager.clickButton(this.handler.syncId, clickedPage.id());
				}
				return true;
			}

			RecipeBookEntry recipe = this.recipeAt(mouseX, mouseY);
			if (recipe != null) {
				this.selectedRecipeId = recipe.id();
				if (recipe.page() != Page.ASSEMBLY
						&& this.client != null
						&& this.client.player != null
						&& this.client.interactionManager != null
						&& this.handler.canAutofillRecipe(this.client.player, recipe.id())) {
					this.client.interactionManager.clickButton(
							this.handler.syncId,
							MechanicsWorkbenchScreenHandler.RECIPE_BUTTON_BASE + recipe.id()
					);
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private Page tabAt(double mouseX, double mouseY) {
		double localX = mouseX - this.x;
		double localY = mouseY - this.y;
		if (localY < 21 || localY >= 35) return null;
		if (localX >= 8 && localX < 58) return Page.BODY;
		if (localX >= 60 && localX < 110) return Page.PARTS;
		if (localX >= 112 && localX < 173) return Page.ASSEMBLY;
		return null;
	}

	private static void drawVanillaPanel(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, BG);
		context.fill(x, y, x + width, y + 1, LIGHT);
		context.fill(x, y, x + 1, y + height, LIGHT);
		context.fill(x, y + height - 1, x + width, y + height, DARK);
		context.fill(x + width - 1, y, x + width, y + height, DARK);
	}

	private static void drawRecessedPanel(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, MID);
		context.fill(x, y, x + width, y + 1, DARK);
		context.fill(x, y, x + 1, y + height, DARK);
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, BG);
		context.fill(x + 1, y + height - 1, x + width, y + height, LIGHT);
		context.fill(x + width - 1, y + 1, x + width, y + height, LIGHT);
	}

	private static void drawVanillaSlot(DrawContext context, int x, int y) {
		context.fill(x - 1, y - 1, x + 17, y + 17, DARK);
		context.fill(x, y, x + 16, y + 16, MID);
		context.fill(x + 16, y, x + 17, y + 17, LIGHT);
		context.fill(x, y + 16, x + 17, y + 17, LIGHT);
	}
}
