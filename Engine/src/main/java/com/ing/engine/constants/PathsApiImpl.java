package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.AppPathsApi;
import com.ing.ingenious.api.contract.paths.PathsApi;
import com.ing.ingenious.api.contract.paths.ProjectPathsApi;
import com.ing.ingenious.api.contract.paths.ReportOutputPathsApi;
import com.ing.ingenious.api.contract.paths.ReportTemplatePathsApi;
import com.ing.ingenious.api.contract.paths.ResultsPathsApi;

/**
 * Delegates the {@link PathsApi} plugin contract to the {@link FilePath} and
 * {@link AppResourcePath} static methods via the per-concern delegates.
 */
public final class PathsApiImpl implements PathsApi {
    public static final PathsApiImpl INSTANCE = new PathsApiImpl();

    private PathsApiImpl() {}

    @Override
    public ProjectPathsApi project() {
        return ProjectPathsApiImpl.INSTANCE;
    }

    @Override
    public AppPathsApi app() {
        return AppPathsApiImpl.INSTANCE;
    }

    @Override
    public ResultsPathsApi results() {
        return ResultsPathsApiImpl.INSTANCE;
    }

    @Override
    public ReportTemplatePathsApi reportTemplates() {
        return ReportTemplatePathsApiImpl.INSTANCE;
    }

    @Override
    public ReportOutputPathsApi reportOutputs() {
        return ReportOutputPathsApiImpl.INSTANCE;
    }
}
