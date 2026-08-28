package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/// The validated shape of one `@Ipc` interface, as both emitters read it.
record IpcModel(
    TypeElement element,
    ClassName interfaceName,
    ClassName clientName,
    ClassName serverName,
    /// Path the handler is mounted at, and the prefix the client posts to, e.g. `/head-database`.
    String path,
    List<Method> methods
) {

    /// The service's name for a span: its path without the leading slash.
    String serviceName() {
        return path.substring(1);
    }

    /// One abstract interface method. Every parameter is a named JSON field of the request object;
    /// the return value is the whole response body.
    record Method(
        ExecutableElement element,
        String name,
        /// Trailing path segment this method answers on: the method name in kebab case.
        String route,
        List<? extends VariableElement> parameters,
        TypeMirror returnType,
        boolean isVoid
    ) {
    }
}
