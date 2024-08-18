
package com.ing.engine.reporting.performance.metrics.pagespeed;

import com.ing.engine.reporting.performance.metrics.PageMetrics;
import com.ing.engine.support.DesktopApi;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * page speed metrics api
 *
 * 
 * 
 */
@SuppressWarnings("unchecked")
public class PageSpeed extends PageMetrics {

    int score = -1;
    JSONArray insights;
    String url;
    File har, exe;

    public PageSpeed(String url, File har, File exe) {
        this.url = url;
        this.exe = exe;
        this.har = har;
    }

    @Override
    public Object call() throws Exception {
        if (har.exists() && DesktopApi.getOs().isWindows()) {
            try {
                insights = new JSONArray();
                System.out.print("|");
                parseHar(har, exe);
                return get();
            } catch (Exception ex) {
                Logger.getLogger(PageSpeed.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        JSONObject data = new JSONObject();
        data.put("name", url);
        data.put("score", insights.size() > 0 && score >= 0 ? score / insights.size() : score);
        data.put("logs", insights);
        return data.toJSONString();
    }

    /**
     * convert har data from file to page speed metrics using the executable and
     * parse it to insights data(JSON)
     *
     * @param har
     * @param exe
     */
    private void parseHar(File har, File exe) throws Exception {

        List<String> command = new ArrayList<>();
        command.add(exe.getAbsolutePath());
        command.add(har.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p;
        p = pb.start();
        Thread.sleep(2000);
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        Pattern insightP = Pattern.compile("_(.*)_ \\(score=(0|[0-9][0-9]|100)\\)");
        Pattern descP = Pattern.compile("  (\\w+.*)");
        Pattern dataP = Pattern.compile("    \\* (\\w+.*)");
        Pattern includes = Pattern.compile("Combine external CSS|Combine external JavaScript|Leverage browser caching|Minimize DNS lookups");
        Insight<String, Object> in = null;
        score = -1;
        while ((line = reader.readLine()) != null) {
            //parse each line
            Matcher m = insightP.matcher(line);
            if (m.matches()) {

                int sc = -1;
                if (includes.matcher(m.group(1)).matches()) {
                    sc = Integer.parseInt(m.group(2));
                    //score += sc;
                }
                //if line matched insight then create new insight
                in = new Insight<>(m.group(1), sc);
                insights.add(in);
            } else if (in != null) {
                // if line matches description add desc to the insight
                m = descP.matcher(line);
                if (m.matches()) {
                    in.desc(m.group(1));
                } else {
                    m = dataP.matcher(line);
                    if (m.matches()) {
                        in.addDescData(m.group(1));
                    }
                }
            }
        }
    }

    public JSONObject get() {
        JSONObject data = new JSONObject();
        data.put("name", url);
        //calculated score of the har
        data.put("score", insights.size() > 0 && score >= 0 ? score / insights.size() : score);
        data.put("logs", insights);
        return data;
    }

}
