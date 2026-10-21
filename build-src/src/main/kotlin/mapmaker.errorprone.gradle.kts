import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    alias(libs.plugins.errorprone)
}

dependencies {
    errorprone(libs.errorprone.core)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Werror")
    // exports and missing-explicit-ctor produce mostly boilerplate,
    // automatic module requires cannot be suppressed in source (fastutil and friends lack module-info)
    options.compilerArgs.add("-Xlint:all,-requires-automatic,-requires-transitive-automatic,-exports,-missing-explicit-ctor")
    options.errorprone {
        excludedPaths = ".*/build/generated/.*"
        option("UnnamedVariable:OnlyRenameVariablesNamedUnused", "false") // otherwise only checks "unnamed"

        error(
            "AlmostJavadoc",
            "BoxedPrimitiveConstructor",
            "CompareToZero",
            "ConstantField",
            "DeprecatedVariable",
            "EqualsIncompatibleType",
            "EqualsUnsafeCast",
            "EqualsUsingHashCode",
            "ErroneousBitwiseExpression",
            "FieldCanBeLocal",
            "FieldCanBeStatic",
            "FieldMissingNullable",
            "Finalize",
            "Finally",
            "FloatCast",
            "FutureTransformAsync",
            "IfChainToSwitch",
            "InconsistentHashCode",
            "InlineTrivialConstant",
            "IntFloatConversion",
            "IntLiteralCast",
            "IntLongMath",
            "InvalidBlockTag",
            "InvalidInlineTag",
            "InvalidLink",
            "InvalidParam",
            "InvalidSnippet",
            "InvalidThrows",
            "InvalidThrowsLink",
            "LambdaFunctionalInterface",
            "LockNotBeforeTry",
            "LongFloatConversion",
            "LoopOverCharArray",
            "PrimitiveArrayPassedToVarargsMethod",
            "AddressSelection",
            "ArgumentSelectionDefectChecker",
            "AssignmentExpression",
            "AttemptedNegativeZero",
            "BadImport",
            "BadInstanceof",
            "BooleanLiteral",
            "BoxingComparator",
            "ByteBufferBackingArray",
            "ClassInitializationDeadlock",
            "DefaultCharset",
            "EffectivelyPrivate",
            "EqualsGetClass",
            "EmptyBlockTag",
            "EmptyCatch",
            "FutureReturnValueIgnored",
            "InlineFormatString",
            "JdkObsolete",
            "LongDoubleConversion",
            "MethodCanBeStatic",
            "MissingCasesInEnumSwitch",
            "MissingSummary",
            "MutablePublicArray",
            "NarrowingCompoundAssignment",
            "NonApiType",
            "NonOverridingEquals",
            "NonCanonicalType",
            "StatementSwitchToExpressionSwitch",
            "StringCaseLocaleUsage",
            "StringSplitter",
            "TypeParameterUnusedInFormals",
            "MissingOverride",
            "ObjectToString",
            "OperatorPrecedence",
            "ParameterMissingNullable",
            "ShortCircuitBoolean",
            "PatternMatchingInstanceof",
            "RedundantControlFlow",
            "SameNameButDifferent",
            "UnnamedVariable",
            "UnusedTypeParameter",
            "VarWithPrimitive",
            "VariableNameSameAsType",
            "UnusedMethod",
            "UnusedVariable",
            "UnnecessaryLambda",
            "UnnecessaryParentheses",
            "UnnecessarilyFullyQualified",
            "UnnecessaryMethodReference",
            "NullArgumentForNonNullParameter",
            "NullableConstructor",
            "NullableOnContainingClass",
            "NullableOptional",
            "NullablePrimitive",
            "NullablePrimitiveArray",
            "NullableTypeParameter",
            "NullableVoid",
            "NullableWildcard",
            "RemoveUnusedImports",
            "UnnecessaryStaticImport",
        )

        disable(
            "AvoidCommonTypeNames", // ill name what i want
            "FallThrough", // duplicates javac fallthrough lint
            "InlineMeSuggester", // not using the slop annotation
            "ReferenceEquality", // intentionally used for perf
            "ArrayRecordComponent", // intentionally used for perf
        )
    }
}
