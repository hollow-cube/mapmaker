package net.hollowcube.mapmaker.util;

import com.google.gson.annotations.SerializedName;
import net.hollowcube.common.lang.GenericMessages;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response is a class that represents a universal response from a service.
 */
public record Response<T>(
    @SerializedName("traceId")
    @Nullable String traceId,
    @UnknownNullability T payload,
    @Nullable Error error
) {

    private static final Logger logger = LoggerFactory.getLogger(Error.class);

    public static final int ERROR_UNKNOWN = 1;

    public boolean isError() {
        return error != null;
    }

    public boolean is(int errorCode) {
        return error != null && error.code == errorCode;
    }

    public @Nullable Integer errorCode() {
        return error != null ? error.code : null;
    }

    public void logError(@Nullable Audience target) {
        if (error == null) return;

        // Create exception so we get a stacktrace here
        logger.error("service error: {} ({})", error.message, error.detail, new RuntimeException(error.message));
        if (target != null) target.sendMessage(GenericMessages.COMMAND_UNKNOWN_ERROR.asError(traceId));
    }

    public record Error(
        int code,
        String message,
        @Nullable String detail
    ) {
    }

}
