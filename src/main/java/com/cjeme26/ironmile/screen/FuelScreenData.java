package com.cjeme26.ironmile.screen;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record FuelScreenData(int entityId) {
	public static final PacketCodec<RegistryByteBuf, FuelScreenData> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			FuelScreenData::entityId,
			FuelScreenData::new
	);
}
