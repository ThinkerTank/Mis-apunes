package co.edu.icesi.sgmms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks two technical rules of the statement directly on the source code, so a violation fails
 * the build instead of being found during grading.
 */
class ProjectRulesTest {

    private static final Path SOURCES = Paths.get("src", "main", "java");

    /** RT-01: no Java collection may replace the project's own structures. */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "java\\.util\\.(ArrayList|LinkedList|HashMap|TreeMap|HashSet|TreeSet|LinkedHashMap|LinkedHashSet"
                    + "|PriorityQueue|Stack|ArrayDeque|Vector|Hashtable|List|Map|Set|Collection|Deque|Queue)\\b"
                    + "|java\\.util\\.concurrent\\.\\w*(Map|Queue|List|Set|Deque)\\b");

    private static final Pattern PUBLIC_METHOD = Pattern.compile(
            "^\\s+public\\s+(static\\s+)?(final\\s+)?(synchronized\\s+)?[\\w<>\\[\\],.? ]+\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);

    @Test
    void noJavaCollectionReplacesTheOwnStructures() throws IOException {
        StringBuilder violations = new StringBuilder();
        for (Path file : javaFiles()) {
            String code = read(file);
            Matcher matcher = FORBIDDEN.matcher(code);
            while (matcher.find()) {
                violations.append(SOURCES.relativize(file)).append(": ").append(matcher.group()).append('\n');
            }
        }
        assertTrue(violations.length() == 0, "Forbidden Java collections found:\n" + violations);
    }

    @Test
    void everyClassDocumentsTheComplexityOfAtLeastTwoPublicOperations() throws IOException {
        StringBuilder missing = new StringBuilder();
        for (Path file : javaFiles()) {
            String code = read(file);
            String name = file.getFileName().toString().replace(".java", "");
            boolean isClass = code.matches("(?s).*public\\s+(final\\s+|abstract\\s+)*class\\s+" + name + "\\b.*");
            boolean isException = name.endsWith("Exception");
            if (!isClass || isException) {
                continue;
            }
            int publicMethods = 0;
            Matcher matcher = PUBLIC_METHOD.matcher(code);
            while (matcher.find()) {
                if (!matcher.group(4).equals(name)) {
                    publicMethods++;
                }
            }
            int documented = count(code, "Complexity —");
            if (publicMethods >= 2 && documented < 2) {
                missing.append(SOURCES.relativize(file)).append(" (").append(documented).append(" of ")
                        .append(publicMethods).append(" public operations documented)\n");
            }
        }
        assertTrue(missing.length() == 0, "Classes without complexity Javadoc on two public operations:\n" + missing);
    }

    /** Test code may use Java collections; the rule only applies to src/main. */
    private static java.util.List<Path> javaFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            return walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(java.util.stream.Collectors.toList());
        }
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static int count(String text, String fragment) {
        int count = 0;
        for (int i = text.indexOf(fragment); i >= 0; i = text.indexOf(fragment, i + 1)) {
            count++;
        }
        return count;
    }
}
