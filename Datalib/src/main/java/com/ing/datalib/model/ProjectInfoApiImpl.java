package com.ing.datalib.model;

import com.ing.ingenious.api.contract.project.ProjectInfoApi;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delegates the {@link ProjectInfoApi} plugin contract to a specific
 * {@link ProjectInfo} instance.
 */
public final class ProjectInfoApiImpl implements ProjectInfoApi {
    private final ProjectInfo projectInfo;

    public ProjectInfoApiImpl(ProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    @Override
    public List<String> getDataItemNames() {
        return projectInfo.getData().stream().map(DataItem::getName).collect(Collectors.toList());
    }
}
