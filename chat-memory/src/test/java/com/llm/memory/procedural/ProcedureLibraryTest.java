package com.llm.memory.procedural;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ProcedureLibraryTest {

    private final ProcedureLibrary library = new ProcedureLibrary();

    ProcedureLibraryTest() throws IOException {
    }

    @Test
    void everyProcedureFileIsInTheIndex() {
        assertThat(library.list())
                .extracting(procedure -> procedure.name())
                .containsExactly("brake-complaint", "oil-change", "warning-light");
        assertThat(library.list()).allSatisfy(procedure -> assertThat(procedure.description()).isNotBlank());
    }

    @Test
    void theStepsAreTheFileWithoutItsHeader() {
        assertThat(library.steps("brake-complaint")).hasValueSatisfying(steps -> assertThat(steps)
                .startsWith("# Brake complaint")
                .contains("Never quote a repair price before the inspection.")
                .doesNotContain("description:"));
    }

    @Test
    void everyProcedureAsksForApprovalBeforeBooking() {
        assertThat(library.list()).allSatisfy(procedure -> assertThat(library.steps(procedure.name()))
                .hasValueSatisfying(steps -> assertThat(steps).contains("approve them with a yes or no")));
    }

    @Test
    void anUnknownProcedureHasNoSteps() {
        assertThat(library.steps("tire-rotation")).isEmpty();
    }
}
