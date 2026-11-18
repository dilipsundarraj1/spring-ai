package com.llm.memory.procedural;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The read side of procedural memory: the model sees only the index of procedures, and calls this tool to load the
 * full steps of the one that matches the request.
 */
@Component
public class ProcedureTools {

    /** Key of the list, in the tool context, that collects the names of the procedures loaded during one call. */
    public static final String LOADED = "proceduresLoaded";

    private static final Logger log = LoggerFactory.getLogger(ProcedureTools.class);

    private final ProcedureLibrary library;

    public ProcedureTools(ProcedureLibrary library) {
        this.library = library;
    }

    @Tool(description = """
            Load the full steps and rules of one of the dealership's procedures, by its name from the PROCEDURES
            list. Call it before answering a request that matches a procedure, then follow what it returns.""")
    @SuppressWarnings("unchecked")
    public String loadProcedure(@ToolParam(description = "The procedure name, for example 'brake-complaint'") String name,
                                ToolContext toolContext) {
        log.info("loadProcedure - {}", name);
        return library.steps(name)
                .map(steps -> {
                    ((List<String>) toolContext.getContext().get(LOADED)).add(name);
                    return steps;
                })
                .orElse("There is no procedure named '" + name + "'. Answer without one.");
    }
}
