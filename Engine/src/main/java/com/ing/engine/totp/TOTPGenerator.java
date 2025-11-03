package com.ing.engine.totp;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.engine.support.methodInf.InputType;
import com.ing.util.encryption.Encryption;
import com.ing.engine.execution.exception.ActionException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

public class TOTPGenerator extends General {
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] BASE32_DECODE_TABLE;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static String totp;

    public TOTPGenerator(CommandControl cc) {
        super(cc);
    }

    static {
        BASE32_DECODE_TABLE = new int[256];
        Arrays.fill(BASE32_DECODE_TABLE, -1);
        for (int i = 0; i < BASE32_ALPHABET.length(); i++) {
            BASE32_DECODE_TABLE[BASE32_ALPHABET.charAt(i)] = i;
        }
    }

    public static byte[] decode(String input) {
        input = input.replaceAll("=", "");
        byte[] output = new byte[input.length() * 5 / 8];
        int outputIndex = 0;
        int buffer = 0;
        int bitsLeft = 0;

        for (char c : input.toCharArray()) {
            int value = BASE32_DECODE_TABLE[c];
            if (value < 0) continue;

            buffer <<= 5;
            buffer |= value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                output[outputIndex++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return output;
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void FillPasswordFromEnv() {
        try {

            Map<String, String> env = EnvLoader.loadEnv(".env");
            String secret = env.get(Data);

            Locator.clear();
            Locator.fill(secret);
            Report.updateTestLog(Action, "Entered Password ' ********** ' on '"
                    + "[" + ObjectName + "]" + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Generate and enter TOTP into field [&lt;Object&gt;]", input = InputType.NO)
    public void generateAndFillTOTP() {
        try {

            String secret = System.getenv("totpSecret");

            if (secret == null || secret.isEmpty()) {
                Map<String, String> env = EnvLoader.loadEnv(".env");
                secret = env.get("totpSecret");

                if ("true".equalsIgnoreCase(env.get("useAzureKeyVault"))) {
                    SecretClient secretClient = new SecretClientBuilder()
                            .vaultUrl(env.get("azureKeyVaultUrl"))
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildClient();
                    secret = secretClient.getSecret(env.get("azureSecretName")).getValue();
                }
            }

            String totp = generateTOTP(secret);
            Locator.clear();
            Locator.fill(totp);

            Report.updateTestLog(Action, "Entered TOTP [" + totp + "] into field [" + ObjectName + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Error generating or entering TOTP: " + ex.getMessage(), Status.FAIL);
            throw new ActionException(ex);
        }
    }

    public static String generateTOTP(String secret) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        byte[] key = decode(secret);
        byte[] timeBytes = longToBytes(timeIndex);
        byte[] hmac = hmacSHA1(key, timeBytes);
        return truncate(hmac);
    }

    private static byte[] hmacSHA1(byte[] key, byte[] data) {
        try{
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);

        } catch (Exception e) {
            throw new RuntimeException("Error in HMAC SHA1", e);
        }
    }

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[7 - i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    private static String truncate(byte[] hmac) {
        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24) |
                ((hmac[offset + 1] & 0xFF) << 16) |
                ((hmac[offset + 2] & 0xFF) << 8) |
                (hmac[offset + 3] & 0xFF);
        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format("%06d", otp);
    }

}