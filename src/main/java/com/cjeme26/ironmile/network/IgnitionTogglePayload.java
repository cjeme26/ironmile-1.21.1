package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record IgnitionTogglePayload(int entityId) implements CustomPayload {
	public static final Id<IgnitionTogglePayload> ID = new Id<>(IronMile.id("toggle_ignition"));
	public static final PacketCodec<RegistryByteBuf, IgnitionTogglePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			IgnitionTogglePayload::entityId,
			IgnitionTogglePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
