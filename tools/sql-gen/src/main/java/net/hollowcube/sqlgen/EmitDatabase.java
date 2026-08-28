package net.hollowcube.sqlgen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

/// Emits the one entry point: a long-lived object holding the pool, with a field per query file.
///
/// It owns no connection of its own. Each call borrows one from the pool, runs a single statement
/// under autocommit, and gives it straight back. `tx` is the exception, and the only reason to reach
/// for it is an invariant that spans more than one statement.
final class EmitDatabase {

    private static final ClassName TRANSACTION = ClassName.get("net.hollowcube.sqlgen.runtime", "Transaction");

    static TypeSpec emit(Model.Database database) {
        var self = database.className();
        var tx = self.nestedClass("Tx");
        var fake = self.nestedClass("Fake");
        var result = TypeVariableName.get("R");

        var type = TypeSpec.classBuilder(self.simpleName())
            .addJavadoc("Every query in this schema, grouped by the file it was written in.\n")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(DataSource.class, "dataSource", Modifier.PRIVATE, Modifier.FINAL);

        var constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(DataSource.class, "dataSource")
            .addStatement("this.dataSource = dataSource")
            .addStatement("$T source = $T.pooled(dataSource)", Emitter.CONNECTION_SOURCE, Emitter.CONNECTION_SOURCE);
        var fakeConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(fake, "fake")
            .addStatement("this.dataSource = null");

        for (var group : database.groups()) {
            type.addField(group.interfaceName(), group.fieldName(), Modifier.PUBLIC, Modifier.FINAL);
            constructor.addStatement("this.$N = new $T(source)", group.fieldName(), group.implName());
            fakeConstructor.addStatement("this.$N = fake.$N", group.fieldName(), group.fieldName());
        }

        return type
            .addMethod(constructor.build())
            .addMethod(fakeConstructor.build())
            .addMethod(MethodSpec.methodBuilder("tx")
                .addJavadoc("Runs `work` on one connection, committing when it returns and rolling back "
                    + "if it throws.\n\nNesting is an error, not a no-op: an inner block that looks like a "
                    + "transaction but commits with the outer one is the kind of thing that only shows up "
                    + "in an incident.\n")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), tx), "work")
                .addCode(CodeBlock.builder()
                    .add("txResult(tx -> {\n").indent()
                    .addStatement("work.accept(tx)")
                    .addStatement("return null")
                    .unindent().add("});\n")
                    .build())
                .build())
            .addMethod(MethodSpec.methodBuilder("txResult")
                .addJavadoc("As {@link #tx($T)}, for a transaction with something to hand back.\n\nIt is a "
                    + "separate name rather than an overload because `tx(tx -> tx.things.insert(...))` would "
                    + "otherwise be ambiguous between the two.\n", Consumer.class)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(result)
                .returns(result)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class), tx, result), "work")
                .addStatement("return $T.run(dataSource(), conn -> work.apply(new $T(conn)))", TRANSACTION, tx)
                .build())
            .addMethod(MethodSpec.methodBuilder("dataSource")
                .addJavadoc("The pool every query here runs on.\n")
                .addModifiers(Modifier.PUBLIC)
                .returns(DataSource.class)
                .addStatement("if (dataSource == null) throw new $T($S)", IllegalStateException.class,
                    "this " + self.simpleName() + " is a fake and has no DataSource")
                .addStatement("return dataSource")
                .build())
            .addMethod(MethodSpec.methodBuilder("fake")
                .addJavadoc("A database whose groups are whatever a test sets on it. Anything left unset "
                    + "throws when it is touched, so a test that reaches a query it did not stub says so "
                    + "instead of returning nothing.\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(fake)
                .addStatement("return new $T()", fake)
                .build())
            .addType(tx(database, tx))
            .addType(fake(database, self, fake))
            .build();
    }

    /// The sibling of the database that runs on one pinned connection. Same fields, same interfaces,
    /// no pool.
    private static TypeSpec tx(Model.Database database, ClassName tx) {
        var type = TypeSpec.classBuilder(tx.simpleName())
            .addJavadoc("One transaction's worth of the same queries, all on the same connection.\n")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addField(Connection.class, "conn", Modifier.PRIVATE, Modifier.FINAL);

        var constructor = MethodSpec.constructorBuilder()
            .addParameter(Connection.class, "conn")
            .addStatement("this.conn = conn")
            .addStatement("$T source = $T.pinned(conn)", Emitter.CONNECTION_SOURCE, Emitter.CONNECTION_SOURCE);

        for (var group : database.groups()) {
            type.addField(group.interfaceName(), group.fieldName(), Modifier.PUBLIC, Modifier.FINAL);
            constructor.addStatement("this.$N = new $T(source)", group.fieldName(), group.implName());
        }

        return type
            .addMethod(constructor.build())
            .addMethod(MethodSpec.methodBuilder("conn")
                .addJavadoc("The connection this transaction holds, for the statements that have no "
                    + "generated query. Do not commit or close it.\n")
                .addModifiers(Modifier.PUBLIC)
                .returns(Connection.class)
                .addStatement("return conn")
                .build())
            .build();
    }

    private static TypeSpec fake(Model.Database database, ClassName self, ClassName fake) {
        var type = TypeSpec.classBuilder(fake.simpleName())
            .addJavadoc("Builds a $T out of stand-in groups.\n", self)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        for (var group : database.groups()) {
            type.addField(FieldSpec
                .builder(group.interfaceName(), group.fieldName(), Modifier.PRIVATE)
                .initializer("new $T.Stub()", group.interfaceName())
                .build());
            type.addMethod(MethodSpec.methodBuilder(group.fieldName())
                .addModifiers(Modifier.PUBLIC)
                .returns(fake)
                .addParameter(group.interfaceName(), group.fieldName())
                .addStatement("this.$N = $N", group.fieldName(), group.fieldName())
                .addStatement("return this")
                .build());
        }

        return type
            .addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(self)
                .addStatement("return new $T(this)", self)
                .build())
            .build();
    }

    private EmitDatabase() {
    }
}
