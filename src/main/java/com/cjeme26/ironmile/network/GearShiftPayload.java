package com.cjeme26.ironmile.network;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record GearShiftPayload(int entityId, int direction) implements CustomPayload {
 public static final Id<GearShiftPayload> ID = new Id<>(IronMile.id("gear_shift"));
 public static final PacketCodec<RegistryByteBuf, GearShiftPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, GearShiftPayload::entityId, PacketCodecs.INTEGER, GearShiftPayload::direction, GearShiftPayload::new);
 @Override public Id<? extends CustomPayload> getId() { return ID; }
}
