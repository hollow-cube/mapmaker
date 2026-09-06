package net.hollowcube.ipc.util;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record Position(double x, double y, double z, float yaw, float pitch) {}
