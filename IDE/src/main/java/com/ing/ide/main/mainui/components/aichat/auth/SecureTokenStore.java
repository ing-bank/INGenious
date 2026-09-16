package com.ing.ide.main.mainui.components.aichat.auth;

import com.ing.engine.constants.AppResourcePath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES/GCM encryption helper for persisting the GitHub Models access token.
 *
 * <p>Unlike {@code com.ing.util.encryption.Encryption}, this store generates a
 * fresh random IV per encryption and prepends it to the ciphertext, so values
 * encrypted in one session can be decrypted after a restart. The symmetric key
 * is derived (PBKDF2) from a locally generated key file kept under the
 * {@code Configuration} directory with owner-only permissions where the
 * filesystem supports it.</p>
 */
public final class SecureTokenStore {
    private static final Logger LOG = Logger.getLogger(SecureTokenStore.class.getName());

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    private static final Path KEY_FILE = Path.of(
        AppResourcePath.getConfigurationPath(),
        ".aichat.key"
    );

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public SecureTokenStore() {
        this.secretKey = deriveKey(loadOrCreateKeyMaterial());
    }

    /**
     * Encrypts the given plaintext, returning a Base64 string containing the IV
     * followed by the ciphertext. Returns {@code null} on failure.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to encrypt token", ex);
            return null;
        }
    }

    /**
     * Decrypts a value previously produced by {@link #encrypt(String)}. Returns
     * {@code null} on failure (e.g. corrupted data or a rotated key).
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            if (combined.length <= IV_BYTES) {
                return null;
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] cipherText = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to decrypt token", ex);
            return null;
        }
    }

    private SecretKey deriveKey(byte[] keyMaterial) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            String pass = Base64.getEncoder().encodeToString(keyMaterial);
            KeySpec spec = new PBEKeySpec(
                pass.toCharArray(),
                keyMaterial,
                PBKDF2_ITERATIONS,
                KEY_BITS
            );
            byte[] derived = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(derived, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive token encryption key", ex);
        }
    }

    private byte[] loadOrCreateKeyMaterial() {
        try {
            if (Files.exists(KEY_FILE)) {
                return Base64
                    .getDecoder()
                    .decode(
                        new String(Files.readAllBytes(KEY_FILE), StandardCharsets.UTF_8).trim()
                    );
            }
            byte[] material = new byte[32];
            new SecureRandom().nextBytes(material);
            Path parent = KEY_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(
                KEY_FILE,
                Base64.getEncoder().encodeToString(material).getBytes(StandardCharsets.UTF_8)
            );
            restrictPermissions(KEY_FILE);
            return material;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to load or create token key file", ex);
            // Fall back to an ephemeral key; tokens will not survive a restart
            // but the application remains functional within the session.
            byte[] fallback = new byte[32];
            new SecureRandom().nextBytes(fallback);
            return fallback;
        }
    }

    private void restrictPermissions(Path path) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            // Non-POSIX filesystem (e.g. Windows); rely on default ACLs.
        }
    }
}
