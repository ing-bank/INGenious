package com.ing.ide.main.mainui.components.aichat.auth;

import com.ing.ide.settings.AppSettings;
import com.ing.ide.settings.AppSettings.APP_SETTINGS;

/**
 * Persists GitHub Models credentials and preferences via {@link AppSettings},
 * encrypting the access token at rest with {@link SecureTokenStore}.
 */
public final class AICredentials {
    /**
     * Default public GitHub OAuth App client id shipped with INGenious. A blank
     * value means no app has been registered yet; users can override it in
     * settings. Replace this with the real registered client id before release.
     */
    private static final String DEFAULT_CLIENT_ID = "";

    private final SecureTokenStore tokenStore = new SecureTokenStore();

    /** Returns the decrypted access token, or {@code null} if not signed in. */
    public String getToken() {
        String encrypted = AppSettings.get(APP_SETTINGS.AI_GITHUB_TOKEN.getKey());
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        return tokenStore.decrypt(encrypted);
    }

    /** Encrypts and persists the access token. */
    public void setToken(String token) {
        String encrypted = token == null ? "" : tokenStore.encrypt(token);
        AppSettings.set(APP_SETTINGS.AI_GITHUB_TOKEN.getKey(), encrypted == null ? "" : encrypted);
        AppSettings.store("AI token updated");
    }

    public boolean isSignedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /** Clears the stored token and login, used by sign-out. */
    public void clear() {
        AppSettings.set(APP_SETTINGS.AI_GITHUB_TOKEN.getKey(), "");
        AppSettings.set(APP_SETTINGS.AI_GITHUB_LOGIN.getKey(), "");
        AppSettings.store("AI sign-out");
    }

    public String getLogin() {
        return AppSettings.get(APP_SETTINGS.AI_GITHUB_LOGIN.getKey());
    }

    public void setLogin(String login) {
        AppSettings.set(APP_SETTINGS.AI_GITHUB_LOGIN.getKey(), login == null ? "" : login);
        AppSettings.store("AI login updated");
    }

    public String getSelectedModel() {
        return AppSettings.get(APP_SETTINGS.AI_SELECTED_MODEL.getKey());
    }

    public void setSelectedModel(String model) {
        if (model != null && !model.isEmpty()) {
            AppSettings.set(APP_SETTINGS.AI_SELECTED_MODEL.getKey(), model);
            AppSettings.store("AI model updated");
        }
    }

    /** Returns the configured OAuth client id, falling back to the shipped default. */
    public String getClientId() {
        String configured = AppSettings.get(APP_SETTINGS.AI_GITHUB_CLIENT_ID.getKey());
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return DEFAULT_CLIENT_ID;
    }

    public void setClientId(String clientId) {
        AppSettings.set(
            APP_SETTINGS.AI_GITHUB_CLIENT_ID.getKey(),
            clientId == null ? "" : clientId
        );
        AppSettings.store("AI client id updated");
    }
}
