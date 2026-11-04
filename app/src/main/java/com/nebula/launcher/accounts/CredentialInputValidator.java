package com.nebula.launcher.accounts;

final class CredentialInputValidator {
    private CredentialInputValidator() {}
    static void validateAccountId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid account identifier");
        }
    }
    static void validateAccessToken(String value) {
        if (value == null || value.isEmpty() || value.length() > 16384 || hasWhitespaceOrControl(value)) {
            throw new IllegalArgumentException("Invalid access token");
        }
    }
    static void validateRefreshToken(String value) {
        if (value != null && (value.length() > 16384 || hasWhitespaceOrControl(value))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }
    private static boolean hasWhitespaceOrControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c)) return true;
        }
        return false;
    }
}
