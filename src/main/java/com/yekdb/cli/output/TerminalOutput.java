package com.yekdb.cli.output;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Interactive SQL terminal için standart
 * çıktı ve hata akışlarını yönetir.
 *
 * Varsayılan olarak System.out ve System.err
 * kullanılır.
 *
 * Testlerde farklı PrintStream nesneleri
 * constructor üzerinden verilebilir.
 */
public final class TerminalOutput {

    private final PrintStream outputStream;
    private final PrintStream errorStream;

    /**
     * Production kullanımı.
     */
    public TerminalOutput() {

        this(
                System.out,
                System.err
        );
    }

    /**
     * Test edilebilirlik için çıktı akışlarının
     * dışarıdan verilmesini sağlar.
     */
    public TerminalOutput(
            PrintStream outputStream,
            PrintStream errorStream
    ) {

        this.outputStream =
                Objects.requireNonNull(
                        outputStream,
                        "Output stream cannot be null."
                );

        this.errorStream =
                Objects.requireNonNull(
                        errorStream,
                        "Error stream cannot be null."
                );
    }

    /**
     * Normal çıktı üretir.
     */
    public void print(
            String value
    ) {

        outputStream.print(
                value
        );
    }

    /**
     * Normal çıktı üretir ve yeni satıra geçer.
     */
    public void println(
            String value
    ) {

        outputStream.println(
                value
        );
    }

    /**
     * Boş satır üretir.
     */
    public void println() {

        outputStream.println();
    }

    /**
     * Hata çıktısı üretir.
     */
    public void error(
            String value
    ) {

        errorStream.println(
                value
        );
    }

    public PrintStream getOutputStream() {
        return outputStream;
    }

    public PrintStream getErrorStream() {
        return errorStream;
    }
}