package net.hollowcube.sqlgen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// Emits one query file: the interface call sites use, and the implementation that runs the SQL.
///
/// A query whose result is not already some table's row needs a record of its own, and it is nested
/// in the interface next to the query that returns it rather than left at package level, where a
/// file of ten queries would otherwise put ten more names.
///
/// The interface exists so tests can substitute a group wholesale — it is the only seam, and it is
/// free, because the implementation is what the database hands out.
final class EmitGroup {

    private static final ClassName SQL_FRAGMENT = ClassName.get("net.hollowcube.sqlgen.runtime", "SqlFragment");
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName ARRAY_LIST = ClassName.get(ArrayList.class);

    private final TypeMap types;
    private final Model.Group group;
    private final EmitCatalog catalog;

    EmitGroup(Model.Database database, TypeMap types, Model.Group group) {
        this.types = types;
        this.group = group;
        this.catalog = new EmitCatalog(database, types);
    }

    TypeSpec groupInterface() {
        var type = TypeSpec.interfaceBuilder(group.interfaceName().simpleName())
            .addJavadoc("The queries in `$L.sql`.\n", group.fieldName())
            .addAnnotation(Emitter.unusedReturnValue())
            .addModifiers(Modifier.PUBLIC);
        for (var query : group.queries()) {
            type.addMethod(signature(query).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
        }
        for (var record : records()) type.addType(record);
        return type.addType(stub()).build();
    }

    /// What a faked database uses for the groups a test did not set. Every method throws, so
    /// touching a query the test did not think about is a failure rather than an empty result.
    private TypeSpec stub() {
        var type = TypeSpec.classBuilder("Stub")
            .addJavadoc("A $T where every query throws. Used for the groups a fake database was not "
                + "given, and open to subclassing so a test can override only the queries it "
                + "exercises.\n", group.interfaceName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterface(group.interfaceName());
        for (var query : group.queries()) {
            type.addMethod(signature(query)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("throw new $T($S)", UnsupportedOperationException.class,
                    group.interfaceName().simpleName() + "." + query.name() + " is not stubbed on this fake")
                .build());
        }
        return type.build();
    }

    TypeSpec groupImpl() {
        var type = TypeSpec.classBuilder(group.implName().simpleName())
            .addJavadoc("Runs $T against a connection borrowed per statement.\n", group.interfaceName())
            .addAnnotation(Emitter.unusedReturnValue())
            .addModifiers(Modifier.FINAL)
            .addSuperinterface(group.interfaceName())
            .addField(Emitter.CONNECTION_SOURCE, "source", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder()
                .addParameter(Emitter.CONNECTION_SOURCE, "source")
                .addStatement("this.source = source")
                .build());

        for (var query : group.queries()) {
            var parts = sqlParts(query);
            for (int i = 0; i < parts.size(); i++) {
                type.addField(FieldSpec.builder(String.class, constant(query, i),
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(Emitter.sqlLiteral(parts.get(i)))
                    .build());
            }
        }
        for (var query : group.queries()) {
            type.addMethod(signature(query)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode(body(query))
                .build());
        }
        return type.build();
    }

    /// The records this file needs of its own: one per query whose result is not already some
    /// table's row, and one per query with too many parameters to pass positionally.
    private List<TypeSpec> records() {
        var records = new ArrayList<TypeSpec>();
        for (var query : group.queries()) {
            if (query.paramsClass() == null) continue;

            var arguments = new ArrayList<ParameterSpec>(query.params().size());
            for (var param : query.params()) arguments.add(Emitter.component(param.type(), param.name(), false));
            records.add(Emitter.record(query.paramsClass().simpleName(),
                "The arguments of `" + query.name() + "`.\n", arguments, Modifier.STATIC));
        }
        for (var query : group.queries()) {
            if (query.result().shape() != Model.Shape.ROW) continue;

            var components = new ArrayList<ParameterSpec>();
            for (var component : query.result().components()) {
                components.add(switch (component) {
                    case Model.Embed embed -> Emitter.component(catalog.tableClass(embed.table()), embed.name(), embed.nullable());
                    case Model.Value value -> Emitter.component(value.type(), value.name(), value.nullable());
                });
            }
            // A member of an interface is implicitly `public static`, but javapoet wants telling.
            records.add(Emitter.record(query.result().rowClass().simpleName(),
                "A row of `" + query.name() + "`.\n", components, Modifier.STATIC));
        }
        return List.copyOf(records);
    }

    private MethodSpec.Builder signature(Model.Query query) {
        var method = MethodSpec.methodBuilder(query.name()).returns(returnType(query));
        if (query.tag() == QueryFile.Tag.ONE && (!query.exactlyOne() || nullableScalar(query)))
            method.addAnnotation(Emitter.NULLABLE);
        if (query.paramsClass() != null) {
            method.addParameter(query.paramsClass(), "params");
        } else {
            for (var param : query.params()) method.addParameter(param.type(), param.name());
        }
        for (var hole : query.holes()) {
            method.addParameter(ParameterSpec.builder(SQL_FRAGMENT, hole.kind().argument)
                .addAnnotation(Emitter.NULLABLE)
                .build());
        }
        return method;
    }

    /// The statement, split at each hole. A query with no holes is one part and one constant.
    private static List<String> sqlParts(Model.Query query) {
        var parts = new ArrayList<String>();
        int start = 0;
        for (var hole : query.holes()) {
            parts.add(query.sql().substring(start, hole.sqlOffset()));
            start = hole.sqlOffset();
        }
        parts.add(query.sql().substring(start));
        return parts;
    }

    private static String constant(Model.Query query, int part) {
        return query.holes().isEmpty() ? query.constantName() : query.constantName() + "_" + part;
    }

    /// The statement to prepare: the single constant, or the parts with each caller fragment spliced
    /// between them.
    private static CodeBlock sql(Model.Query query) {
        if (query.holes().isEmpty()) return CodeBlock.of("$N", query.constantName());

        var sql = CodeBlock.builder().add("$N$>$>", constant(query, 0));
        for (int i = 0; i < query.holes().size(); i++) {
            var hole = query.holes().get(i);
            sql.add("\n+ $T.clause($S, $N)", SQL_FRAGMENT, hole.kind().keyword, hole.kind().argument)
                .add("\n+ $N", constant(query, i + 1));
        }
        return sql.add("$<$<").build();
    }

    private TypeName returnType(Model.Query query) {
        return switch (query.tag()) {
            case EXEC -> TypeName.LONG;
            // A row that is always there has no "no row" to box for; a scalar's own nullability is
            // already in its type.
            case ONE -> query.exactlyOne() ? rowType(query) : rowType(query).box();
            case MANY -> ParameterizedTypeName.get(LIST, rowType(query).box());
        };
    }

    private static boolean nullableScalar(Model.Query query) {
        return query.result().shape() == Model.Shape.SCALAR
            && ((Model.Value) query.result().components().getFirst()).nullable();
    }

    private TypeName rowType(Model.Query query) {
        var result = query.result();
        return switch (result.shape()) {
            case NONE -> TypeName.LONG;
            case SCALAR -> ((Model.Value) result.components().getFirst()).type();
            case TABLE, ROW -> result.rowClass();
        };
    }

    private CodeBlock body(Model.Query query) {
        var inner = CodeBlock.builder();
        inner.add(binds(query));
        inner.add(execute(query));

        return CodeBlock.builder()
            .beginControlFlow("try")
            .addStatement("$T conn = source.acquire()", Connection.class)
            .beginControlFlow("try ($T ps = conn.prepareStatement($L))", PreparedStatement.class, sql(query))
            .add(inner.build())
            .nextControlFlow("finally")
            .addStatement("source.release(conn)")
            .endControlFlow()
            .nextControlFlow("catch ($T e)", SQLException.class)
            .addStatement("throw $T.rethrow(e)", Emitter.SNEAKY)
            .endControlFlow()
            .build();
    }

    /// Binds every parameter in the order the statement asks for them. Without holes the indices are
    /// constants; with them a fragment contributes an unknown number, so the index has to be counted
    /// at runtime from there on.
    private CodeBlock binds(Model.Query query) {
        var code = CodeBlock.builder();
        boolean counted = !query.holes().isEmpty();
        if (counted) code.addStatement("int i = 1");

        int hole = 0;
        for (int bind = 0; bind <= query.binds().size(); bind++) {
            while (hole < query.holes().size() && query.holes().get(hole).bindOffset() == bind) {
                var kind = query.holes().get(hole++).kind();
                code.addStatement("i = $T.bind(ps, i, $N)", SQL_FRAGMENT, kind.argument);
            }
            if (bind == query.binds().size()) break;

            var param = query.params().get(query.binds().get(bind));
            var value = query.paramsClass() == null
                ? CodeBlock.of("$N", param.name())
                : CodeBlock.of("params.$N()", param.name());
            code.addStatement(types.bind(param.pgType(), false, "ps", counted ? "i++" : String.valueOf(bind + 1), value));
        }
        return code.build();
    }

    private CodeBlock execute(Model.Query query) {
        if (query.tag() == QueryFile.Tag.EXEC) {
            return CodeBlock.builder().addStatement("return ps.executeLargeUpdate()").build();
        }

        var row = rowType(query).box();
        var body = CodeBlock.builder().beginControlFlow("try ($T rs = ps.executeQuery())", ResultSet.class);
        if (query.tag() == QueryFile.Tag.ONE && query.exactlyOne()) {
            body.addStatement("if (!rs.next()) throw new $T($S)", SQLException.class, query.name() + " returned no row")
                .addStatement("return $L", map(query));
        } else if (query.tag() == QueryFile.Tag.ONE) {
            body.addStatement("return rs.next() ? $L : null", map(query));
        } else {
            body.addStatement("$T<$T> rows = new $T<>()", LIST, row, ARRAY_LIST)
                .addStatement("while (rs.next()) rows.add($L)", map(query))
                .addStatement("return rows");
        }
        return body.endControlFlow().build();
    }

    /// The expression that turns the result set's current row into one value.
    private CodeBlock map(Model.Query query) {
        var result = query.result();
        return switch (result.shape()) {
            case NONE -> throw new IllegalStateException();
            case SCALAR -> {
                var value = (Model.Value) result.components().getFirst();
                yield types.read(value.pgType(), value.nullable(), "rs", CodeBlock.of("$L", value.column()));
            }
            case TABLE -> read(result.components().getFirst());
            case ROW -> {
                var arguments = CodeBlock.builder();
                for (int i = 0; i < result.components().size(); i++) {
                    if (i > 0) arguments.add(", ");
                    arguments.add(read(result.components().get(i)));
                }
                yield CodeBlock.of("new $T($L)", result.rowClass(), arguments.build());
            }
        };
    }

    private CodeBlock read(Model.Component component) {
        return switch (component) {
            case Model.Embed embed -> CodeBlock.of("$T.$N(rs, $L)", catalog.tableClass(embed.table()),
                embed.nullable() ? "readOrNull" : "read", embed.column());
            case Model.Value value -> types.read(value.pgType(), value.nullable(), "rs", CodeBlock.of("$L", value.column()));
        };
    }
}
