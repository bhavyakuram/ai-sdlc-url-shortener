package com.aisdlc.urlshortener.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Base62 helpers for Candidate A (insert-then-derive-then-update) short-code generation.
 * No third-party base62 library is used -- {@code dependency-audit}'s feasibility-report.md
 * Part 2.3 explicitly evaluated and rejected one as unjustified surface area for ~15 lines
 * of divide-and-remainder encoding.
 */
@Component
public class CodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    /** AC01: generated codes are exactly 7 base62 characters (62^7 =~ 3.5x10^12 space). */
    private static final int GENERATED_CODE_LENGTH = 7;

    /**
     * Encodes a DB-assigned identity id as a base62 string, left-padded with the alphabet's
     * zero character to {@link #GENERATED_CODE_LENGTH} for ids that fit in that width (the
     * overwhelming common case for this prototype). An id large enough to overflow 7 base62
     * digits still encodes correctly, just longer than 7 chars -- the schema's {@code
     * VARCHAR(32)} code column comfortably accommodates that pathological edge.
     */
    public String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative: " + id);
        }
        StringBuilder sb = new StringBuilder();
        long n = id;
        if (n == 0) {
            sb.append(ALPHABET.charAt(0));
        }
        while (n > 0) {
            sb.append(ALPHABET.charAt((int) (n % BASE)));
            n /= BASE;
        }
        while (sb.length() < GENERATED_CODE_LENGTH) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }

    /**
     * A short-lived, practically-collision-free placeholder used only for the brief
     * insert-before-id-is-known window Candidate A's first write needs (the {@code code}
     * column is {@code NOT NULL UNIQUE}, so it cannot be left blank between the insert and
     * the follow-up update). Exactly 32 chars -- fits the schema's {@code VARCHAR(32)} column
     * exactly, using the full width rather than a shorter placeholder to keep collision
     * probability negligible.
     */
    public String randomPlaceholder() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
