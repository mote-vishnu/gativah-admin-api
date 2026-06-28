package com.gativah.admin.archunit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Source-level convention: use imports, never inline fully-qualified {@code java.*}
 * or {@code jakarta.*} names (e.g. {@code java.util.List<String>} in a signature).
 *
 * <p>This is a source scan rather than an ArchUnit rule on purpose: ArchUnit analyses
 * compiled bytecode, where the FQN-vs-import distinction has already been erased, so it
 * cannot tell whether a type was written fully qualified or imported. We therefore read
 * the .java files directly.
 */
class NoInlineFqnConventionTest {

    // A fully-qualified reference: java/jakarta, then lower-case package segments,
    // then a Capitalised type. Negative look-behind avoids matching mid-identifier.
    private static final Pattern INLINE_FQN =
            Pattern.compile("(?<![\\w.$])(java|jakarta)\\.[a-z][\\w.]*\\.[A-Z]\\w+");

    private static final List<String> SOURCE_ROOTS = List.of("src/main/java", "src/test/java");

    @Test
    void no_inline_fully_qualified_java_or_jakarta_names() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String root : SOURCE_ROOTS) {
            Path dir = Path.of(root);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            for (Path file : javaFiles(dir)) {
                if (file.getFileName().toString().equals("NoInlineFqnConventionTest.java")) {
                    continue; // this file names the forbidden packages in its regex/text
                }
                scan(file, violations);
            }
        }
        assertThat(violations)
                .as("Use imports, not inline fully-qualified java/jakarta names:\n" + String.join("\n", violations))
                .isEmpty();
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static void scan(Path file, List<String> violations) throws IOException {
        List<String> lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).strip();
            if (trimmed.startsWith("import ") || trimmed.startsWith("package ")
                    || trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }
            if (INLINE_FQN.matcher(lines.get(i)).find()) {
                violations.add(file + ":" + (i + 1) + "  ->  " + trimmed);
            }
        }
    }
}
