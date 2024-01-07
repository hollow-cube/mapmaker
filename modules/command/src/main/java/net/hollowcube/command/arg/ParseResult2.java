package net.hollowcube.command.arg;

import org.jetbrains.annotations.UnknownNullability;

public sealed interface ParseResult2<T> permits ParseResult2.Partial, ParseResult2.Success, ParseResult2.Failure {

    record Partial<T>() implements ParseResult2<T> {

    }

    record Success<T>(@UnknownNullability T value) implements ParseResult2<T> {

    }

    record Failure<T>(int start) implements ParseResult2<T> {

    }

}
