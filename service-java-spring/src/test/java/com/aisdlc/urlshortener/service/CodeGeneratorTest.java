package com.aisdlc.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for Candidate A's base62 encoding helper (supports AC01's "7-char base62" shape). */
class CodeGeneratorTest {

    private final CodeGenerator codeGenerator = new CodeGenerator();

    @Test
    void encodesToExactlySevenCharsForSmallIds() {
        assertThat(codeGenerator.encode(0)).hasSize(7).matches("^[0-9A-Za-z]{7}$");
        assertThat(codeGenerator.encode(1)).hasSize(7);
        assertThat(codeGenerator.encode(61)).hasSize(7);
        assertThat(codeGenerator.encode(12345)).hasSize(7);
    }

    @Test
    void distinctIdsProduceDistinctCodes() {
        Set<String> codes = new HashSet<>();
        for (long id = 0; id < 5000; id++) {
            codes.add(codeGenerator.encode(id));
        }
        assertThat(codes).hasSize(5000);
    }

    @Test
    void rejectsNegativeIds() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> codeGenerator.encode(-1));
    }

    @Test
    void placeholderFitsTheVarchar32CodeColumn() {
        String placeholder = codeGenerator.randomPlaceholder();
        assertThat(placeholder).hasSize(32);
        assertThat(placeholder).matches("^[0-9a-f]{32}$");
    }

    @Test
    void placeholdersAreDistinct() {
        assertThat(codeGenerator.randomPlaceholder()).isNotEqualTo(codeGenerator.randomPlaceholder());
    }
}
