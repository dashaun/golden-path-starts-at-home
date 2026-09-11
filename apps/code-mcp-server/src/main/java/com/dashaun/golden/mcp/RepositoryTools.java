package com.dashaun.golden.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The three things an agent is allowed to do with the repository.
 *
 * Every path an agent supplies is resolved against a fixed root and rejected
 * if it lands outside it, so a traversal attempt fails before any file is
 * opened. There is no write tool and no shell tool on purpose.
 */
@Component
public class RepositoryTools {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTools.class);
    private static final List<String> IGNORED_DIRECTORIES =
            List.of(".git", "target", "build", "node_modules", ".idea", "bindings");

    private final WorkspaceProperties properties;
    private final Path root;

    RepositoryTools(WorkspaceProperties properties) {
        this.properties = properties;
        this.root = Paths.get(properties.workspaceRoot()).toAbsolutePath().normalize();
        log.info("Repository tools rooted at {}", this.root);
    }

    @Tool(description = "List the files and directories at a path inside the repository.")
    public List<String> listFiles(
            @ToolParam(description = "Repository-relative directory, for example 'apps/greeting-service/src'")
            String path) {
        Path target = resolve(path);
        if (!Files.isDirectory(target)) {
            return List.of("Not a directory: " + relative(target));
        }
        try (Stream<Path> entries = Files.list(target)) {
            return entries
                    .filter(entry -> !isIgnored(entry))
                    .sorted(Comparator.comparing(Path::toString))
                    .limit(properties.maxResults())
                    .map(entry -> Files.isDirectory(entry) ? relative(entry) + "/" : relative(entry))
                    .toList();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Tool(description = "Read one text file from the repository. Binary and oversized files are refused.")
    public String readFile(
            @ToolParam(description = "Repository-relative file, for example 'config-repo/greeting-service.yml'")
            String path) {
        Path target = resolve(path);
        if (!Files.isRegularFile(target)) {
            return "No such file: " + relative(target);
        }
        if (!hasAllowedExtension(target)) {
            return "Refused: this server only reads " + String.join(", ", properties.allowedExtensions());
        }
        try {
            long size = Files.size(target);
            if (size > properties.maxFileBytes()) {
                return "Refused: %s is %d bytes, over the %d byte limit"
                        .formatted(relative(target), size, properties.maxFileBytes());
            }
            return Files.readString(target, StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Tool(description = "Find files whose contents contain a literal string. Returns path and line number.")
    public List<String> searchRepository(
            @ToolParam(description = "Literal text to look for, case insensitive") String query) {
        if (query == null || query.isBlank()) {
            return List.of("Refused: empty query");
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(candidate -> !isIgnored(candidate))
                    .filter(this::hasAllowedExtension)
                    .forEach(candidate -> collectMatches(candidate, needle, hits));
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return hits.isEmpty() ? List.of("No matches for: " + query) : hits;
    }

    private void collectMatches(Path candidate, String needle, List<String> hits) {
        if (hits.size() >= properties.maxResults()) {
            return;
        }
        try {
            if (Files.size(candidate) > properties.maxFileBytes()) {
                return;
            }
            List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size() && hits.size() < properties.maxResults(); i++) {
                if (lines.get(i).toLowerCase(Locale.ROOT).contains(needle)) {
                    hits.add("%s:%d: %s".formatted(relative(candidate), i + 1, lines.get(i).strip()));
                }
            }
        }
        catch (IOException ignored) {
            // Unreadable files are simply not part of what the agent can see.
        }
    }

    /**
     * Resolves a caller-supplied path against the workspace root.
     *
     * A leading slash means the root of the repository, not the root of the
     * filesystem. That is what a caller means by "/" and refusing it only
     * teaches a model to ask again. Traversal is still caught: whatever the
     * path looks like, the normalized result has to sit under the root.
     */
    private Path resolve(String path) {
        String requested = (path == null || path.isBlank()) ? "." : path.trim();
        while (requested.startsWith("/")) {
            requested = requested.substring(1);
        }
        if (requested.isEmpty()) {
            requested = ".";
        }
        Path candidate = root.resolve(requested).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Refused: %s is outside the workspace".formatted(path));
        }
        return candidate;
    }

    private String relative(Path path) {
        return root.relativize(path).toString();
    }

    private boolean isIgnored(Path path) {
        for (Path segment : root.relativize(path)) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAllowedExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return properties.allowedExtensions().contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
