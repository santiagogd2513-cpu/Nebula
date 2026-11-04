package com.nebula.launcher.accounts;

import org.junit.Test;
import static org.junit.Assert.*;

public class CredentialInputValidatorTest {
    @Test public void acceptsProviderAccountIdentifiers() {
        CredentialInputValidator.validateAccountId("local:1234-abcd");
        CredentialInputValidator.validateAccountId("microsoft.user_1");
    }
    @Test public void rejectsUnsafeAccountIdentifiers() {
        assertInvalidId(null);
        assertInvalidId("");
        assertInvalidId("../token");
        assertInvalidId("id with spaces");
        assertInvalidId(new String(new char[129]).replace('\0', 'a'));
    }
    @Test public void validatesTokenBoundaries() {
        CredentialInputValidator.validateAccessToken("opaque.token-value_123");
        CredentialInputValidator.validateRefreshToken(null);
        CredentialInputValidator.validateRefreshToken("");
        assertInvalidAccess(null);
        assertInvalidAccess("");
        assertInvalidAccess("token with spaces");
        assertInvalidAccess("token\nforged");
        assertInvalidRefresh("refresh\rforged");
    }
    private void assertInvalidId(String value) {
        try { CredentialInputValidator.validateAccountId(value); fail("Expected invalid account id"); }
        catch (IllegalArgumentException expected) { }
    }
    private void assertInvalidAccess(String value) {
        try { CredentialInputValidator.validateAccessToken(value); fail("Expected invalid access token"); }
        catch (IllegalArgumentException expected) { }
    }
    private void assertInvalidRefresh(String value) {
        try { CredentialInputValidator.validateRefreshToken(value); fail("Expected invalid refresh token"); }
        catch (IllegalArgumentException expected) { }
    }
}
