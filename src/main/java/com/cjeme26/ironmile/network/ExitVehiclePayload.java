package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record ExitVehiclePayload(int entityId) implements CustomPayload {
	public static final Id<ExitVehiclePayload> ID = new Id<>(IronMile.id("exit_vehicle"));
	public static final PacketCodec<RegistryByteBuf, ExitVehiclePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER,
			ExitVehiclePayload::entityId,
			ExitVehiclePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
