package com.aisdlc.urlshortener.service;

import org.springframework.stereotype.Component;

/**
 * Base62-encodes a persisted id into a short code. Per
 * step3/technical-design.md Primitive Selection: codes are derived
 * from a DB-guaranteed-unique id, not generated speculatively and
 * checked for uniqueness — this is what makes AC-9 (collision-safety)
 * hold without any retry loop on the generated-code path.
 */
@Component
public class CodeGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }
}
