
package com.ing.engine.reporting.sync;

import com.ing.engine.reporting.util.TestInfo;
import java.io.File;
import java.util.List;
import org.json.simple.JSONObject;

public class Unknown implements Sync {

    @Override
    public boolean isConnected() {
        System.out.println("UNKNOWN CONNECTION!!");
        return false;
    }

    @Override
    public boolean updateResults(TestInfo TestCase, String status,
            List<File> attach) {
        System.out.println("UNKNOWN CONNECTION!!");
        return false;
    }

    @Override
    public void disConnect() {

    }

    @Override
    public String getModule() {
        return "UNKNOWN";
    }

    @Override
    public String createIssue(JSONObject issue, List<File> attach) {
        return null;
    }

}
