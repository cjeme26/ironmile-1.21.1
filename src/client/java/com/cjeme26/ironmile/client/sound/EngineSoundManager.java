package com.cjeme26.ironmile.client.sound;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EngineSoundManager {
	private static final double AUDIBLE_DISTANCE_SQUARED = 48.0 * 48.0;
	private static final Map<Integer, CarEngineSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

	private EngineSoundManager() {
	}

	public static void tick(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			ACTIVE_SOUNDS.values().forEach(CarEngineSoundInstance::requestStop);
			ACTIVE_SOUNDS.clear();
			return;
		}

		Set<Integer> audibleCars = new HashSet<>();
		for (Entity entity : client.world.getEntities()) {
			if (!(entity instanceof CarEntity car)
					|| !car.isEngineRunning()
					|| car.squaredDistanceTo(client.player) > AUDIBLE_DISTANCE_SQUARED) {
				continue;
			}

			audibleCars.add(car.getId());
			ACTIVE_SOUNDS.computeIfAbsent(car.getId(), ignored -> {
				boolean listenerRiding = client.player.getVehicle() == car;
				CarEngineSoundInstance sound = new CarEngineSoundInstance(car, listenerRiding);
				client.getSoundManager().play(sound);
				return sound;
			});
		}

		ACTIVE_SOUNDS.entrySet().removeIf(entry -> {
			CarEngineSoundInstance sound = entry.getValue();
			if (!audibleCars.contains(entry.getKey())) {
				sound.requestStop();
				return true;
			}
			return sound.isDone();
		});
	}
}
