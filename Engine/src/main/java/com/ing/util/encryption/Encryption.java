package com.ing.util.encryption;

import com.ing.engine.constants.FilePath;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.FileUtils;

public class Encryption {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    /**
     * Legacy parameters: a process-wide random IV. Values produced by an older
     * build can only be read back inside the session that wrote them, which is
     * why the IV is now stored alongside the ciphertext. Kept solely so
     * same-session legacy values still decrypt.
     */
    GCMParameterSpec gcmParameterSpec;

    static Encryption encrypt;

    SecretKeyFactory factory;

    KeySpec spec;

    SecretKey tmp;

    SecretKeySpec secretKey;

    private final SecureRandom random = new SecureRandom();

    public static Encryption getInstance() {
        if (encrypt == null) {
            encrypt = new Encryption();
        }
        return encrypt;
    }

    private Encryption() {
        init();
    }

    private void init() {
        try {
            byte[] legacyIv = random.generateSeed(16);
            gcmParameterSpec = new GCMParameterSpec(16 * 8, legacyIv);
            String passKey = initKey();
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(passKey.toCharArray(), passKey.getBytes(), 65536, 256);
            tmp = factory.generateSecret(spec);
            secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Encrypts a value as {@code Base64(iv || ciphertext)}. Storing the IV with
     * the ciphertext is what lets the value be decrypted by a later run of the
     * IDE or by the engine in a separate JVM.
     */
    public String encrypt(String strToEncrypt) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes("UTF-8"));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String decrypt(String strToDecrypt) {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(strToDecrypt);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        String plain = decryptWithEmbeddedIv(payload);
        return plain != null ? plain : decryptLegacy(payload);
    }

    private String decryptWithEmbeddedIv(byte[] payload) {
        if (payload.length <= IV_LENGTH) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(
                cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH),
                "UTF-8"
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String decryptLegacy(byte[] payload) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            return new String(cipher.doFinal(payload), "UTF-8");
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String initKey() {
        try {
            File encFile = new File(FilePath.getEncFile());
            if (encFile.exists()) {
                return FileUtils.readFileToString(encFile, "UTF-8");
            } else {
                Logger
                    .getLogger(Encryption.class.getName())
                    .log(Level.SEVERE, "Key File not exist");
            }
        } catch (IOException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
