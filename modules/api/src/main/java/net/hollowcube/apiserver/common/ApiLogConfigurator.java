package net.hollowcube.apiserver.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.google.auto.service.AutoService;

@AutoService(Configurator.class)
@ConfiguratorRank(ConfiguratorRank.CUSTOM_NORMAL_PRIORITY)
public final class ApiLogConfigurator extends ContextAwareBase implements Configurator {
    // %kvp renders the key values RequestLog attaches, and nothing (so no trailing space) without.
    private static final String TEXT_PATTERN =
        "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%replace( %kvp){'^ $', ''}%n";

    @Override
    public ExecutionStatus configure(LoggerContext loggerContext) {
        var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(loggerContext);
        appender.setName("console");
        appender.setEncoder(createEncoder(loggerContext));
        appender.start();

        var root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(appender);
        loggerContext.getLogger("com.zaxxer.hikari").setLevel(Level.INFO);
        loggerContext.getLogger("org.postgresql").setLevel(Level.WARN);

        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    private static Encoder<ILoggingEvent> createEncoder(LoggerContext loggerContext) {
        if ("json".equalsIgnoreCase(System.getenv("LOG_FORMAT"))) {
            var encoder = new JsonLogEncoder();
            encoder.setContext(loggerContext);
            encoder.start();
            return encoder;
        }

        var encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(TEXT_PATTERN);
        encoder.start();
        return encoder;
    }
}
