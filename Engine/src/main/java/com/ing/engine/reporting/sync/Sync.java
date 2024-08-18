
package com.ing.engine.reporting.sync;

import com.ing.engine.reporting.util.TestInfo;
import java.io.File;
import java.util.List;
import org.json.simple.JSONObject;

public interface Sync {

    public String getModule();

    public boolean isConnected();

    public boolean updateResults(TestInfo TestCase, String status,
            List<File> attach);

    public String createIssue(JSONObject issue, List<File> attach);

    public void disConnect();
}
