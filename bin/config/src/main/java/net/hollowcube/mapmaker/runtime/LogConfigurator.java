package net.hollowcube.mapmaker.runtime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.google.auto.service.AutoService;

import java.util.Map;

/// Logging for the game servers, in code rather than a logback.xml: parsing one is what pulls
/// logback's Joran and the JDK's xerces into a native image.
///
/// Ranked above modules/api's configurator so that it is the one the development server, which
/// hosts the api processes too, ends up with.
@AutoService(Configurator.class)
@ConfiguratorRank(ConfiguratorRank.CUSTOM_HIGH_PRIORITY)
public final class LogConfigurator extends ContextAwareBase implements Configurator {
    private static final String PATTERN =
        "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%replace( %kvp){'^ $', ''}%n";

    private static final Map<String, Level> LEVELS = Map.ofEntries(
        Map.entry("net.minestom.server", Level.INFO),
        Map.entry("io.netty", Level.WARN),
        Map.entry("io.grpc", Level.WARN),
        Map.entry("io.helidon", Level.WARN),
        Map.entry("jdk.event.security", Level.WARN),
        Map.entry("sun.net.www", Level.WARN),
        Map.entry("com.github.benmanes.caffeine.cache", Level.ERROR),
        Map.entry("com.zaxxer.hikari", Level.INFO),
        Map.entry("org.postgresql", Level.WARN));

    @Override
    public ExecutionStatus configure(LoggerContext loggerContext) {
        // Tests configure themselves with modules/test's logback-test.xml.
        if (LogConfigurator.class.getClassLoader().getResource("logback-test.xml") != null)
            return ExecutionStatus.INVOKE_NEXT_IF_ANY;

        var encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(PATTERN);
        encoder.start();

        var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(loggerContext);
        appender.setName("console");
        appender.setEncoder(encoder);
        appender.start();

        var root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(appender);
        LEVELS.forEach((name, level) -> loggerContext.getLogger(name).setLevel(level));

        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }
}
