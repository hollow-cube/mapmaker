package net.hollowcube.terraform.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;

public record TypedData(String type, CompoundBinaryTag nbt) {}
