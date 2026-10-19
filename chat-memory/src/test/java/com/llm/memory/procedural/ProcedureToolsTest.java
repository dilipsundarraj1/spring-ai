package com.llm.memory.procedural;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcedureToolsTest {

    private final ProcedureTools tools = new ProcedureTools(new ProcedureLibrary());
    private final List<String> loaded = new ArrayList<>();
    private final ToolContext toolContext = new ToolContext(Map.of(ProcedureTools.LOADED, loaded));

    ProcedureToolsTest() throws IOException {
    }

    @Test
    void loadingAProcedureReturnsItsStepsAndRecordsIt() {
        var steps = tools.loadProcedure("oil-change", toolContext);

        assertThat(steps).contains("conventional $49, synthetic $89");
        assertThat(loaded).containsExactly("oil-change");
    }

    @Test
    void anUnknownNameIsToldSoAndNotRecorded() {
        var result = tools.loadProcedure("tire-rotation", toolContext);

        assertThat(result).isEqualTo("There is no procedure named 'tire-rotation'. Answer without one.");
        assertThat(loaded).isEmpty();
    }
}
