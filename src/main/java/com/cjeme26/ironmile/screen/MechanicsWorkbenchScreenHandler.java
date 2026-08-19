package com.cjeme26.ironmile.screen;

import com.cjeme26.ironmile.block.ModBlocks;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.item.CarItem;
import com.cjeme26.ironmile.item.ModItems;
import com.cjeme26.ironmile.item.TireSetItem;
import com.cjeme26.ironmile.item.TransmissionItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Iron Mile's intentionally small Alpha 2 vehicle-building system.
 *
 * <p>The workbench owns its recipes instead of using Minecraft's normal crafting
 * recipe book. That lets the Body page use a 3x4 grid and keeps every vehicle
 * recipe visible from the moment the block is opened.</p>
 */
public final class MechanicsWorkbenchScreenHandler extends ScreenHandler {
	public enum Page {
		BODY(0),
		PARTS(1),
		ASSEMBLY(2);

		private final int id;

		Page(int id) {
			this.id = id;
		}

		public int id() {
			return this.id;
		}

		public static Page fromId(int id) {
			return switch (id) {
				case 1 -> PARTS;
				case 2 -> ASSEMBLY;
				default -> BODY;
			};
		}
	}

	public record RecipeBookEntry(int id, Page page, ItemStack icon, Text name, Text detail) {
	}

	/**
	 * Compact recipe-catalogue requirement. Assembly can accept alternatives.
	 */
	public record RecipeRequirement(
			ItemStack icon,
			List<Item> options,
			int required,
			Text label
	) {
		public boolean accepts(ItemStack stack) {
			if (stack.isEmpty()) return false;
			for (Item option : this.options) {
				if (stack.isOf(option)) return true;
			}
			return false;
		}
	}

	public static final int RECIPE_BUTTON_BASE = 1000;
	public static final int RECIPE_SHIFT_BUTTON_BASE = 2000;

	public static final int BODY_HATCHBACK = 0;

	public static final int PART_SUMMER_TIRE = 10;
	public static final int PART_ALL_SEASON_TIRE = 11;
	public static final int PART_WINTER_TIRE = 12;
	public static final int PART_SUMMER_SET = 13;
	public static final int PART_ALL_SEASON_SET = 14;
	public static final int PART_WINTER_SET = 15;
	public static final int PART_UNPACK_SUMMER_SET = 16;
	public static final int PART_UNPACK_ALL_SEASON_SET = 17;
	public static final int PART_UNPACK_WINTER_SET = 18;
	public static final int PART_MANUAL_TRANSMISSION = 19;
	public static final int PART_AUTOMATIC_TRANSMISSION = 20;

	public static final int ASSEMBLY_HATCHBACK = 30;

	private static final int BODY_START = 0;
	private static final int BODY_END = 12;
	private static final int PARTS_START = 12;
	private static final int PARTS_END = 21;
	private static final int ASSEMBLY_START = 21;
	private static final int ASSEMBLY_END = 24;
	private static final int OUTPUT_SLOT = 24;
	private static final int PLAYER_START = 25;
	private static final int PLAYER_MAIN_END = 52;
	private static final int HOTBAR_START = 52;
	private static final int PLAYER_END = 61;

	private final ScreenHandlerContext context;
	private final PlayerInventory playerInventory;
	private final SimpleInventory bodyGrid = new SimpleInventory(12);
	private final SimpleInventory partsGrid = new SimpleInventory(9);
	private final SimpleInventory assemblyGrid = new SimpleInventory(3);
	private final SimpleInventory resultInventory = new SimpleInventory(1);
	private Page page = Page.BODY;
	private int currentRecipeId = -1;
	private boolean changingIngredients;

