package com.cjeme26.ironmile.screen;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FuelScreenHandler extends ScreenHandler {
	private static final int FUEL_SLOT = 0;
	private static final int PLAYER_INVENTORY_START = 1;
	private static final int PLAYER_INVENTORY_END = 28;
	private static final int HOTBAR_START = 28;
	private static final int HOTBAR_END = 37;

	private final CarEntity car;
	private final SimpleInventory fuelInput;
	private boolean consumingFuel;

	public FuelScreenHandler(int syncId, PlayerInventory playerInventory, FuelScreenData data) {
		this(syncId, playerInventory, resolveCar(playerInventory, data.entityId()));
	}

	public FuelScreenHandler(int syncId, PlayerInventory playerInventory, CarEntity car) {
		super(ModScreenHandlers.FUEL, syncId);
		this.car = car;
		this.fuelInput = new SimpleInventory(1);
		this.fuelInput.addListener(inventory -> this.consumeFuelInput());

		this.addSlot(new Slot(this.fuelInput, 0, 8, 83) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isPrototypeFuel(stack);
			}
		});

		// Player main inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(
						playerInventory,
						column + row * 9 + 9,
						8 + column * 18,
						123 + row * 18
				));
			}
		}

		// Hotbar.
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(
					playerInventory,
					column,
					8 + column * 18,
					181
			));
		}
	}

	private static CarEntity resolveCar(PlayerInventory playerInventory, int entityId) {
		Entity entity = playerInventory.player.getWorld().getEntityById(entityId);
		return entity instanceof CarEntity car ? car : null;
	}

	public static boolean isPrototypeFuel(ItemStack stack) {
		return stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL);
	}

	private void consumeFuelInput() {
		if (this.consumingFuel
				|| this.car == null
				|| this.car.getWorld().isClient) {
			return;
		}

		ItemStack stack = this.fuelInput.getStack(0);
		if (!isPrototypeFuel(stack) || stack.isEmpty()) {
			return;
		}

		int perItem = this.car.getPrototypeFuelItemMilliliters();
		int room = this.car.getFuelCapacityMilliliters() - this.car.getFuelMilliliters();
		int itemsThatFit = room / perItem;
		int consume = Math.min(stack.getCount(), itemsThatFit);

		if (consume <= 0) {
			return;
		}

		this.consumingFuel = true;
		try {
			this.car.addFuelMilliliters(consume * perItem);
			stack.decrement(consume);
			if (stack.isEmpty()) {
				this.fuelInput.setStack(0, ItemStack.EMPTY);
			} else {
				this.fuelInput.markDirty();
			}
		} finally {
			this.consumingFuel = false;
		}
	}

	public int getFuelMilliliters() {
		return this.car == null ? 0 : this.car.getFuelMilliliters();
	}

	public int getFuelCapacityMilliliters() {
		return this.car == null ? 45_000 : this.car.getFuelCapacityMilliliters();
	}

	public double getFuelFraction() {
		int capacity = this.getFuelCapacityMilliliters();
		return capacity <= 0 ? 0.0 : this.getFuelMilliliters() / (double) capacity;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.car == null
				|| (!this.car.isRemoved() && player.squaredDistanceTo(this.car) <= 64.0);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= this.slots.size()) {
			return ItemStack.EMPTY;
		}

		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();

		if (slotIndex == FUEL_SLOT) {
			if (!this.insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (isPrototypeFuel(stack)) {
			if (!this.insertItem(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
				if (slotIndex < HOTBAR_START) {
					if (!this.insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.insertItem(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		} else if (slotIndex < HOTBAR_START) {
			if (!this.insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}

		return original;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			this.dropInventory(player, this.fuelInput);
		}
	}
}
