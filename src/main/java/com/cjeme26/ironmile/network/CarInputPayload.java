package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Sends the controlling player's current driving inputs to server simulation. */
public record CarInputPayload(
		int entityId,
		int inputMask
) implements CustomPayload {
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int FORWARD = 4;
	public static final int BACK = 8;
	public static final int CLUTCH = 16;

	public static final Id<CarInputPayload> ID = new Id<>(IronMile.id("car_input"));
	public static final PacketCodec<RegistryByteBuf, CarInputPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			CarInputPayload::entityId,
			PacketCodecs.INTEGER,
			CarInputPayload::inputMask,
			CarInputPayload::new
	);

	public boolean left() {
		return (this.inputMask & LEFT) != 0;
	}

	public boolean right() {
		return (this.inputMask & RIGHT) != 0;
	}

	public boolean forward() {
		return (this.inputMask & FORWARD) != 0;
	}

	public boolean back() {
		return (this.inputMask & BACK) != 0;
	}

	public boolean clutch() {
		return (this.inputMask & CLUTCH) != 0;
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
