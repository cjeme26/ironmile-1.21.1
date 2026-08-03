package com.cjeme26.ironmile.client.sound;

import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.sound.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

public final class CarEngineSoundInstance extends MovingSoundInstance {
	private static final float IDLE_RPM = 800.0F;
	private static final float REDLINE_RPM = 6500.0F;

	private final CarEntity car;
	private final boolean listenerRiding;
	private boolean stopRequested;

	public CarEngineSoundInstance(CarEntity car, boolean listenerRiding) {
		super(ModSounds.ENGINE_LOOP, SoundCategory.NEUTRAL, SoundInstance.createRandom());
		this.car = car;
		this.listenerRiding = listenerRiding;
		this.relative = listenerRiding;
		this.repeat = true;
		this.repeatDelay = 0;
		this.volume = 0.0F;
		this.pitch = 0.72F;
		this.updateSoundPosition();
	}

	@Override
	public void tick() {
		this.updateSoundPosition();

		boolean active = !this.stopRequested && !this.car.isRemoved() && this.car.hasPassengers();
		float targetVolume = 0.0F;
		float targetPitch = this.pitch;

		if (active) {
			float rpm = this.getAudibleRpm();
			float rpmAmount = MathHelper.clamp((rpm - IDLE_RPM) / (REDLINE_RPM - IDLE_RPM), 0.0F, 1.0F);
			// A narrower, slower pitch sweep avoids resampling chatter while still
			// making the full RPM range clearly audible.
			targetPitch = 0.72F + rpmAmount * 0.63F;
			targetVolume = 0.28F + rpmAmount * 0.28F;

			if (this.car.hasThrottleInput()) {
				targetVolume += 0.14F;
			}
			if (this.car.isReverseEngaged()) {
				targetPitch *= 0.92F;
			}
			if (this.car.isChangingGear()) {
				targetPitch *= 0.93F;
				targetVolume *= 0.82F;
			}
		}

		this.pitch += (targetPitch - this.pitch) * 0.08F;
		this.volume += (targetVolume - this.volume) * 0.15F;

		if (!active && this.volume < 0.01F) {
			this.setDone();
		}
	}

	private void updateSoundPosition() {
		if (this.listenerRiding) {
			// A listener-relative source prevents tiny camera/car position timing
			// differences from becoming Doppler-like buzzing at road speed.
			this.x = 0.0;
			this.y = 0.0;
			this.z = 0.0;
			return;
		}

		this.x = this.car.getX();
		this.y = this.car.getY();
		this.z = this.car.getZ();
	}

	private float getAudibleRpm() {
		float rpm = this.car.getEngineRpm();
		if (rpm <= 900.0F && this.car.getHorizontalSpeedKmh() > 5.0) {
			rpm = (float) (900.0 + Math.min(this.car.getHorizontalSpeedKmh() / 120.0, 1.0) * 5000.0);
		}
		return rpm;
	}

	public void requestStop() {
		this.stopRequested = true;
	}

	@Override
	public boolean shouldAlwaysPlay() {
		return true;
	}
}
