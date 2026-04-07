package org.myjtools.openbbt.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

class LogConfig {

    private LogConfig() {}

    static void redirectToFile(Path logFile) {
        var ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.reset();

        var encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern("%date{HH:mm:ss.SSS} %-5level %msg%n");
        encoder.start();

        var rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(ctx);
        rollingPolicy.setFileNamePattern(logFile + ".%d{yyyy-MM-dd}.%i.gz");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("5MB"));
        rollingPolicy.setMaxHistory(3);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("20MB"));

        var appender = new RollingFileAppender<>();
        appender.setContext(ctx);
        appender.setFile(logFile.toString());
        appender.setEncoder(encoder);
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.setParent(appender);
        rollingPolicy.start();
        appender.start();

        var root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(appender);

        var openbbt = ctx.getLogger("org.myjtools.openbbt");
        openbbt.setLevel(Level.INFO);
    }
}