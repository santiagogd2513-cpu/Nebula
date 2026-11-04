package com.nebula.launcher.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts account tokens at rest with an Android Keystore AES-GCM key. */
public final class SecureTokenStore {
    private static final String PREFS = "nebula_secure_tokens";
    private static final String KEY_ALIAS = "nebula.account.tokens.v1";
    private static final int TAG_BITS = 128;
    private final SharedPreferences preferences;

    public SecureTokenStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void save(String accountId, String accessToken, String refreshToken) throws GeneralSecurityException, IOException {
        if (accountId == null || accountId.trim().isEmpty()) throw new IllegalArgumentException("accountId is required");
        String payload = (accessToken == null ? "" : accessToken) + "\n" + (refreshToken == null ? "" : refreshToken);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String packed = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        if (!preferences.edit().putString(accountId, packed).commit()) throw new IOException("Could not persist encrypted account credentials");
    }

    public synchronized String[] read(String accountId) throws GeneralSecurityException, IOException {
        String packed = preferences.getString(accountId, null);
        if (packed == null) return null;
        String[] parts = packed.split(":", 2);
        if (parts.length != 2) throw new IOException("Stored credential format is invalid");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(TAG_BITS, iv));
        String payload = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        int separator = payload.indexOf('\n');
        if (separator < 0) throw new IOException("Stored credential payload is invalid");
        return new String[]{payload.substring(0, separator), payload.substring(separator + 1)};
    }

    public synchronized void delete(String accountId) throws IOException {
        if (!preferences.edit().remove(accountId).commit()) throw new IOException("Could not remove account credentials");
    }

    // KeyStore.load() declares IOException in addition to security exceptions.
    // save() and read() already expose IOException to their callers.
    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
