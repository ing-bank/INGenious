package com.ing.datalib.or;

import com.ing.ingenious.api.contract.project.ObjectRepositoryApi;

/**
 * Delegates the {@link ObjectRepositoryApi} plugin contract to a specific
 * {@link ObjectRepository} instance.
 */
public final class ObjectRepositoryApiImpl implements ObjectRepositoryApi {
    private final ObjectRepository objectRepository;

    public ObjectRepositoryApiImpl(ObjectRepository objectRepository) {
        this.objectRepository = objectRepository;
    }

    @Override
    public boolean isObjectPresent(String pageName, String objectName) {
        return objectRepository.isObjectPresent(pageName, objectName);
    }

    @Override
    public boolean isMobileObjectPresent(String pageName, String objectName) {
        return objectRepository.isMobileObjectPresent(pageName, objectName);
    }

    @Override
    public boolean isStructuredDataObjectPresent(String pageName, String objectName) {
        return objectRepository.isStructuredDataObjectPresent(pageName, objectName);
    }
}
