package com.ing.datalib.or.mobile;

/**
 * Identifies the target mobile platform for a {@link MobileORObject}'s
 * properties view. INGenious maintains a separate set of locator attributes
 * for each platform; the appropriate set is picked up at execution time
 * based on the Appium driver in use.
 */
public enum MobilePlatform {
    ANDROID,
    IOS
}
