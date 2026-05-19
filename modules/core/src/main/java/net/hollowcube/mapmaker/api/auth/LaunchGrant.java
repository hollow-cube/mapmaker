package net.hollowcube.mapmaker.api.auth;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record LaunchGrant(String url, String code, Instant expiresAt) {
}
