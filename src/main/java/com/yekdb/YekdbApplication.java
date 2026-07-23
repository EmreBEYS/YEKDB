package com.yekdb;

import com.yekdb.core.YekdbEngine;
import com.yekdb.exception.YekdbException;

public final class YekdbApplication {

    private YekdbApplication() {
    }

    public static void main(String[] args) {
        YekdbEngine engine = new YekdbEngine();

        registerShutdownHook(engine);

        try {
            engine.start();
        } catch (YekdbException exception) {
            printStartupError(exception);
            System.exit(1);
        } catch (RuntimeException exception) {
            printUnexpectedError(exception);
            System.exit(1);
        }
    }

    private static void registerShutdownHook(
            YekdbEngine engine
    ) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        engine::shutdown,
                        "yekdb-shutdown-hook"
                )
        );
    }

    private static void printStartupError(
            YekdbException exception
    ) {
        System.err.println(
                "[YEKDB] Database engine could not be started."
        );

        System.err.println(
                "[YEKDB] Error: " + exception.getMessage()
        );

        exception.printStackTrace();
    }

    private static void printUnexpectedError(
            RuntimeException exception
    ) {
        System.err.println(
                "[YEKDB] An unexpected system error occurred."
        );

        System.err.println(
                "[YEKDB] Error: " + exception.getMessage()
        );

        exception.printStackTrace();
    }
}