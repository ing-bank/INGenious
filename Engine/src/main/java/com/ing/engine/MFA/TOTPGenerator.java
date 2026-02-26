package com.ing.engine.MFA;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.execution.exception.ActionException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;


/**
 * Generates and fills Time-based One-Time Passwords (TOTP) in UI fields,
 * with support for secret retrieval from environment variables, a local
 * <code>.env</code> file, or Azure Key Vault.
 *
 * <p>
 * This class extends {@link General} from the INGenious framework and provides actions
 * that can be invoked in test automation steps. The TOTP generation follows the standard
 * RFC 6238 algorithm (HMAC-SHA1, 30-second time step, 6 digits), using a Base32-encoded secret.
 * </p>
 *
 * <h3>Secret Resolution Order</h3>
 * <ol>
 *   <li>System environment variable <code>totpSecret</code></li>
 *   <li><code>.env</code> file entry <code>totpSecret</code> via {@link EnvLoader}</li>
 *   <li>Azure Key Vault (when <code>useAzureKeyVault=true</code> in <code>.env</code>)</li>
 * </ol>
 *
 * <h3>Azure Key Vault</h3>
 * <ul>
 *   <li>Uses {@link DefaultAzureCredentialBuilder} to authenticate.</li>
 *   <li>Requires <code>azureKeyVaultUrl</code> and <code>azureSecretName</code> keys in <code>.env</code>.</li>
 * </ul>
 *
 * <p><b>Note:</b> Ensure that the TOTP secret is Base32-encoded. Invalid encoding will result in
 * incorrect HMAC computation and OTP values.</p>
 */
public class TOTPGenerator extends General {
    /** Base32 alphabet used to decode shared secrets. */
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** Lookup table for Base32 character decoding. */
    private static final int[] BASE32_DECODE_TABLE;

    /** Time step in seconds for TOTP (RFC 6238 default). */
    private static final int TIME_STEP_SECONDS = 30;

    /** Number of digits in the generated OTP. */
    private static final int DIGITS = 6;

    /** Last generated TOTP value (if you need to reference it externally). */
    private static String totp;


    /**
     * Constructs a new {@code TOTPGenerator}.
     *
     * @param cc the {@link CommandControl} context passed by the INGenious framework
     */
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

    /**
     * Decodes a Base32-encoded string into raw bytes.
     *
     * <p>The method ignores padding characters (<code>=</code>) and any invalid characters.</p>
     *
     * @param input the Base32-encoded secret string
     * @return the decoded byte array representing the shared secret key
     */
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

    /**
     * INGenious action: Fills a password from the local <code>.env</code> file into the target field.
     *
     * <p>
     * The key used to look up the secret in <code>.env</code> is taken from the
     * framework-provided {@code Data} variable (e.g., the test step's data input).
     * </p>
     *
     * <p><b>Side effects:</b> Clears the current field via {@code Locator.clear()} and fills it
     * via {@code Locator.fill(secret)}. Logs success/failure to {@code Report} and throws
     * {@link ActionException} on error.</p>
     *
     * @implNote This action reads <code>.env</code> from the current working directory.
     */
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

    /**
     * INGenious action: Generates a TOTP using the current time window and fills it into the target field.
     *
     * <p>
     * Secret resolution follows this order:
     * </p>
     * <ol>
     *   <li>System environment variable <code>totpSecret</code></li>
     *   <li><code>.env</code> file key <code>totpSecret</code></li>
     *   <li>Azure Key Vault (if <code>useAzureKeyVault=true</code> in <code>.env</code>)</li>
     * </ol>
     *
     * <p><b>Side effects:</b> Clears and fills the Playwright locator, logs via {@code Report}.
     * On failure, logs at {@link Level#SEVERE} and throws {@link ActionException}.</p>
     *
     * @implNote Azure Key Vault access requires valid environment configuration and credentials discoverable
     *           by {@link DefaultAzureCredentialBuilder}.
     */
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

    /**
     * Generates a TOTP for the given Base32-encoded secret using RFC 6238 defaults
     * (HMAC-SHA1, 30-second time step, 6 digits).
     *
     * @param secret the Base32-encoded shared secret
     * @return the 6-digit TOTP as a string (zero-padded)
     * @throws RuntimeException if HMAC computation fails
     */
    public static String generateTOTP(String secret) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        byte[] key = decode(secret);
        byte[] timeBytes = longToBytes(timeIndex);
        byte[] hmac = hmacSHA1(key, timeBytes);
        return truncate(hmac);
    }

    /**
     * Computes HMAC-SHA1 for the given key and message.
     *
     * @param key  the secret key (raw bytes)
     * @param data the message bytes to authenticate
     * @return the HMAC-SHA1 digest
     * @throws RuntimeException if the Mac instance cannot be initialized or executed
     */
    private static byte[] hmacSHA1(byte[] key, byte[] data) {
        try{
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);

        } catch (Exception e) {
            throw new RuntimeException("Error in HMAC SHA1", e);
        }
    }

    /**
     * Converts a 64-bit long value into an 8-byte big-endian array.
     *
     * @param value the long value to convert
     * @return an 8-byte array in big-endian order
     */
    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[7 - i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    /**
     * Applies dynamic truncation to an HMAC digest and formats the result
     * as a 6-digit OTP string.
     *
     * @param hmac the HMAC-SHA1 digest
     * @return the zero-padded 6-digit OTP
     */
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