package com.cjeme26.ironmile.client.screen;

import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler;
import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler.Page;
import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler.RecipeBookEntry;
import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler.RecipeRequirement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-style Mechanic's Workbench UI.
 * The right-hand catalogue is always visible and never uses recipe discovery.
 */
public final class MechanicsWorkbenchScreen extends HandledScreen<MechanicsWorkbenchScreenHandler> {
	private static final Identifier CRAFTING_TABLE_TEXTURE =
			Identifier.ofVanilla("textures/gui/container/crafting_table.png");

	private static final int BG = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFE6E6E6;
	private static final int MID = 0xFF8B8B8B;
	private static final int DARK = 0xFF373737;
	private static final int TEXT = 0xFF404040;
	private static final int TAB_SELECTED = 0xFFE0E0E0;
	private static final int TAB_IDLE = 0xFFAFAFAF;
	private static final int RECIPE_AVAILABLE = 0xFFD7D7D7;
	private static final int RECIPE_AVAILABLE_HOVER = 0xFFE4E4E4;
	private static final int RECIPE_UNAVAILABLE = 0xFF747474;
	private static final int RECIPE_UNAVAILABLE_HOVER = 0xFF818181;
	private static final int RECIPE_SELECTED = 0xFFF1C75B;
	private static final int REQUIREMENT_AVAILABLE = 0xFFB5B5B5;
	private static final int REQUIREMENT_MISSING = 0xFF9A5959;

	private static final int PANEL_WIDTH = 286;
	private static final int PANEL_HEIGHT = 220;
	private static final int RECIPE_X = 181;
	private static final int RECIPE_Y = 22;
	private static final int RECIPE_W = 98;
	private static final int RECIPE_H = 190;

