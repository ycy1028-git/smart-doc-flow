package io.ycy.smartdocflow.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SmartDocFlowCliApplicationTest {
    @Test
    void printsUsageForInvalidArguments() {
        String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[0]));

        assertTrue(output.contains("Usage: smartdoc-flow <parse|profile> --input <file> [--format markdown|json]"));
    }

    @Test
    void printsMarkdownForSampleTextFile() {
        String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[] {
            "parse",
            "--input",
            "../sample.txt"
        }));

        assertTrue(output.contains("# sample.txt"));
        assertTrue(output.contains("SmartDoc-Flow skeleton extracted content for sample.txt"));
    }

    @Test
    void printsJsonWhenJsonFormatRequested() {
        String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[] {
            "parse",
            "--input",
            "../sample.txt",
            "--format",
            "json"
        }));

        assertTrue(output.contains("\"fileName\":\"sample.txt\""));
        assertTrue(output.contains("\"blocks\""));
    }

    @Test
    void printsProfileForSampleTextFile() {
        String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[] {
            "profile",
            "--input",
            "../sample.txt"
        }));

        assertTrue(output.contains("sourceType: UNKNOWN"));
        assertTrue(output.contains("scanned: false"));
        assertTrue(output.contains("multiColumn: false"));
        assertTrue(output.contains("tableHeavy: false"));
        assertTrue(output.contains("imageHeavy: false"));
    }

    @Test
    void writesOutputToFile() throws Exception {
        Path tempFile = Files.createTempFile("smartdoc-output", ".txt");
        try {
            final Path file = tempFile;
            String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[] {
                "parse",
                "--input",
                "../sample.txt",
                "--output",
                file.toString()
            }));

            assertTrue(output.contains("Output written to:"));
            assertTrue(Files.readString(file).contains("# sample.txt"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void printsDiagnosticsWhenRequested() {
        String output = captureStdout(() -> SmartDocFlowCliApplication.main(new String[] {
            "parse",
            "--input",
            "../sample.txt",
            "--diagnostics"
        }));

        assertTrue(output.contains("PIPELINE.sourceType:"));
        assertTrue(output.contains("PIPELINE.multiColumn:"));
        assertTrue(output.contains("EXTRACT.started:"));
        assertTrue(output.contains("EXTRACT.durationMs:"));
        assertTrue(output.contains("EXTRACT.nodeDelta:"));
        assertTrue(output.contains("EXTRACT.beforeNodes:"));
        assertTrue(output.contains("POST.afterNodes:"));
    }

    @Test
    void printsErrorForMissingInputFile() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = captureStderr(error, () -> SmartDocFlowCliApplication.run(new String[] {
            "parse",
            "--input",
            "missing-file.txt"
        }));

        assertTrue(error.toString(StandardCharsets.UTF_8).contains("Error: 输入文件不存在:"));
        assertTrue(exitCode == 1);
    }

    @Test
    void printsErrorForDirectoryInput() throws Exception {
        Path directory = Files.createTempDirectory("smartdoc-cli-dir-");
        try {
            ByteArrayOutputStream error = new ByteArrayOutputStream();
            int exitCode = captureStderr(error, () -> SmartDocFlowCliApplication.run(new String[] {
                "parse",
                "--input",
                directory.toString()
            }));

            assertTrue(error.toString(StandardCharsets.UTF_8).contains("Error: 输入路径不是文件:"));
            assertTrue(exitCode == 1);
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    private String captureStdout(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private int captureStderr(ByteArrayOutputStream outputStream, java.util.concurrent.Callable<Integer> action) {
        PrintStream originalErr = System.err;
        try (PrintStream capture = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            System.setErr(originalErr);
        }
    }
}
