package net.hollowcube.test;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ServerTest.Before.class)
@ExtendWith(ServerTest.ParameterResolver.class)
public @interface ServerTest {

    final class Before implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            System.setProperty("minestom.viewable-packet", "false");
            System.setProperty("minestom.event.multiple-parents", "true");
            System.setProperty("minestom.use-new-chunk-sending", "true");
        }
    }

    final class ParameterResolver extends TypeBasedParameterResolver<TestEnv> {
        @Override
        public TestEnv resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            try {
                System.out.println(Path.of(".").toRealPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new TestEnvImpl(
                    extensionContext.getUniqueId(),
                    String.format("%s-%s",
                            extensionContext.getTestClass().get().getName().replace(".", "_"),
                            extensionContext.getTestMethod().get().getName()),
                    MinecraftServer.updateProcess()
            );
        }
    }
}
