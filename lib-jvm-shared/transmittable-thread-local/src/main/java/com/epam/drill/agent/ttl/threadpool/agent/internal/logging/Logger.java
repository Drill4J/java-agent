/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.ttl.threadpool.agent.internal.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * logger adaptor for ttl java agent, internal use for ttl usage only!
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 2.6.0
 */
public abstract class Logger {
    public static final String TTL_AGENT_LOGGER_KEY = "ttl.agent.logger";
    public static final String STDOUT = "STDOUT";
    public static final String STDERR = "STDERR";

    private static volatile int loggerImplType = -1;

    public static void setLoggerImplType(String type) {
        if (loggerImplType != -1) {
            throw new IllegalStateException("TTL logger implementation type is already set! type = " + loggerImplType);
        }

        if (STDERR.equalsIgnoreCase(type)) loggerImplType = 0;
        else if (STDOUT.equalsIgnoreCase(type)) loggerImplType = 1;
        else loggerImplType = 0;
    }

    /**
     * Only for test code
     */
    public static void setLoggerImplTypeIfNotSetYet(String type) {
        if (loggerImplType == -1) setLoggerImplType(type);
    }

    public static Logger getLogger(Class<?> clazz) {
        if (loggerImplType == -1) throw new IllegalStateException("TTL logger implementation type is NOT set!");

        switch (loggerImplType) {
            case 1:
                return new StdOutLogger(clazz);
            default:
                return new StdErrorLogger(clazz);
        }
    }

    final Class<?> logClass;

    private Logger(Class<?> logClass) {
        this.logClass = logClass;
    }

    public void info(String msg) {
        log(Level.INFO, msg, null);
    }

    public abstract void log(Level level, String msg, Throwable thrown);

    private static class StdErrorLogger extends Logger {
        StdErrorLogger(Class<?> clazz) {
            super(clazz);
        }

        @Override
        public void log(Level level, String msg, Throwable thrown) {
            if (level == Level.SEVERE) {
                final String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                System.err.printf("%s %s [%s] %s: %s%n", time, level, Thread.currentThread().getName(), logClass.getSimpleName(), msg);
                if (thrown != null) thrown.printStackTrace();
            }
        }
    }

    private static class StdOutLogger extends Logger {
        StdOutLogger(Class<?> clazz) {
            super(clazz);
        }

        @Override
        public void log(Level level, String msg, Throwable thrown) {
            final String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            System.out.printf("%s %s [%s] %s: %s%n", time, level, Thread.currentThread().getName(), logClass.getSimpleName(), msg);
            if (thrown != null) thrown.printStackTrace(System.out);
        }
    }
}