	public MechanicsWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public MechanicsWorkbenchScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			ScreenHandlerContext context
	) {
		super(ModScreenHandlers.MECHANICS_WORKBENCH, syncId);
		this.context = context;
		this.playerInventory = playerInventory;

		this.bodyGrid.addListener(inventory -> this.updateResult());
		this.partsGrid.addListener(inventory -> this.updateResult());
		this.assemblyGrid.addListener(inventory -> this.updateResult());

		// Body: 3 high x 4 long.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 4; column++) {
				this.addSlot(new PageSlot(
						this.bodyGrid,
						column + row * 4,
						34 + column * 18,
						42 + row * 18,
						Page.BODY
				));
			}
		}

		// Parts: familiar 3x3 grid.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				this.addSlot(new PageSlot(
						this.partsGrid,
						column + row * 3,
						43 + column * 18,
						42 + row * 18,
						Page.PARTS
				));
			}
		}

		// Assembly: Body + Transmission + Tire Set.
		this.addSlot(new AssemblySlot(this.assemblyGrid, 0, 23, 58, AssemblyKind.BODY));
		this.addSlot(new AssemblySlot(this.assemblyGrid, 1, 65, 58, AssemblyKind.TRANSMISSION));
		this.addSlot(new AssemblySlot(this.assemblyGrid, 2, 107, 58, AssemblyKind.TIRES));

		this.addSlot(new Slot(this.resultInventory, 0, 148, 58) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				super.onTakeItem(player, stack);
				MechanicsWorkbenchScreenHandler.this.craftCurrentRecipe(player);
			}
		});

		// Player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(
						playerInventory,
						column + row * 9 + 9,
						8 + column * 18,
						139 + row * 18
				));
			}
		}

		// Hotbar.
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 197));
		}

		this.updateResult();
	}

	public Page getPage() {
		return this.page;
	}

	public void setPage(int pageId) {
		this.page = Page.fromId(pageId);
		this.updateResult();
	}

	public Inventory getBodyGrid() {
		return this.bodyGrid;
	}

	public Inventory getPartsGrid() {
		return this.partsGrid;
	}

	public Inventory getAssemblyGrid() {
		return this.assemblyGrid;
	}

	public static List<RecipeBookEntry> getRecipeBookEntries(Page page) {
		List<RecipeBookEntry> entries = new ArrayList<>();
		if (page == Page.BODY) {
			entries.add(entry(
					BODY_HATCHBACK,
					page,
					ModItems.HATCHBACK_BODY,
					"recipe.ironmile.hatchback_body",
					"recipe.ironmile.hatchback_body.detail"
			));
			return entries;
		}

		if (page == Page.PARTS) {
			entries.add(entry(PART_SUMMER_TIRE, page, ModItems.SUMMER_TIRES,
					"recipe.ironmile.summer_tire", "recipe.ironmile.single_tire.detail"));
			entries.add(entry(PART_ALL_SEASON_TIRE, page, ModItems.ALL_SEASON_TIRES,
					"recipe.ironmile.all_season_tire", "recipe.ironmile.single_tire.detail"));
			entries.add(entry(PART_WINTER_TIRE, page, ModItems.WINTER_TIRES,
					"recipe.ironmile.winter_tire", "recipe.ironmile.single_tire.detail"));
			entries.add(entry(PART_SUMMER_SET, page, ModItems.SUMMER_TIRE_SET,
					"recipe.ironmile.summer_tire_set", "recipe.ironmile.tire_set.detail"));
			entries.add(entry(PART_ALL_SEASON_SET, page, ModItems.ALL_SEASON_TIRE_SET,
					"recipe.ironmile.all_season_tire_set", "recipe.ironmile.tire_set.detail"));
			entries.add(entry(PART_WINTER_SET, page, ModItems.WINTER_TIRE_SET,
					"recipe.ironmile.winter_tire_set", "recipe.ironmile.tire_set.detail"));
			entries.add(entry(PART_UNPACK_SUMMER_SET, page, new ItemStack(ModItems.SUMMER_TIRES, 4),
					"recipe.ironmile.unpack_summer_tire_set", "recipe.ironmile.unpack_tire_set.detail"));
			entries.add(entry(PART_UNPACK_ALL_SEASON_SET, page, new ItemStack(ModItems.ALL_SEASON_TIRES, 4),
					"recipe.ironmile.unpack_all_season_tire_set", "recipe.ironmile.unpack_tire_set.detail"));
			entries.add(entry(PART_UNPACK_WINTER_SET, page, new ItemStack(ModItems.WINTER_TIRES, 4),
					"recipe.ironmile.unpack_winter_tire_set", "recipe.ironmile.unpack_tire_set.detail"));
			entries.add(entry(PART_MANUAL_TRANSMISSION, page, ModItems.MANUAL_TRANSMISSION,
					"recipe.ironmile.manual_transmission", "recipe.ironmile.manual_transmission.detail"));
			entries.add(entry(PART_AUTOMATIC_TRANSMISSION, page, ModItems.AUTOMATIC_TRANSMISSION,
					"recipe.ironmile.automatic_transmission", "recipe.ironmile.automatic_transmission.detail"));
			return entries;
		}

		entries.add(entry(
				ASSEMBLY_HATCHBACK,
				page,
				ModItems.CAR,
				"recipe.ironmile.yellow_hatchback_assembly",
				"recipe.ironmile.yellow_hatchback_assembly.detail"
		));
		return entries;
	}

	private static RecipeBookEntry entry(int id, Page page, Item item, String name, String detail) {
		return entry(id, page, new ItemStack(item), name, detail);
	}

	private static RecipeBookEntry entry(int id, Page page, ItemStack icon, String name, String detail) {
		return new RecipeBookEntry(id, page, icon, Text.translatable(name), Text.translatable(detail));
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (id >= 0 && id <= 2) {
			this.setPage(id);
			return true;
		}

		if (id >= RECIPE_SHIFT_BUTTON_BASE) {
			return this.autofillRecipe(player, id - RECIPE_SHIFT_BUTTON_BASE, true);
		}

		if (id >= RECIPE_BUTTON_BASE) {
			return this.autofillRecipe(player, id - RECIPE_BUTTON_BASE, false);
		}

		return super.onButtonClick(player, id);
	}

	public boolean canAutofillRecipe(PlayerEntity player, int recipeId) {
		return this.getMaximumAutofillCrafts(player, recipeId) >= 1;
	}

	public boolean canCraftRecipe(PlayerEntity player, int recipeId) {
		List<RecipeRequirement> requirements = getRecipeRequirements(recipeId);
		if (requirements.isEmpty()) return false;
		for (RecipeRequirement requirement : requirements) {
			if (this.getAvailableCount(player, recipeId, requirement) < requirement.required()) return false;
		}
		return true;
	}

	public int getAvailableCount(PlayerEntity player, int recipeId, RecipeRequirement requirement) {
		int count = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (requirement.accepts(stack)) count += stack.getCount();
		}

		Inventory active = this.getInventoryForRecipe(recipeId);
		if (active != null) {
			for (int i = 0; i < active.size(); i++) {
				ItemStack stack = active.getStack(i);
				if (requirement.accepts(stack)) count += stack.getCount();
			}
		}
		return count;
	}

	private boolean autofillRecipe(PlayerEntity player, int recipeId, boolean fillMaximum) {
		Page targetPage = pageForRecipe(recipeId);
		if (targetPage == null || targetPage != this.page || targetPage == Page.ASSEMBLY) return false;

		List<Item> ingredients = this.getAutofillIngredients(recipeId);
		if (ingredients == null) return false;

		int crafts = fillMaximum ? this.getMaximumAutofillCrafts(player, recipeId) : 1;
		if (crafts < 1) return false;

		Inventory target = this.getGridForRecipe(recipeId);
		if (target == null) return false;

		this.changingIngredients = true;
		try {
			this.returnInventoryToPlayer(player, target);
			for (int i = 0; i < ingredients.size(); i++) {
				Item item = ingredients.get(i);
				if (item == null) continue;
				ItemStack pulled = this.takeAmount(player, item, crafts);
				if (pulled.isEmpty() || pulled.getCount() != crafts) return false;
				target.setStack(i, pulled);
			}
		} finally {
			this.changingIngredients = false;
			this.updateResult();
		}
		return true;
	}

	private int getMaximumAutofillCrafts(PlayerEntity player, int recipeId) {
		List<Item> ingredients = this.getAutofillIngredients(recipeId);
		if (ingredients == null) return 0;

		Map<Item, Integer> neededPerCraft = countItems(ingredients);
		if (neededPerCraft.isEmpty()) return 0;
		Map<Item, Integer> available = new HashMap<>();

		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty()) available.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		Inventory active = this.getGridForRecipe(recipeId);
		if (active != null) {
			for (int i = 0; i < active.size(); i++) {
				ItemStack stack = active.getStack(i);
				if (!stack.isEmpty()) available.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}

		int crafts = Integer.MAX_VALUE;
		for (Map.Entry<Item, Integer> requirement : neededPerCraft.entrySet()) {
			crafts = Math.min(crafts, available.getOrDefault(requirement.getKey(), 0) / requirement.getValue());
		}
		for (Item item : ingredients) {
			if (item != null) crafts = Math.min(crafts, new ItemStack(item).getMaxCount());
		}
		return crafts == Integer.MAX_VALUE ? 0 : Math.max(0, crafts);
	}

	private Inventory getGridForRecipe(int recipeId) {
		Page recipePage = pageForRecipe(recipeId);
		if (recipePage == Page.BODY) return this.bodyGrid;
		if (recipePage == Page.PARTS) return this.partsGrid;
		return null;
	}

	private Inventory getInventoryForRecipe(int recipeId) {
		Page recipePage = pageForRecipe(recipeId);
		if (recipePage == Page.BODY) return this.bodyGrid;
		if (recipePage == Page.PARTS) return this.partsGrid;
		if (recipePage == Page.ASSEMBLY) return this.assemblyGrid;
		return null;
	}

	private static Page pageForRecipe(int recipeId) {
		if (recipeId == BODY_HATCHBACK) return Page.BODY;
		if (recipeId >= PART_SUMMER_TIRE && recipeId <= PART_AUTOMATIC_TRANSMISSION) return Page.PARTS;
		if (recipeId == ASSEMBLY_HATCHBACK) return Page.ASSEMBLY;
		return null;
	}

	private List<Item> getAutofillIngredients(int recipeId) {
		return getAutofillIngredientsStatic(recipeId);
	}

	private static List<Item> getAutofillIngredientsStatic(int recipeId) {
		if (recipeId == BODY_HATCHBACK) {
			return List.of(
					Items.GLASS_PANE, Items.IRON_INGOT, Items.IRON_INGOT, Items.GLASS_PANE,
					Items.IRON_INGOT, Items.FURNACE, Items.CHEST, Items.IRON_INGOT,
					Items.IRON_BARS, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_BARS
			);
		}

		if (recipeId == PART_SUMMER_TIRE) {
			return List.of(
					Items.BLACK_WOOL, Items.BLACK_WOOL, Items.BLACK_WOOL,
					Items.BLACK_WOOL, Items.IRON_INGOT, Items.BLACK_WOOL,
					Items.BLACK_WOOL, Items.BLACK_WOOL, Items.BLACK_WOOL
			);
		}
		if (recipeId == PART_ALL_SEASON_TIRE) {
			return List.of(
					Items.BLACK_WOOL, Items.LEATHER, Items.BLACK_WOOL,
					Items.LEATHER, Items.IRON_INGOT, Items.LEATHER,
					Items.BLACK_WOOL, Items.LEATHER, Items.BLACK_WOOL
			);
		}
		if (recipeId == PART_WINTER_TIRE) {
			return List.of(
					Items.BLACK_WOOL, Items.IRON_NUGGET, Items.BLACK_WOOL,
					Items.IRON_NUGGET, Items.SNOWBALL, Items.IRON_NUGGET,
					Items.BLACK_WOOL, Items.IRON_NUGGET, Items.BLACK_WOOL
			);
		}
		if (recipeId == PART_SUMMER_SET) {
			return listWithSlots(ModItems.SUMMER_TIRES, ModItems.SUMMER_TIRES, null,
					ModItems.SUMMER_TIRES, ModItems.SUMMER_TIRES, null, null, null, null);
		}
		if (recipeId == PART_ALL_SEASON_SET) {
			return listWithSlots(ModItems.ALL_SEASON_TIRES, ModItems.ALL_SEASON_TIRES, null,
					ModItems.ALL_SEASON_TIRES, ModItems.ALL_SEASON_TIRES, null, null, null, null);
		}
		if (recipeId == PART_WINTER_SET) {
			return listWithSlots(ModItems.WINTER_TIRES, ModItems.WINTER_TIRES, null,
					ModItems.WINTER_TIRES, ModItems.WINTER_TIRES, null, null, null, null);
		}
		if (recipeId == PART_UNPACK_SUMMER_SET) {
			return listWithSlots(null, null, null, null, ModItems.SUMMER_TIRE_SET, null, null, null, null);
		}
		if (recipeId == PART_UNPACK_ALL_SEASON_SET) {
			return listWithSlots(null, null, null, null, ModItems.ALL_SEASON_TIRE_SET, null, null, null, null);
		}
		if (recipeId == PART_UNPACK_WINTER_SET) {
			return listWithSlots(null, null, null, null, ModItems.WINTER_TIRE_SET, null, null, null, null);
		}
		if (recipeId == PART_MANUAL_TRANSMISSION) {
			return listWithSlots(
					null, Items.IRON_INGOT, null,
					Items.GOLD_INGOT, Items.FURNACE, Items.GOLD_INGOT,
					null, Items.IRON_INGOT, null
			);
		}
		if (recipeId == PART_AUTOMATIC_TRANSMISSION) {
			return listWithSlots(
					null, Items.REDSTONE, null,
					Items.GOLD_INGOT, Items.FURNACE, Items.GOLD_INGOT,
					null, Items.DIAMOND, null
			);
		}

		// Assembly recipe book is informational because transmission and tire type
		// are deliberate player choices rather than one fixed ingredient pattern.
		return null;
	}

	public static List<RecipeRequirement> getRecipeRequirements(int recipeId) {
		if (recipeId == ASSEMBLY_HATCHBACK) {
			return List.of(
					new RecipeRequirement(new ItemStack(ModItems.HATCHBACK_BODY), List.of(ModItems.HATCHBACK_BODY), 1, Text.translatable("item.ironmile.hatchback_body")),
					new RecipeRequirement(new ItemStack(ModItems.MANUAL_TRANSMISSION), List.of(ModItems.MANUAL_TRANSMISSION, ModItems.AUTOMATIC_TRANSMISSION), 1, Text.translatable("gui.ironmile.workbench.any_transmission")),
					new RecipeRequirement(new ItemStack(ModItems.ALL_SEASON_TIRE_SET), List.of(ModItems.SUMMER_TIRE_SET, ModItems.ALL_SEASON_TIRE_SET, ModItems.WINTER_TIRE_SET), 1, Text.translatable("gui.ironmile.workbench.any_tire_set"))
			);
		}

		List<Item> ingredients = getAutofillIngredientsStatic(recipeId);
		if (ingredients == null) return List.of();
		Map<Item, Integer> counts = new LinkedHashMap<>();
		for (Item item : ingredients) {
			if (item != null) counts.merge(item, 1, Integer::sum);
		}
		List<RecipeRequirement> requirements = new ArrayList<>();
		for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
			ItemStack stack = new ItemStack(entry.getKey());
			requirements.add(new RecipeRequirement(stack, List.of(entry.getKey()), entry.getValue(), stack.getName()));
		}
		return requirements;
	}

	@SafeVarargs
	private static <T> List<T> listWithSlots(T... values) {
		List<T> list = new ArrayList<>(values.length);
		for (T value : values) list.add(value);
		return list;
	}

	private static Map<Item, Integer> countItems(List<Item> items) {
		Map<Item, Integer> counts = new HashMap<>();
		for (Item item : items) {
			if (item != null) counts.merge(item, 1, Integer::sum);
		}
		return counts;
	}

	private void returnInventoryToPlayer(PlayerEntity player, Inventory inventory) {
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.removeStack(i);
			if (stack.isEmpty()) continue;
			if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) {
				player.dropItem(stack, false);
			}
		}
	}

	private ItemStack takeAmount(PlayerEntity player, Item item, int amount) {
		if (amount <= 0) return ItemStack.EMPTY;
		ItemStack pulled = new ItemStack(item, amount);
		int remaining = amount;
		for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isOf(item)) continue;
			int take = Math.min(remaining, stack.getCount());
			stack.decrement(take);
			remaining -= take;
		}
		player.getInventory().markDirty();
		return remaining == 0 ? pulled : ItemStack.EMPTY;
	}

	private void updateResult() {
		if (this.changingIngredients) return;

		ItemStack result = ItemStack.EMPTY;
		this.currentRecipeId = -1;

		if (this.page == Page.BODY && this.matchesBodyRecipe()) {
			this.currentRecipeId = BODY_HATCHBACK;
			result = new ItemStack(ModItems.HATCHBACK_BODY);
		} else if (this.page == Page.PARTS) {
			this.currentRecipeId = this.matchPartsRecipe();
			result = this.getPartsResult(this.currentRecipeId);
		} else if (this.page == Page.ASSEMBLY && this.matchesAssembly()) {
			this.currentRecipeId = ASSEMBLY_HATCHBACK;
			result = this.createAssembledCar();
		}

		this.resultInventory.setStack(0, result);
		this.sendContentUpdates();
	}

	private boolean matchesBodyRecipe() {
		Item[] pattern = new Item[] {
				Items.GLASS_PANE, Items.IRON_INGOT, Items.IRON_INGOT, Items.GLASS_PANE,
				Items.IRON_INGOT, Items.FURNACE, Items.CHEST, Items.IRON_INGOT,
				Items.IRON_BARS, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_BARS
		};
		return matchesPattern(this.bodyGrid, pattern);
	}

	private int matchPartsRecipe() {
		if (matchesPattern(this.partsGrid, new Item[] {
				Items.BLACK_WOOL, Items.BLACK_WOOL, Items.BLACK_WOOL,
				Items.BLACK_WOOL, Items.IRON_INGOT, Items.BLACK_WOOL,
				Items.BLACK_WOOL, Items.BLACK_WOOL, Items.BLACK_WOOL
		})) return PART_SUMMER_TIRE;

		if (matchesPattern(this.partsGrid, new Item[] {
				Items.BLACK_WOOL, Items.LEATHER, Items.BLACK_WOOL,
				Items.LEATHER, Items.IRON_INGOT, Items.LEATHER,
				Items.BLACK_WOOL, Items.LEATHER, Items.BLACK_WOOL
		})) return PART_ALL_SEASON_TIRE;

		if (matchesPattern(this.partsGrid, new Item[] {
				Items.BLACK_WOOL, Items.IRON_NUGGET, Items.BLACK_WOOL,
				Items.IRON_NUGGET, Items.SNOWBALL, Items.IRON_NUGGET,
				Items.BLACK_WOOL, Items.IRON_NUGGET, Items.BLACK_WOOL
		})) return PART_WINTER_TIRE;

		if (matchesPattern(this.partsGrid, new Item[] {
				null, Items.IRON_INGOT, null,
				Items.GOLD_INGOT, Items.FURNACE, Items.GOLD_INGOT,
				null, Items.IRON_INGOT, null
		})) return PART_MANUAL_TRANSMISSION;

		if (matchesPattern(this.partsGrid, new Item[] {
				null, Items.REDSTONE, null,
				Items.GOLD_INGOT, Items.FURNACE, Items.GOLD_INGOT,
				null, Items.DIAMOND, null
		})) return PART_AUTOMATIC_TRANSMISSION;

		if (matchesOnly(this.partsGrid, ModItems.SUMMER_TIRES, 4)) return PART_SUMMER_SET;
		if (matchesOnly(this.partsGrid, ModItems.ALL_SEASON_TIRES, 4)) return PART_ALL_SEASON_SET;
		if (matchesOnly(this.partsGrid, ModItems.WINTER_TIRES, 4)) return PART_WINTER_SET;
		if (matchesOnly(this.partsGrid, ModItems.SUMMER_TIRE_SET, 1)) return PART_UNPACK_SUMMER_SET;
		if (matchesOnly(this.partsGrid, ModItems.ALL_SEASON_TIRE_SET, 1)) return PART_UNPACK_ALL_SEASON_SET;
		if (matchesOnly(this.partsGrid, ModItems.WINTER_TIRE_SET, 1)) return PART_UNPACK_WINTER_SET;

		return -1;
	}

	private ItemStack getPartsResult(int recipeId) {
		return switch (recipeId) {
			case PART_SUMMER_TIRE -> new ItemStack(ModItems.SUMMER_TIRES);
			case PART_ALL_SEASON_TIRE -> new ItemStack(ModItems.ALL_SEASON_TIRES);
			case PART_WINTER_TIRE -> new ItemStack(ModItems.WINTER_TIRES);
			case PART_SUMMER_SET -> new ItemStack(ModItems.SUMMER_TIRE_SET);
			case PART_ALL_SEASON_SET -> new ItemStack(ModItems.ALL_SEASON_TIRE_SET);
			case PART_WINTER_SET -> new ItemStack(ModItems.WINTER_TIRE_SET);
			case PART_UNPACK_SUMMER_SET -> new ItemStack(ModItems.SUMMER_TIRES, 4);
			case PART_UNPACK_ALL_SEASON_SET -> new ItemStack(ModItems.ALL_SEASON_TIRES, 4);
			case PART_UNPACK_WINTER_SET -> new ItemStack(ModItems.WINTER_TIRES, 4);
			case PART_MANUAL_TRANSMISSION -> new ItemStack(ModItems.MANUAL_TRANSMISSION);
			case PART_AUTOMATIC_TRANSMISSION -> new ItemStack(ModItems.AUTOMATIC_TRANSMISSION);
			default -> ItemStack.EMPTY;
		};
	}

	private boolean matchesAssembly() {
		return this.assemblyGrid.getStack(0).isOf(ModItems.HATCHBACK_BODY)
				&& this.assemblyGrid.getStack(1).getItem() instanceof TransmissionItem
				&& this.assemblyGrid.getStack(2).getItem() instanceof TireSetItem;
	}

	private ItemStack createAssembledCar() {
		TransmissionItem transmission = (TransmissionItem) this.assemblyGrid.getStack(1).getItem();
		TireSetItem tires = (TireSetItem) this.assemblyGrid.getStack(2).getItem();
		ItemStack car = new ItemStack(transmission.isManual() ? ModItems.CAR_MANUAL : ModItems.CAR);
		CarItem.setInstalledTireType(car, tires.getTireType());
		return car;
	}

	private static boolean matchesPattern(Inventory inventory, Item[] pattern) {
		if (inventory.size() != pattern.length) return false;
		for (int i = 0; i < pattern.length; i++) {
			Item expected = pattern[i];
			ItemStack actual = inventory.getStack(i);
			if (expected == null) {
				if (!actual.isEmpty()) return false;
			} else if (!actual.isOf(expected)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesOnly(Inventory inventory, Item item, int count) {
		int found = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) continue;
			if (!stack.isOf(item)) return false;
			found += stack.getCount();
		}
		return found >= count;
	}

	private void craftCurrentRecipe(PlayerEntity player) {
		if (player.getWorld().isClient || this.currentRecipeId < 0) {
			return;
		}

		this.changingIngredients = true;
		try {
			if (this.currentRecipeId == BODY_HATCHBACK) {
				consumeOneFromEveryOccupiedSlot(this.bodyGrid);
			} else if (this.currentRecipeId >= PART_SUMMER_TIRE
					&& this.currentRecipeId <= PART_AUTOMATIC_TRANSMISSION) {
				this.consumePartsRecipe(this.currentRecipeId);
			} else if (this.currentRecipeId == ASSEMBLY_HATCHBACK) {
				for (int i = 0; i < this.assemblyGrid.size(); i++) {
					this.assemblyGrid.getStack(i).decrement(1);
				}
			}
		} finally {
			this.changingIngredients = false;
			this.updateResult();
		}
	}

	private void consumePartsRecipe(int recipeId) {
		if (recipeId == PART_SUMMER_SET
				|| recipeId == PART_ALL_SEASON_SET
				|| recipeId == PART_WINTER_SET) {
			consumeAmountAcrossGrid(this.partsGrid, 4);
			return;
		}
		if (recipeId == PART_UNPACK_SUMMER_SET
				|| recipeId == PART_UNPACK_ALL_SEASON_SET
				|| recipeId == PART_UNPACK_WINTER_SET) {
			consumeAmountAcrossGrid(this.partsGrid, 1);
			return;
		}
		consumeOneFromEveryOccupiedSlot(this.partsGrid);
	}

	private static void consumeOneFromEveryOccupiedSlot(Inventory inventory) {
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) stack.decrement(1);
		}
	}

	private static void consumeAmountAcrossGrid(Inventory inventory, int amount) {
		int remaining = amount;
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) continue;
			int take = Math.min(remaining, stack.getCount());
			stack.decrement(take);
			remaining -= take;
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) return ItemStack.EMPTY;

		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();

		if (slotIndex == OUTPUT_SLOT) {
			if (!this.insertItem(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
			slot.onQuickTransfer(stack, original);
		} else if (slotIndex >= PLAYER_START) {
			if (this.page == Page.ASSEMBLY && this.tryInsertAssemblyItem(stack)) {
				// inserted into one of the three component slots
			} else if (slotIndex < HOTBAR_START) {
				if (!this.insertItem(stack, HOTBAR_START, PLAYER_END, false)) return ItemStack.EMPTY;
			} else if (!this.insertItem(stack, PLAYER_START, PLAYER_MAIN_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, PLAYER_START, PLAYER_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
		else slot.markDirty();

		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTakeItem(player, stack);
		return original;
	}

	private boolean tryInsertAssemblyItem(ItemStack stack) {
		if (stack.isOf(ModItems.HATCHBACK_BODY)) {
			return this.insertItem(stack, ASSEMBLY_START, ASSEMBLY_START + 1, false);
		}
		if (stack.getItem() instanceof TransmissionItem) {
			return this.insertItem(stack, ASSEMBLY_START + 1, ASSEMBLY_START + 2, false);
		}
		if (stack.getItem() instanceof TireSetItem) {
			return this.insertItem(stack, ASSEMBLY_START + 2, ASSEMBLY_END, false);
		}
		return false;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, ModBlocks.MECHANICS_WORKBENCH);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			this.dropInventory(player, this.bodyGrid);
			this.dropInventory(player, this.partsGrid);
			this.dropInventory(player, this.assemblyGrid);
		}
	}

	private final class PageSlot extends Slot {
		private final Page slotPage;

		private PageSlot(Inventory inventory, int index, int x, int y, Page slotPage) {
			super(inventory, index, x, y);
			this.slotPage = slotPage;
		}

		@Override
		public boolean isEnabled() {
			return MechanicsWorkbenchScreenHandler.this.page == this.slotPage;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return this.isEnabled();
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return this.isEnabled();
		}
	}

	private enum AssemblyKind {
		BODY,
		TRANSMISSION,
		TIRES
	}

	private final class AssemblySlot extends Slot {
		private final AssemblyKind kind;

		private AssemblySlot(Inventory inventory, int index, int x, int y, AssemblyKind kind) {
			super(inventory, index, x, y);
			this.kind = kind;
		}

		@Override
		public boolean isEnabled() {
			return MechanicsWorkbenchScreenHandler.this.page == Page.ASSEMBLY;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			if (!this.isEnabled()) return false;
			return switch (this.kind) {
				case BODY -> stack.isOf(ModItems.HATCHBACK_BODY);
				case TRANSMISSION -> stack.getItem() instanceof TransmissionItem;
				case TIRES -> stack.getItem() instanceof TireSetItem;
			};
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return this.isEnabled();
		}
	}
}
