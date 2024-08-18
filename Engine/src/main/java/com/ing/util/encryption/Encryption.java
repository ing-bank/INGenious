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

    GCMParameterSpec gcmParameterSpec;

    static Encryption encrypt;

    SecretKeyFactory factory;

    KeySpec spec;

    SecretKey tmp;

    SecretKeySpec secretKey;

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
            SecureRandom random = new SecureRandom();
            byte[] iv = random.generateSeed(16);
            gcmParameterSpec = new GCMParameterSpec(16 * 8, iv);
            String passKey = initKey();
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(passKey.toCharArray(), passKey.getBytes(), 65536, 256);
            tmp = factory.generateSecret(spec);
            secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));

        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
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
                Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, "Key File not exist");
            }
        } catch (IOException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
