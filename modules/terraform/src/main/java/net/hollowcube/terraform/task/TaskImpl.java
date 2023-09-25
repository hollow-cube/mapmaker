package net.hollowcube.terraform.task;

import com.google.gson.JsonObject;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.ThreadLocalRandom;

@ApiStatus.Internal
public class TaskImpl implements Task {
    private final LocalSession session;

    private final String id = generateId();
    private final String tag;

    private State state = State.INIT;

    private ComputeFunc computeFunc;
    private BlockBuffer buffer; // Set after compute

    public TaskImpl(@NotNull LocalSession session, @NotNull String tag, @Nullable ComputeFunc computeFunc, @Nullable BlockBuffer buffer) {
        this.session = session;

        this.tag = tag;

        this.computeFunc = computeFunc;
        this.buffer = buffer;
    }

    @Override
    public @NotNull LocalSession session() {
        return session;
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull String tag() {
        return tag;
    }

    @Override
    public @NotNull State state() {
        return state;
    }

    public void setState(@NotNull State state) {
        this.state = state;
    }

    @Override
    public @NotNull JsonObject source() {
        return new JsonObject(); //todo
    }

    public @Nullable ComputeFunc computeFunc() {
        return computeFunc;
    }

    @Override
    public @UnknownNullability BlockBuffer buffer() {
        return buffer;
    }

    public void setBuffer(@NotNull BlockBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", tag, id);
    }

    private static final String idChars = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static @NotNull String generateId() {
        return ThreadLocalRandom.current().ints()
                .map(i -> Math.abs(i % idChars.length()))
                .mapToObj(idChars::charAt)
                .limit(5)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
