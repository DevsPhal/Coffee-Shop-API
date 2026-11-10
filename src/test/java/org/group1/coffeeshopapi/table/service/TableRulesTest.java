package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.service.TableRules.Verdict;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// The admin app mirrors this matrix in src/lib/tableTransitions.ts (and its test); keep them in step.
class TableRulesTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "AVAILABLE, AVAILABLE, ALLOWED",
            "AVAILABLE, OCCUPIED,  ALLOWED",
            "AVAILABLE, RESERVED,  NEVER",
            "AVAILABLE, CLEANING,  ALLOWED",
            "OCCUPIED,  AVAILABLE, ADMIN_OVERRIDE",
            "OCCUPIED,  OCCUPIED,  ALLOWED",
            "OCCUPIED,  RESERVED,  NEVER",
            "OCCUPIED,  CLEANING,  ALLOWED",
            "RESERVED,  AVAILABLE, ALLOWED",
            "RESERVED,  OCCUPIED,  ADMIN_OVERRIDE",
            "RESERVED,  RESERVED,  ALLOWED",
            "RESERVED,  CLEANING,  ALLOWED",
            "CLEANING,  AVAILABLE, ALLOWED",
            "CLEANING,  OCCUPIED,  ALLOWED",
            "CLEANING,  RESERVED,  NEVER",
            "CLEANING,  CLEANING,  ALLOWED",
    })
    void manualStatusChanges(TableStatus from, TableStatus to, Verdict expected) {
        TableRules.Decision decision = TableRules.manualChange(from, to, "A1");

        assertThat(decision.verdict()).isEqualTo(expected);
        if (expected != Verdict.ALLOWED) {
            assertThat(decision.message()).contains("A1");
        }
    }
}
