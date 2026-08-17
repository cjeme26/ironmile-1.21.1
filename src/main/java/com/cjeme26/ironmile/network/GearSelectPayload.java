package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record GearSelectPayload(int entityId, int gear) implements CustomPayload {
    public static final Id<GearSelectPayload> ID = new Id<>(IronMile.id("gear_select"));
    public static final PacketCodec<RegistryByteBuf, GearSelectPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, GearSelectPayload::entityId,
            PacketCodecs.INTEGER, GearSelectPayload::gear,
            GearSelectPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
