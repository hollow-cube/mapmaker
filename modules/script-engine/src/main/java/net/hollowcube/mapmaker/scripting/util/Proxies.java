package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.common.util.FutureUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Proxies {

    public static @NotNull ProxyObject proxyObject(@NotNull Map<String, Object> map) {
        return ProxyObject.fromMap(map);
    }

    public static @NotNull ProxyObject freezeObject(@NotNull ProxyObject object) {
        // We delegate to the builtin proxy object to preserve the same behavior with host object unboxing.
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                return object.getMember(key);
            }

            @Override
            public Object getMemberKeys() {
                return object.getMemberKeys();
            }

            @Override
            public boolean hasMember(String key) {
                return object.hasMember(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("putMember() not supported.");
            }
        };
    }

    public static @NotNull CompletableFuture<Void> wrapPromiseLike(@Nullable Value promiseLike) {
        if (promiseLike == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (promiseLike.isHostObject()) {
            // Can be either a FutureThenable or a ResolvedThenable
            return switch (promiseLike.asHostObject()) {
                case FutureThenable<?> futureThenable -> futureThenable.future.thenRun(() -> {
                    /* Intentionally empty to convert to void */
                });
                case null, default -> CompletableFuture.completedFuture(null);
            };
        }
        if (!promiseLike.canInvokeMember("then")) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        promiseLike.invokeMember("then",
                (ProxyExecutable) (args) -> {
                    future.complete(null);
                    return null;
                },
                (ProxyExecutable) (args) -> {
                    var error = args[0];
                    future.completeExceptionally(new RuntimeException(error.toString()));
                    return null;
                });

        return future;
    }

    public static <T> @NotNull Value resolved(T value) {
        return Value.asValue(new ResolvedThenable<>(value));
    }

    public static <T> @NotNull Value async(@NotNull Callable<T> task, @NotNull Executor jsExecutor) {
        return Value.asValue(new FutureThenable<>(FutureUtil.fork(task), jsExecutor));
    }

    public record FutureThenable<T>(@NotNull CompletableFuture<T> future, Executor jsExecutor) {
        @HostAccess.Export
        public void then(@NotNull Value onResolve, @NotNull Value onReject) {
            future
                    .thenAcceptAsync(onResolve::executeVoid, jsExecutor)
                    .exceptionallyAsync((throwable) -> {
                        onReject.executeVoid(makeError(throwable));
                        return null;
                    }, jsExecutor);
        }
    }

    public record ResolvedThenable<T>(@NotNull T value) {
        @HostAccess.Export
        public void then(@NotNull Value onResolve, @NotNull Value ignored) {
            onResolve.execute(value);
        }
    }

    public static @NotNull Value makeError(@NotNull Throwable e) {
        var sb = new ByteArrayOutputStream();
        var pw = new PrintWriter(sb);
        e.printStackTrace(pw);
        pw.flush();
        String stackTrace = new String(sb.toByteArray());
        Value jsBindings = Context.getCurrent().getBindings("js");
        Value errorConstructor = jsBindings.getMember("Error");
        return errorConstructor.newInstance(e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + stackTrace);
    }

    public static @NotNull RuntimeException wrapException(@NotNull Throwable e) {
        var sb = new ByteArrayOutputStream();
        var pw = new PrintWriter(sb);
        e.printStackTrace(pw);
        pw.flush();
        String stackTrace = new String(sb.toByteArray());
        return wrapException(e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + stackTrace);
    }

    public static @NotNull RuntimeException wrapException(@NotNull String message) {
        Value jsBindings = Context.getCurrent().getBindings("js");
        Value errorConstructor = jsBindings.getMember("Error");
        Value errorObject = errorConstructor.newInstance(message);
        return errorObject.throwException();
    }
}
