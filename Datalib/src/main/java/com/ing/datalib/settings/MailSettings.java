
package com.ing.datalib.settings;

/**
 *
 * 
 */
public class MailSettings extends AbstractPropSettings {

    public MailSettings(String location) {
        super(location, "MailSettings");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("username", "domain\\username or username");
        put("password", "");
        put("from.mail", "");
        put("to.mail", "");
        put("msg.subject", "Execution Report - {{component}} - {{status}}");
        put("msg.Body", "");
        put("attach.reports", "true");
        put("attach.standaloneHtml", "false");
        put("attach.screenshots", "false");
        put("attach.console", "false");
        put("mail.smtp.host", "");
        put("mail.smtp.port", "");
        put("mail.smtp.ssl.trust", "Same as mail.smtp.host");
        put("mail.smtp.starttls.enable", "true");
        put("mail.smtp.starttls.required", "true");
        put("mail.smtp.auth", "true");
        put("mail.smtp.connectiontimeout", "10000");
        put("mail.debug", "false");
    }

}
