package net.hollowcube.sqlgen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// Emits the types that come from the schema rather than from any one query: one record per table,
/// one Java enum per Postgres enum.
///
/// A table's record also carries the mapper that reads it out of a result set, package-private
/// beside the shape it builds rather than off in a `Mappers` class every query has to reach into.
final class EmitCatalog {

    private final Model.Database database;
    private final TypeMap types;

    EmitCatalog(Model.Database database, TypeMap types) {
        this.database = database;
        this.types = types;
    }

    ClassName tableClass(Catalog.Table table) {
        return ClassName.get(database.packageName(), Names.pascal(table.name()));
    }

    TypeSpec tableRecord(Catalog.Table table) {
        var components = new ArrayList<ParameterSpec>(table.columns().size());
        for (var column : table.columns()) {
            components.add(Emitter.component(
                types.javaType(column.pgType(), !column.notNull(), "table " + table.name()),
                Names.camel(column.name()),
                !column.notNull()));
        }
        var mappers = new ArrayList<MethodSpec>(2);
        mappers.add(mapper(table));
        if (database.nullableEmbeds().contains(table.name())) mappers.add(nullableMapper(table));

        return Emitter.record(Names.pascal(table.name()),
            "A row of the `" + table.name() + "` table.\n", components, mappers);
    }

    TypeSpec pgEnum(Catalog.PgEnum pgEnum) {
        var self = types.enumClass(pgEnum);
        var listOfSelf = ParameterizedTypeName.get(ClassName.get(List.class), self);
        var listOfString = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));

        var type = TypeSpec.enumBuilder(self.simpleName())
            .addJavadoc("The `$L` Postgres enum.\n", pgEnum.name())
            .addModifiers(Modifier.PUBLIC);
        for (var label : pgEnum.labels()) {
            type.addEnumConstant(Names.constant(label), TypeSpec.anonymousClassBuilder("$S", label).build());
        }

        return type
            .addField(String.class, "pgLabel", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder()
                .addParameter(String.class, "pgLabel")
                .addStatement("this.pgLabel = pgLabel")
                .build())
            .addMethod(MethodSpec.methodBuilder("pgLabel")
                .addJavadoc("The label this constant is stored as.\n")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return pgLabel")
                .build())
            .addMethod(MethodSpec.methodBuilder("fromPg")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(self)
                .addAnnotation(Emitter.NULLABLE)
                .addParameter(Emitter.component(ClassName.get(String.class), "label", true))
                .addStatement("if (label == null) return null")
                .beginControlFlow("for ($T value : values())", self)
                .addStatement("if (value.pgLabel.equals(label)) return value")
                .endControlFlow()
                .addStatement("throw new $T($S + label)", IllegalArgumentException.class,
                    "unknown " + pgEnum.name() + " label: ")
                .build())
            .addMethod(MethodSpec.methodBuilder("listFromPg")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(listOfSelf)
                .addAnnotation(Emitter.NULLABLE)
                .addParameter(Emitter.component(listOfString, "labels", true))
                .addStatement("if (labels == null) return null")
                .addStatement("$T values = new $T<>(labels.size())", listOfSelf, ArrayList.class)
                .addStatement("for ($T label : labels) values.add(fromPg(label))", String.class)
                .addStatement("return values")
                .build())
            .addMethod(MethodSpec.methodBuilder("pgLabels")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(listOfString)
                .addAnnotation(Emitter.NULLABLE)
                .addParameter(Emitter.component(listOfSelf, "values", true))
                .addStatement("if (values == null) return null")
                .addStatement("$T labels = new $T<>(values.size())", listOfString, ArrayList.class)
                .addStatement("for ($T value : values) labels.add(value == null ? null : value.pgLabel)", self)
                .addStatement("return labels")
                .build())
            .build();
    }

    /// Reads this table's columns out of a result set, starting at a 1-based offset. Every query
    /// that embeds the table shares it, which is the whole point of having one record per table.
    private MethodSpec mapper(Catalog.Table table) {
        var arguments = CodeBlock.builder();
        for (int i = 0; i < table.columns().size(); i++) {
            var column = table.columns().get(i);
            if (i > 0) arguments.add(",\n");
            arguments.add(types.read(column.pgType(), !column.notNull(), "rs", offset(i)));
        }

        return MethodSpec.methodBuilder("read")
            .addJavadoc("Reads one `$L` row, its first column at `col`.\n", table.name())
            .addModifiers(Modifier.STATIC)
            .returns(tableClass(table))
            .addParameter(ResultSet.class, "rs")
            .addParameter(int.class, "col")
            .addException(SQLException.class)
            .addCode(CodeBlock.builder()
                .add("return new $T(", tableClass(table))
                .add("$>$>\n").add(arguments.build()).add("$<$<)")
                .add(";\n")
                .build())
            .build();
    }

    /// The outer-join variant: an all-null segment means there was no joined row, and the primary
    /// key is what says so — it is the one column that cannot be null in a row that exists.
    private MethodSpec nullableMapper(Catalog.Table table) {
        var pk = table.primaryKey().getFirst();
        int index = 0;
        for (int i = 0; i < table.columns().size(); i++) {
            if (table.columns().get(i).name().equals(pk)) index = i;
        }

        return MethodSpec.methodBuilder("readOrNull")
            .addJavadoc("As {@link #read}, for a row that an outer join may not have produced.\n")
            .addModifiers(Modifier.STATIC)
            .addAnnotation(Emitter.NULLABLE)
            .returns(tableClass(table))
            .addParameter(ResultSet.class, "rs")
            .addParameter(int.class, "col")
            .addException(SQLException.class)
            .addStatement("if (rs.getObject($L) == null) return null", offset(index))
            .addStatement("return read(rs, col)")
            .build();
    }

    private static CodeBlock offset(int i) {
        return i == 0 ? CodeBlock.of("col") : CodeBlock.of("col + $L", i);
    }
}
