package org.openclover.core.reporters.html;

import org.openclover.runtime.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Bridges the SLF4J API, which Velocity 2.x logs through, to the OpenClover {@link Logger}.
 */
public class VelocityLogAdapter extends MarkerIgnoringBase {

    private final Logger delegate;

    public VelocityLogAdapter(Logger delegate) {
        this.delegate = delegate;
        this.name = "velocity";
    }

    private void log(int logLevel, String message, Throwable t) {
        delegate.log(logLevel, "[Velocity] " + message, t);
    }

    private void logFormatted(int logLevel, String format, Object... arguments) {
        final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
        log(logLevel, tuple.getMessage(), tuple.getThrowable());
    }

    @Override
    public boolean isTraceEnabled() {
        return Logger.isVerbose();
    }

    @Override
    public void trace(String msg) {
        log(Logger.LOG_VERBOSE, msg, null);
    }

    @Override
    public void trace(String format, Object arg) {
        logFormatted(Logger.LOG_VERBOSE, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logFormatted(Logger.LOG_VERBOSE, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logFormatted(Logger.LOG_VERBOSE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(Logger.LOG_VERBOSE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return Logger.isDebug() || Logger.isVerbose();
    }

    @Override
    public void debug(String msg) {
        log(Logger.LOG_DEBUG, msg, null);
    }

    @Override
    public void debug(String format, Object arg) {
        logFormatted(Logger.LOG_DEBUG, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logFormatted(Logger.LOG_DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logFormatted(Logger.LOG_DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Logger.LOG_DEBUG, msg, t);
    }

    // note: info messages are logged at the debug level, as Velocity is quite verbose
    // about macro definitions and resource loading

    @Override
    public boolean isInfoEnabled() {
        return isDebugEnabled();
    }

    @Override
    public void info(String msg) {
        log(Logger.LOG_DEBUG, msg, null);
    }

    @Override
    public void info(String format, Object arg) {
        logFormatted(Logger.LOG_DEBUG, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logFormatted(Logger.LOG_DEBUG, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logFormatted(Logger.LOG_DEBUG, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Logger.LOG_DEBUG, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        log(Logger.LOG_WARN, msg, null);
    }

    @Override
    public void warn(String format, Object arg) {
        logFormatted(Logger.LOG_WARN, format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logFormatted(Logger.LOG_WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logFormatted(Logger.LOG_WARN, format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Logger.LOG_WARN, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        log(Logger.LOG_ERR, msg, null);
    }

    @Override
    public void error(String format, Object arg) {
        logFormatted(Logger.LOG_ERR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logFormatted(Logger.LOG_ERR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logFormatted(Logger.LOG_ERR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Logger.LOG_ERR, msg, t);
    }
}
