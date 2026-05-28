package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.datalib.settings.TestMgmtSettings;
import com.ing.engine.reporting.sync.Sync;
import com.ing.engine.reporting.sync.Unknown;
import com.ing.engine.reporting.sync.azure.AzureSync;
import com.ing.util.encryption.Encryption;

import org.mockito.MockedStatic;
import org.testng.annotations.Test;

/**
 * Tests for TMIntegration — getInstance factory, decrypt/encrypt logic,
 * isEnc check.
 */
public class TMIntegrationTest {

    // ---- getInstance ----

    @Test
    public void testGetInstanceNone_returnsNull() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);
        when(settings.getUpdateResultsToTM()).thenReturn("None");
        Sync result = TMIntegration.getInstance(settings);
        assertThat(result).isNull();
    }

    @Test
    public void testGetInstanceUnknownModule_returnsUnknown() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);
        when(settings.getUpdateResultsToTM()).thenReturn("SomeUnknownTool");
        // decryptValues iterates properties. Since TestMgmtSettings extends Properties,
        // mock stringPropertyNames to return empty set
        when(settings.stringPropertyNames()).thenReturn(java.util.Collections.emptySet());

        Sync result = TMIntegration.getInstance(settings);
        assertThat(result).isInstanceOf(Unknown.class);
    }

    // ---- isEnc ----

    @Test
    public void testIsEncTrue() {
        assertThat(TMIntegration.isEnc("TMENC:abc123")).isTrue();
    }

    @Test
    public void testIsEncFalse_noPrefix() {
        assertThat(TMIntegration.isEnc("plaintext")).isFalse();
    }

    @Test
    public void testIsEncFalse_null() {
        assertThat(TMIntegration.isEnc(null)).isFalse();
    }

    @Test
    public void testIsEncFalse_empty() {
        assertThat(TMIntegration.isEnc("")).isFalse();
    }

    // ---- decrypt ----

    @Test
    public void testDecryptNonEncrypted_returnsAsIs() {
        String plain = "plaintext";
        assertThat(TMIntegration.decrypt(plain)).isEqualTo(plain);
    }

    @Test
    public void testDecryptEncrypted_stripsPrefix() {
        // Mock the Encryption singleton to verify the stripped value is passed
        Encryption mockEnc = mock(Encryption.class);
        when(mockEnc.decrypt("abc123")).thenReturn("decrypted");

        try (MockedStatic<Encryption> encStatic = mockStatic(Encryption.class)) {
            encStatic.when(Encryption::getInstance).thenReturn(mockEnc);
            String result = TMIntegration.decrypt("TMENC:abc123");
            assertThat(result).isEqualTo("decrypted");
            verify(mockEnc).decrypt("abc123");
        }
    }

    // ---- encrypt ----

    @Test
    public void testEncryptAlreadyEncrypted_returnsAsIs() {
        String enc = "TMENC:existing";
        assertThat(TMIntegration.encrypt(enc)).isEqualTo(enc);
    }

    @Test
    public void testEncryptPlaintext_addsPrefix() {
        Encryption mockEnc = mock(Encryption.class);
        when(mockEnc.encrypt("secret")).thenReturn("encoded");

        try (MockedStatic<Encryption> encStatic = mockStatic(Encryption.class)) {
            encStatic.when(Encryption::getInstance).thenReturn(mockEnc);
            String result = TMIntegration.encrypt("secret");
            assertThat(result).isEqualTo("TMENC:encoded");
            verify(mockEnc).encrypt("secret");
        }
    }
}
