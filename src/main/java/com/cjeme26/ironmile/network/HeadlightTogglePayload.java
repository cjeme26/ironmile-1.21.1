package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record HeadlightTogglePayload(int entityId) implements CustomPayload {
	public static final Id<HeadlightTogglePayload> ID = new Id<>(IronMile.id("toggle_headlights"));
	public static final PacketCodec<RegistryByteBuf, HeadlightTogglePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			HeadlightTogglePayload::entityId,
			HeadlightTogglePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
