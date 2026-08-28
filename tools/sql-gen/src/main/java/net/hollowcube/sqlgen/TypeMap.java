package net.hollowcube.sqlgen;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/// Postgres types to Java types, and the JDBC calls that move values between them.
///
/// There is no fallback to `Object`: a type this does not know is a generation error naming the
/// query, on the theory that a wrong guess here becomes a `ClassCastException` in production.
final class TypeMap {

    static final ClassName JDBC = ClassName.get("net.hollowcube.sqlgen.runtime", "Jdbc");

    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName STRING = ClassName.get(String.class);

    private final Catalog catalog;
    private final String packageName;

    TypeMap(Catalog catalog, String packageName) {
        this.catalog = catalog;
        this.packageName = packageName;
    }

    ClassName enumClass(Catalog.PgEnum pgEnum) {
        return ClassName.get(packageName, Names.pascal(pgEnum.name()));
    }

    /// The Java type for a value of `pgType`. Nullable values box; non-null ones use the primitive
    /// where there is one.
    TypeName javaType(String pgType, boolean nullable, String where) {
        if (pgType.startsWith("_")) {
            var element = pgType.substring(1);
            if (!arrayElementSupported(element)) {
                throw new GenException(where + ": arrays of '" + element + "' are not supported");
            }
            return ParameterizedTypeName.get(LIST, javaType(element, true, where));
        }

        var pgEnum = catalog.pgEnum(pgType);
        if (pgEnum != null) return enumClass(pgEnum);

        var type = switch (pgType) {
            case "int2", "int4" -> TypeName.INT;
            case "int8" -> TypeName.LONG;
            case "bool" -> TypeName.BOOLEAN;
            case "float4" -> TypeName.FLOAT;
            case "float8" -> TypeName.DOUBLE;
            case "text", "varchar", "bpchar", "json", "jsonb" -> STRING;
            case "uuid" -> ClassName.get(UUID.class);
            case "timestamptz" -> ClassName.get(Instant.class);
            case "date" -> ClassName.get(LocalDate.class);
            case "numeric" -> ClassName.get(BigDecimal.class);
            case "bytea" -> ArrayTypeName.of(TypeName.BYTE);
            default -> throw new GenException(where + ": no Java type for Postgres type '" + pgType + "'");
        };
        return nullable ? type.box() : type;
    }

    /// An expression reading column `col` of `rs` as [#javaType].
    CodeBlock read(String pgType, boolean nullable, String rs, CodeBlock col) {
        if (pgType.startsWith("_")) {
            var element = pgType.substring(1);
            var pgEnum = catalog.pgEnum(element);
            if (pgEnum != null) {
                return CodeBlock.of("$T.listFromPg($T.getList($N, $L, $T.class))", enumClass(pgEnum), JDBC, rs, col, STRING);
            }
            return CodeBlock.of("$T.getList($N, $L, $T.class)", JDBC, rs, col, javaType(element, true, "array"));
        }

        var pgEnum = catalog.pgEnum(pgType);
        if (pgEnum != null) return CodeBlock.of("$T.fromPg($N.getString($L))", enumClass(pgEnum), rs, col);

        if (nullable) {
            return switch (pgType) {
                case "text", "varchar", "bpchar", "json", "jsonb" -> CodeBlock.of("$N.getString($L)", rs, col);
                case "numeric" -> CodeBlock.of("$N.getBigDecimal($L)", rs, col);
                case "bytea" -> CodeBlock.of("$N.getBytes($L)", rs, col);
                case "timestamptz" -> CodeBlock.of("$T.getInstant($N, $L)", JDBC, rs, col);
                default -> CodeBlock.of("$N.getObject($L, $T.class)", rs, col, javaType(pgType, true, "column"));
            };
        }
        return switch (pgType) {
            case "int2", "int4" -> CodeBlock.of("$N.getInt($L)", rs, col);
            case "int8" -> CodeBlock.of("$N.getLong($L)", rs, col);
            case "bool" -> CodeBlock.of("$N.getBoolean($L)", rs, col);
            case "float4" -> CodeBlock.of("$N.getFloat($L)", rs, col);
            case "float8" -> CodeBlock.of("$N.getDouble($L)", rs, col);
            case "text", "varchar", "bpchar", "json", "jsonb" -> CodeBlock.of("$N.getString($L)", rs, col);
            case "numeric" -> CodeBlock.of("$N.getBigDecimal($L)", rs, col);
            case "bytea" -> CodeBlock.of("$N.getBytes($L)", rs, col);
            case "timestamptz" -> CodeBlock.of("$T.getInstant($N, $L)", JDBC, rs, col);
            default -> CodeBlock.of("$N.getObject($L, $T.class)", rs, col, javaType(pgType, true, "column"));
        };
    }

