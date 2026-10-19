package com.llm.memory.procedural;

import com.llm.dto.Procedure;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Procedural memory: the dealership's procedures, one Markdown file each in {@code resources/procedures}.
 * <p>
 * They are written by people and read-only here - loaded once at startup, never changed by the application.
 * Each file starts with a small header:
 *
 * <pre>
 * ---
 * name: brake-complaint
 * description: The person reports a brake problem ...
 * ---
 * # Brake complaint
 * 1. ...
 * </pre>
 */
@Component
public class ProcedureLibrary {

    static final String LOCATION = "classpath:procedures/*.md";

    private final Map<String, Loaded> procedures = new LinkedHashMap<>();

    private record Loaded(Procedure procedure, String steps) {
    }

    public ProcedureLibrary() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        Arrays.stream(files)
                .map(ProcedureLibrary::parse)
                .sorted(Comparator.comparing(loaded -> loaded.procedure().name()))
                .forEach(loaded -> procedures.put(loaded.procedure().name(), loaded));
    }

    /** The index: every procedure's name and description, without the steps. */
    public List<Procedure> list() {
        return procedures.values().stream().map(Loaded::procedure).toList();
    }

    /** The full steps and rules of one procedure. */
    public Optional<String> steps(String name) {
        return Optional.ofNullable(procedures.get(name)).map(Loaded::steps);
    }

    private static Loaded parse(Resource file) {
        String text;
        try {
            text = file.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // "---\n<header>\n---\n<body>"
        String[] parts = text.split("(?m)^---\\s*$", 3);
        if (parts.length < 3) {
            throw new IllegalStateException("No --- header in " + file.getFilename());
        }
        Map<String, String> header = new LinkedHashMap<>();
        parts[1].lines()
                .filter(line -> line.contains(":"))
                .forEach(line -> header.put(line.substring(0, line.indexOf(':')).strip(),
                        line.substring(line.indexOf(':') + 1).strip()));
        return new Loaded(new Procedure(header.get("name"), header.get("description")), parts[2].strip());
    }
}
