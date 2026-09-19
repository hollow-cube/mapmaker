package net.hollowcube.apiserver.job;

import org.jetbrains.annotations.Nullable;

/// @param reason logged only
public record TranscodeReplay(String replayId, @Nullable String reason) {}
