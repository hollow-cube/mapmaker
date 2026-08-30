package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;
import org.jetbrains.annotations.Nullable;

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

    /// Whether any method's request body is a blob, which is what decides where the server reads
    /// its arguments from.
    boolean uploads() {
        return methods.stream().anyMatch(method -> method.blob() != null);
    }

    /// Whether any method answers with a blob.
    boolean downloads() {
        return methods.stream().anyMatch(Method::returnsBlob);
    }

    /// Whether any method is json both ways, and so is made with the plain json plumbing.
    boolean jsonCalls() {
        return methods.stream().anyMatch(method -> !method.isBlobCall());
    }

    /// Whether any method has bytes on either side, and so is made with the streaming plumbing.
    boolean blobCalls() {
        return methods.stream().anyMatch(Method::isBlobCall);
    }

    /// Whether any blob call answers with json, which is the one case that reads a streamed response
    /// as a value rather than as bytes.
    boolean blobCallsWithJsonAnswer() {
        return methods.stream().anyMatch(method -> method.isBlobCall() && !method.returnsBlob());
    }

    /// Whether any method answers with json.
    boolean jsonAnswers() {
        return methods.stream().anyMatch(method -> !method.returnsBlob());
    }

    /// One abstract interface method. Every parameter is a named JSON field of the request object;
    /// the return value is the whole response body.
    ///
    /// Unless one of them is a [net.hollowcube.ipc.Blob]: a blob parameter is the request body
    /// itself and the rest of the arguments travel in a header, and a blob return is the response
    /// body itself.
    ///
    /// @param parameters the json arguments, which is every parameter but [#blob]
    /// @param blob       the one parameter the request body is, or null for a json request
    record Method(
        ExecutableElement element,
        String name,
        /// Trailing path segment this method answers on: the method name in kebab case.
        String route,
        List<? extends VariableElement> parameters,
        @Nullable VariableElement blob,
        TypeMirror returnType,
        boolean isVoid,
        boolean returnsBlob
    ) {

        /// Whether either half of this call is bytes, and so cannot go through the json plumbing.
        boolean isBlobCall() {
            return blob != null || returnsBlob;
        }
    }
}