	private int selectedBodyRecipeId = MechanicsWorkbenchScreenHandler.BODY_HATCHBACK;
	private int selectedPartsRecipeId = MechanicsWorkbenchScreenHandler.PART_SUMMER_TIRE;
	private int selectedAssemblyRecipeId = MechanicsWorkbenchScreenHandler.ASSEMBLY_HATCHBACK;

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
		this.drawCatalogueTooltip(context, mouseX, mouseY);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;
		drawVanillaPanel(context, left, top, this.backgroundWidth, this.backgroundHeight);
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
			drawVanillaSlot(context, left + 35, top + 48);
			drawVanillaSlot(context, left + 35, top + 82);
			drawVanillaSlot(context, left + 91, top + 65);
		}

		if (page == Page.ASSEMBLY) {
			drawVanillaSlot(context, left + 148, top + 65);
			context.drawTexture(
					CRAFTING_TABLE_TEXTURE,
					left + 119,
					top + 66,
					90.0F,
					35.0F,
					22,
					15,
					256,
					256
			);
		} else {
			drawVanillaSlot(context, left + 148, top + 58);
			context.drawTexture(
					CRAFTING_TABLE_TEXTURE,
					left + 123,
					top + 59,
					90.0F,
					35.0F,
					22,
					15,
					256,
					256
			);
		}

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
			this.drawCenteredSlotLabel(context, 35, 38, "gui.ironmile.workbench.body_slot");
			this.drawCenteredSlotLabel(context, 35, 72, "gui.ironmile.workbench.transmission_short");
			this.drawCenteredSlotLabel(context, 91, 55, "gui.ironmile.workbench.tires_slot");
		}

		context.drawText(
				this.textRenderer,
				Text.translatable("gui.ironmile.workbench.recipes"),
				RECIPE_X + 7,
				RECIPE_Y + 6,
				TEXT,
				false
		);

		RecipeBookEntry selected = this.findRecipe(this.getSelectedRecipeId());
		if (selected != null && selected.page() == this.handler.getPage()) {
			int detailY = RECIPE_Y + 102;
			context.drawTextWrapped(this.textRenderer, selected.name(), RECIPE_X + 7, detailY, 84, TEXT);
			context.drawText(
					this.textRenderer,
					Text.translatable("gui.ironmile.workbench.required"),
					RECIPE_X + 7,
					detailY + 20,
					TEXT,
					false
			);
			this.drawRequirements(context, selected.id(), detailY + 32);
		}
	}

	private void drawCenteredSlotLabel(DrawContext context, int slotX, int y, String key) {
		Text label = Text.translatable(key);
		int labelX = slotX + (16 - this.textRenderer.getWidth(label)) / 2;
		context.drawText(this.textRenderer, label, labelX, y, TEXT, false);
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
		int selectedId = this.getSelectedRecipeId();

		for (int index = 0; index < entries.size(); index++) {
			RecipeBookEntry entry = entries.get(index);
			int column = index % 4;
			int row = index / 4;
			int cellX = startX + column * 22;
			int cellY = startY + row * 22;
			boolean available = this.client != null
					&& this.client.player != null
					&& this.handler.canCraftRecipe(this.client.player, entry.id());
			boolean hovered = mouseX >= cellX && mouseX < cellX + 20
					&& mouseY >= cellY && mouseY < cellY + 20;

			int cellColor;
			if (entry.id() == selectedId) {
				cellColor = RECIPE_SELECTED;
			} else if (available) {
				cellColor = hovered ? RECIPE_AVAILABLE_HOVER : RECIPE_AVAILABLE;
			} else {
				cellColor = hovered ? RECIPE_UNAVAILABLE_HOVER : RECIPE_UNAVAILABLE;
			}

			context.fill(cellX, cellY, cellX + 20, cellY + 20, DARK);
			context.fill(cellX + 1, cellY + 1, cellX + 19, cellY + 19, cellColor);
			context.drawItem(entry.icon(), cellX + 2, cellY + 2);
			if (!available && entry.id() != selectedId) {
				context.fill(cellX + 2, cellY + 2, cellX + 18, cellY + 18, 0x28000000);
			}
		}
	}

	private void drawRequirements(DrawContext context, int recipeId, int startY) {
		if (this.client == null || this.client.player == null) return;
		List<RecipeRequirement> requirements = MechanicsWorkbenchScreenHandler.getRecipeRequirements(recipeId);
		int startX = RECIPE_X + 7;

		for (int index = 0; index < requirements.size(); index++) {
			RecipeRequirement requirement = requirements.get(index);
			int column = index % 4;
			int row = index / 4;
			int x = startX + column * 21;
			int y = startY + row * 22;
			int available = this.handler.getAvailableCount(this.client.player, recipeId, requirement);
			boolean hasEnough = available >= requirement.required();

			context.fill(x, y, x + 20, y + 20, DARK);
			context.fill(
					x + 1,
					y + 1,
					x + 19,
					y + 19,
					hasEnough ? REQUIREMENT_AVAILABLE : REQUIREMENT_MISSING
			);
			context.drawItem(requirement.icon(), x + 2, y + 2);
			if (!hasEnough) {
				// Dim the icon slightly while keeping it readable over the red slot.
				context.fill(x + 2, y + 2, x + 18, y + 18, 0x48000000);
			}

			if (requirement.required() > 1) {
				String count = Integer.toString(requirement.required());
				int countX = x + 18 - this.textRenderer.getWidth(count);
				int countY = y + 11;

				/*
				 * Item rendering uses its own depth. Explicitly lift this overlay
				 * above it so the quantity can never disappear underneath the icon.
				 */
				context.getMatrices().push();
				context.getMatrices().translate(0.0F, 0.0F, 300.0F);
				context.fill(countX - 1, countY - 1, x + 19, y + 19, 0xA0000000);
				context.drawText(this.textRenderer, count, countX, countY, 0xFFFFFFFF, true);
				context.getMatrices().pop();
			}
		}
	}

	private void drawCatalogueTooltip(DrawContext context, int mouseX, int mouseY) {
		RecipeBookEntry hoveredRecipe = this.recipeAt(mouseX, mouseY);
		if (hoveredRecipe != null) {
			List<Text> lines = new ArrayList<>();
			lines.add(hoveredRecipe.name());
			boolean available = this.client != null
					&& this.client.player != null
					&& this.handler.canCraftRecipe(this.client.player, hoveredRecipe.id());
			if (!available) {
				lines.add(Text.translatable("gui.ironmile.workbench.missing_materials").formatted(Formatting.RED));
			}
			context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
			return;
		}

		RequirementHover hoveredRequirement = this.requirementAt(mouseX, mouseY);
		if (hoveredRequirement == null) return;

		List<Text> lines = new ArrayList<>();
		lines.add(hoveredRequirement.requirement().label());
		Text countLine = Text.translatable(
				"gui.ironmile.workbench.have_need",
				hoveredRequirement.available(),
				hoveredRequirement.requirement().required()
		);
		if (hoveredRequirement.available() < hoveredRequirement.requirement().required()) {
			countLine = countLine.copy().formatted(Formatting.RED);
		} else {
			countLine = countLine.copy().formatted(Formatting.GRAY);
		}
		lines.add(countLine);
		context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
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

	private RequirementHover requirementAt(double mouseX, double mouseY) {
		if (this.client == null || this.client.player == null) return null;
		int recipeId = this.getSelectedRecipeId();
		RecipeBookEntry selected = this.findRecipe(recipeId);
		if (selected == null) return null;

		int detailY = this.y + RECIPE_Y + 102;
		int startX = this.x + RECIPE_X + 7;
		int startY = detailY + 32;
		List<RecipeRequirement> requirements = MechanicsWorkbenchScreenHandler.getRecipeRequirements(recipeId);
		for (int index = 0; index < requirements.size(); index++) {
			int column = index % 4;
			int row = index / 4;
			int cellX = startX + column * 21;
			int cellY = startY + row * 22;
			if (mouseX >= cellX && mouseX < cellX + 20 && mouseY >= cellY && mouseY < cellY + 20) {
				RecipeRequirement requirement = requirements.get(index);
				return new RequirementHover(
						requirement,
						this.handler.getAvailableCount(this.client.player, recipeId, requirement)
				);
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
				if (this.client != null && this.client.interactionManager != null) {
					this.client.interactionManager.clickButton(this.handler.syncId, clickedPage.id());
				}
				return true;
			}

			RecipeBookEntry recipe = this.recipeAt(mouseX, mouseY);
			if (recipe != null) {
				this.setSelectedRecipeId(recipe.page(), recipe.id());
				if (this.client != null
						&& this.client.player != null
						&& this.client.interactionManager != null
						&& this.handler.canAutofillRecipe(this.client.player, recipe.id())) {
					int base = Screen.hasShiftDown()
							? MechanicsWorkbenchScreenHandler.RECIPE_SHIFT_BUTTON_BASE
							: MechanicsWorkbenchScreenHandler.RECIPE_BUTTON_BASE;
					this.client.interactionManager.clickButton(this.handler.syncId, base + recipe.id());
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private int getSelectedRecipeId() {
		return switch (this.handler.getPage()) {
			case BODY -> this.selectedBodyRecipeId;
			case PARTS -> this.selectedPartsRecipeId;
			case ASSEMBLY -> this.selectedAssemblyRecipeId;
		};
	}

	private void setSelectedRecipeId(Page page, int recipeId) {
		switch (page) {
			case BODY -> this.selectedBodyRecipeId = recipeId;
			case PARTS -> this.selectedPartsRecipeId = recipeId;
			case ASSEMBLY -> this.selectedAssemblyRecipeId = recipeId;
		}
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

	private record RequirementHover(RecipeRequirement requirement, int available) {
	}
}
