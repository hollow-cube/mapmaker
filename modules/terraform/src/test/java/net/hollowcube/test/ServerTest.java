package net.hollowcube.test;

import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ServerTest.Before.class)
@ExtendWith(ServerTest.After.class)
@ExtendWith(ServerTest.ParameterResolver.class)
public @interface ServerTest {

    ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ServerTest.class);
    @SuppressWarnings("ClassEscapesDefinedScope")
    CoreEnv CORE = new CoreEnv();

    final class Before implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            System.setProperty("minestom.viewable-packet", "false");
            System.setProperty("minestom.event.multiple-parents", "true");
            System.setProperty("minestom.use-new-chunk-sending", "true");
        }
    }

    final class After implements AfterAllCallback {

        @Override
        public void afterAll(ExtensionContext extensionContext) throws Exception {
            CORE.afterAllHook();
        }

    }

    final class ParameterResolver extends TypeBasedParameterResolver<TestEnv> {
        @Override
        public TestEnv resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            var core = CORE;

            var testId = String.format("%s-%s",
                    extensionContext.getTestClass().get().getName().replace(".", "_"),
                    extensionContext.getTestMethod().get().getName()
            );

            var server = MinecraftServer.updateProcess();

            return new TestEnvImpl(core, extensionContext.getUniqueId(), testId, server);
        }
    }
}
