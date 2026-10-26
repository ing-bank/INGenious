package com.ing.ingenious.api.contract.project;

import java.util.List;

/**
 * Contract for a project's metadata, backed by the framework's
 * {@code ProjectInfo}. Exposes only the custom data item names; the nested
 * attribute/tag/meta structure is intentionally not exposed to plugins.
 */
public interface ProjectInfoApi {

    /** @return the names of the project's custom data items */
    List<String> getDataItemNames();
}
