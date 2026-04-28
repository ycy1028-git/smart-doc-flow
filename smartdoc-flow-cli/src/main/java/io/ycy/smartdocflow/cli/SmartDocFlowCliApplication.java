package io.ycy.smartdocflow.cli;

import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.sdk.SmartDocFlow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SmartDocFlowCliApplication {
    private SmartDocFlowCliApplication() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            if (args.length < 3 || !"--input".equals(args[1])) {
                printUsage();
                return 0;
            }

            Path input = Path.of(args[2]);
            Path outputFile = getOutputFile(args);
            SmartDocFlow smartDocFlow = new SmartDocFlow();

            if ("profile".equals(args[0])) {
                printProfile(smartDocFlow, input);
                return 0;
            }

            if (!"parse".equals(args[0])) {
                printUsage();
                return 0;
            }

            if (containsDiagnosticsFlag(args)) {
                printDiagnostics(smartDocFlow, input);
                return 0;
            }

            String output = containsJsonFlag(args)
                ? smartDocFlow.parseToJson(input)
                : smartDocFlow.parseToMarkdown(input);

            if (outputFile != null) {
                writeToFile(outputFile, output);
            } else {
                System.out.println(output);
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static Path getOutputFile(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                return Path.of(args[i + 1]);
            }
        }
        return null;
    }

    private static void writeToFile(Path file, String content) {
        try {
            Files.writeString(file, content);
            System.out.println("Output written to: " + file.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printProfile(SmartDocFlow smartDocFlow, Path input) {
        var profile = smartDocFlow.profile(input);
        System.out.println("sourceType: " + profile.sourceType());
        System.out.println("scanned: " + profile.scanned());
        System.out.println("multiColumn: " + profile.multiColumn());
        System.out.println("tableHeavy: " + profile.tableHeavy());
        System.out.println("imageHeavy: " + profile.imageHeavy());
    }

    private static void printDiagnostics(SmartDocFlow smartDocFlow, Path input) {
        List<Diagnostic> diagnostics = smartDocFlow.parseDiagnostics(input);
        for (Diagnostic diagnostic : diagnostics) {
            System.out.println(diagnostic.stage() + "." + diagnostic.key() + ": " + diagnostic.value());
        }
    }

    private static boolean containsJsonFlag(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--format".equals(args[i]) && i + 1 < args.length) {
                return "json".equalsIgnoreCase(args[i + 1]);
            }
        }
        return false;
    }

    private static boolean containsDiagnosticsFlag(String[] args) {
        for (String arg : args) {
            if ("--diagnostics".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("Usage: smartdoc-flow <parse|profile> --input <file> [--format markdown|json] [--output <file>] [--diagnostics]");
    }
}
