package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Argument<T extends @UnknownNullability Object> {

    public static ArgumentLiteral Literal(String literal) {
        return new ArgumentLiteral(literal);
    }

    public static ArgumentWord Word(String id) {
        return new ArgumentWord(id);
    }

    public static ArgumentGreedyString GreedyString(String id) {
        return new ArgumentGreedyString(id);
    }

    public static ArgumentBool Bool(String id) {
        return new ArgumentBool(id);
    }

    public static ArgumentInt Int(String id) {
        return new ArgumentInt(id);
    }

    public static ArgumentFloat Float(String id) {
        return new ArgumentFloat(id);
    }

    public static ArgumentDouble Double(String id) {
        return new ArgumentDouble(id);
    }

    public static <E extends Enum<?>> ArgumentEnum<E> Enum(String id, Class<E> enumType) {
        return new ArgumentEnum<>(id, enumType);
    }

    public static ArgumentAxis Axis(String id) {
        return new ArgumentAxis(id);
    }

    public static ArgumentEntity Entity(String id) {
        return new ArgumentEntity(id);
    }

    public static ArgumentRelativeVec3 RelativeVec3(String id) {
        return new ArgumentRelativeVec3(id);
    }

    public static ArgumentMaterial Material(String id) {
        return new ArgumentMaterial(id);
    }

    public static ArgumentBlock Block(String id) {
        return new ArgumentBlock(id);
    }

    // Impl

    private final String id;

    private @Nullable String description = null;

    private @Nullable Function<CommandSender, T> defaultProvider = null;

    protected Argument(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    // Properties

    public boolean isOptional() {
        return defaultProvider != null;
    }

    public Argument<T> defaultValue(@Nullable T value) {
        return defaultValue(_ -> value);
    }

    public Argument<T> defaultValue(Function<CommandSender, T> provider) {
        this.defaultProvider = provider;
        return this;
    }

    public @Nullable T getDefaultValue(CommandSender sender) {
        return defaultProvider == null ? null : defaultProvider.apply(sender);
    }

    public @Nullable String description() {
        return description;
    }

    public Argument<T> description(String description) {
        this.description = description;
        return this;
    }

    public ArgumentParserType argumentType() {
        // score holder just reads every character until a whitespace,
        // only drawback is that we can't have any strings that start with @
        return ArgumentParserType.SCORE_HOLDER;
    }

    public void properties(NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.BYTE, (byte) 1);
    }

    public DeclareCommandsPacket.NodeType getType() {
        return DeclareCommandsPacket.NodeType.ARGUMENT;
    }

    public boolean shouldSuggest() {
        return true;
    }

    // Transforms

    public <R> Argument<R> map(ArgumentMap.ParseFunc<T, R> mapFunc) {
        return new ArgumentMap<>(id, this, mapFunc, null);
    }

    public <R> Argument<R> map(ArgumentMap.ParseFunc<T, R> mapFunc, ArgumentMap.SuggestFunc suggestFunc) {
        return new ArgumentMap<>(id, this, mapFunc, suggestFunc);
    }

    // Logic

    public abstract ParseResult<T> parse(CommandSender sender, StringReader reader);

    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        // No suggestions returned by default
    }


    // Result factories

    public ParseResult<T> partial() {
        return new ParseResult.Partial<>();
    }

    public ParseResult<T> partial(@Nullable String message) {
        return new ParseResult.Partial<>(message);
    }

    public ParseResult<T> partial(@Nullable String message, T value) {
        return new ParseResult.Partial<>(message, () -> value);
    }

    public ParseResult<T> partialWithValue(T value) {
        return new ParseResult.Partial<>(null, () -> value);
    }

    public ParseResult<T> success(Supplier<@UnknownNullability T> valueFunc) {
        return new ParseResult.Success<>(valueFunc);
    }

    public ParseResult<T> success(@UnknownNullability T value) {
        return new ParseResult.Success<>(value);
    }

    public ParseResult<T> syntaxError() {
        return new ParseResult.Failure<>(-1);
    }

    public ParseResult<T> syntaxError(String message) {
        return new ParseResult.Failure<>(-1, message);
    }

    public ParseResult<T> syntaxError(int start) {
        return new ParseResult.Failure<>(start);
    }

    public ParseResult<T> syntaxError(int start, @Nullable String message) {
        return new ParseResult.Failure<>(start, message);
    }
}
