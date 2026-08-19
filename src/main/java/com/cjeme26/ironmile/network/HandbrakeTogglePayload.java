package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record HandbrakeTogglePayload(int entityId) implements CustomPayload {
	public static final Id<HandbrakeTogglePayload> ID = new Id<>(IronMile.id("toggle_handbrake"));
	public static final PacketCodec<RegistryByteBuf, HandbrakeTogglePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			HandbrakeTogglePayload::entityId,
			HandbrakeTogglePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
