package com.cjeme26.ironmile.sound;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final Identifier ENGINE_LOOP_ID = IronMile.id("engine_loop");
	public static final SoundEvent ENGINE_LOOP = SoundEvent.of(ENGINE_LOOP_ID, 48.0F);

	private ModSounds() {
	}

	public static void initialize() {
		Registry.register(Registries.SOUND_EVENT, ENGINE_LOOP_ID, ENGINE_LOOP);
	}
}
