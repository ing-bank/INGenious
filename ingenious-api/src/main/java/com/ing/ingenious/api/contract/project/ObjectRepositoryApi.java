package com.ing.ingenious.api.contract.project;

/**
 * Contract for a project's object repository, backed by the framework's
 * {@code ObjectRepository}. Exposes only existence checks; the remaining
 * (design-time authoring) operations are intentionally not exposed to
 * plugins.
 */
public interface ObjectRepositoryApi {

    /**
     * @param pageName the web page name
     * @param objectName the object name
     * @return whether the web object exists on the given page
     */
    boolean isObjectPresent(String pageName, String objectName);

    /**
     * @param pageName the mobile page name
     * @param objectName the object name
     * @return whether the mobile object exists on the given page
     */
    boolean isMobileObjectPresent(String pageName, String objectName);

    /**
     * @param pageName the structured-data page name
     * @param objectName the object name
     * @return whether the structured-data object exists on the given page
     */
    boolean isStructuredDataObjectPresent(String pageName, String objectName);
}