    /// A statement binding `value` into parameter `index` of `ps`. The index is source text rather
    /// than a number because a query with holes counts its parameters at runtime.
    CodeBlock bind(String pgType, boolean nullable, String ps, String index, CodeBlock value) {
        if (pgType.startsWith("_")) {
            var element = pgType.substring(1);
            var pgEnum = catalog.pgEnum(element);
            var values = pgEnum == null ? value : CodeBlock.of("$T.pgLabels($L)", enumClass(pgEnum), value);
            return CodeBlock.of("$T.setList($N, $L, $S, $L)", JDBC, ps, index, element, values);
        }

        var pgEnum = catalog.pgEnum(pgType);
        if (pgEnum != null) {
            var label = nullable
                ? CodeBlock.of("$L == null ? null : $L.pgLabel()", value, value)
                : CodeBlock.of("$L.pgLabel()", value);
            return CodeBlock.of("$T.setInferred($N, $L, $L)", JDBC, ps, index, label);
        }

        if (nullable) {
            return switch (pgType) {
                case "timestamptz" -> CodeBlock.of("$T.setInstant($N, $L, $L)", JDBC, ps, index, value);
                case "json", "jsonb" -> CodeBlock.of("$T.setInferred($N, $L, $L)", JDBC, ps, index, value);
                default -> CodeBlock.of("$N.setObject($L, $L, $T.$L)", ps, index, value, Types.class, sqlTypeConstant(pgType));
            };
        }
        return switch (pgType) {
            case "int2", "int4" -> CodeBlock.of("$N.setInt($L, $L)", ps, index, value);
            case "int8" -> CodeBlock.of("$N.setLong($L, $L)", ps, index, value);
            case "bool" -> CodeBlock.of("$N.setBoolean($L, $L)", ps, index, value);
            case "float4" -> CodeBlock.of("$N.setFloat($L, $L)", ps, index, value);
            case "float8" -> CodeBlock.of("$N.setDouble($L, $L)", ps, index, value);
            case "text", "varchar", "bpchar" -> CodeBlock.of("$N.setString($L, $L)", ps, index, value);
            case "json", "jsonb" -> CodeBlock.of("$T.setInferred($N, $L, $L)", JDBC, ps, index, value);
            case "numeric" -> CodeBlock.of("$N.setBigDecimal($L, $L)", ps, index, value);
            case "bytea" -> CodeBlock.of("$N.setBytes($L, $L)", ps, index, value);
            case "timestamptz" -> CodeBlock.of("$T.setInstant($N, $L, $L)", JDBC, ps, index, value);
            case "uuid", "date" -> CodeBlock.of("$N.setObject($L, $L)", ps, index, value);
            default -> throw new GenException("no JDBC binding for Postgres type '" + pgType + "'");
        };
    }

    private static String sqlTypeConstant(String pgType) {
        return switch (pgType) {
            case "int2" -> "SMALLINT";
            case "int4" -> "INTEGER";
            case "int8" -> "BIGINT";
            case "bool" -> "BOOLEAN";
            case "float4" -> "REAL";
            case "float8" -> "DOUBLE";
            case "text", "varchar", "bpchar" -> "VARCHAR";
            case "numeric" -> "NUMERIC";
            case "bytea" -> "BINARY";
            case "uuid" -> "OTHER";
            case "date" -> "DATE";
            default -> throw new GenException("no JDBC binding for Postgres type '" + pgType + "'");
        };
    }

    /// pgjdbc hands array elements back as the JDBC default for the element type, so only the ones
    /// whose default already is the type we map to can go through [Jdbc#getList] unconverted.
    private boolean arrayElementSupported(String pgType) {
        if (catalog.pgEnum(pgType) != null) return true;
        return switch (pgType) {
            case "int2", "int4", "int8", "bool", "float4", "float8", "text", "varchar", "bpchar", "uuid", "numeric" -> true;
            default -> false;
        };
    }

    @Nullable Catalog.PgEnum pgEnum(String pgType) {
        return catalog.pgEnum(pgType);
    }
}
