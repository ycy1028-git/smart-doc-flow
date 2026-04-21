package io.github.smartdocflow.cli;

import io.github.smartdocflow.sdk.SmartDocFlow;
import java.nio.file.Path;

public final class SmartDocFlowCliApplication {
    private SmartDocFlowCliApplication() {
    }

    public static void main(String[] args) {
        if (args.length < 3 || !"parse".equals(args[0]) || !"--input".equals(args[1])) {
            printUsage();
            return;
        }

        Path input = Path.of(args[2]);
        SmartDocFlow smartDocFlow = new SmartDocFlow();

        String output = containsJsonFlag(args)
            ? smartDocFlow.parseToJson(input)
            : smartDocFlow.parseToMarkdown(input);

        System.out.println(output);
    }

    private static boolean containsJsonFlag(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--format".equals(args[i]) && i + 1 < args.length) {
                return "json".equalsIgnoreCase(args[i + 1]);
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("Usage: smartdoc-flow parse --input <file> [--format markdown|json]");
    }
}
