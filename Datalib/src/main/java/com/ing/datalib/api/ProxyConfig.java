package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Configuration for an HTTP proxy attached to an API request.
 * <p>
 * When {@link #enabled} is {@code true}, requests are routed through the proxy
 * identified by {@link #host} and {@link #port}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled;
    private String host;
    private String port;

    public ProxyConfig() {
        this.enabled = false;
        this.host = "";
        this.port = "";
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Returns true if this config has enough information to route a request.
     */
    public boolean hasValidConfig() {
        return (
            enabled &&
            host != null &&
            !host.trim().isEmpty() &&
            port != null &&
            !port.trim().isEmpty()
        );
    }

    /**
     * Creates a deep copy of this config.
     */
    public ProxyConfig copy() {
        ProxyConfig copy = new ProxyConfig();
        copy.setEnabled(this.enabled);
        copy.setHost(this.host);
        copy.setPort(this.port);
        return copy;
    }

    @Override
    public String toString() {
        if (!enabled) return "Proxy: disabled";
        return "Proxy: " + (host != null ? host : "(none)") + ":" + (port != null ? port : "");
    }
}
