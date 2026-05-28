package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Configuration for SSL client certificates attached to an API request.
 * Supports PEM/PFX certificate formats.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Supported certificate/keystore formats.
     */
    public enum CertificateType {
        PEM,
        PFX
    }

    private boolean enabled;
    private CertificateType certificateType;
    private String caCertPath;         // CA / root certificate file path (PEM)
    private String clientCertPath;     // Client certificate file path (PEM .crt/.pem)
    private String clientKeyPath;      // Client private key file path (PEM .key)
    private String pfxPath;            // PFX/PKCS12 keystore file path
    private String passphrase;         // Passphrase for private key or PFX

    public CertificateConfig() {
        this.enabled = false;
        this.certificateType = CertificateType.PEM;
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public void setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }

    public void setClientCertPath(String clientCertPath) {
        this.clientCertPath = clientCertPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public void setClientKeyPath(String clientKeyPath) {
        this.clientKeyPath = clientKeyPath;
    }

    public String getPfxPath() {
        return pfxPath;
    }

    public void setPfxPath(String pfxPath) {
        this.pfxPath = pfxPath;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * Returns true if this config has enough information to build an SSL context.
     */
    public boolean hasValidConfig() {
        if (!enabled) return false;
        if (certificateType == CertificateType.PFX) {
            return pfxPath != null && !pfxPath.trim().isEmpty();
        }
        // PEM: need at least a client cert or CA cert
        return (clientCertPath != null && !clientCertPath.trim().isEmpty())
                || (caCertPath != null && !caCertPath.trim().isEmpty());
    }

    /**
     * Creates a deep copy of this config.
     */
    public CertificateConfig copy() {
        CertificateConfig copy = new CertificateConfig();
        copy.setEnabled(this.enabled);
        copy.setCertificateType(this.certificateType);
        copy.setCaCertPath(this.caCertPath);
        copy.setClientCertPath(this.clientCertPath);
        copy.setClientKeyPath(this.clientKeyPath);
        copy.setPfxPath(this.pfxPath);
        copy.setPassphrase(this.passphrase);
        return copy;
    }

    @Override
    public String toString() {
        if (!enabled) return "Certificates: disabled";
        if (certificateType == CertificateType.PFX) {
            return "PFX: " + (pfxPath != null ? pfxPath : "(none)");
        }
        return "PEM: " + (clientCertPath != null ? clientCertPath : "(none)");
    }
}
