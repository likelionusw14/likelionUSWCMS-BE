package com.likelion.cms.domain.auth.service;

import java.security.SecureRandom;
import java.util.Base64;

final class OpaqueTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private OpaqueTokenGenerator() {
    }

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
