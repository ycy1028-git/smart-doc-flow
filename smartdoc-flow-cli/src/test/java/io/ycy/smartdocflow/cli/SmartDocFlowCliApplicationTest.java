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
}
