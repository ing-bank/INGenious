
package com.ing.ide.main.dashboard.server;

import com.ing.ide.main.dashboard.server.websocket.HarAdapter;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * 
 */
public class HarCompareHandler implements Handler {

    private static final Logger LOG = Logger.getLogger(HarCompareHandler.class.getName());

    static JSONObject conf;

    public HarCompareHandler() {
        try {
            if (conf == null) {
                init();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void onMessage(WebSocketAdapter client, String message) {
        try {
            onMessage((HarAdapter) client, (JSONObject) JSONValue.parse(message));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void onMessage(HarAdapter conn, JSONObject msg) {
        try {
            LOG.log(Level.INFO, "{0}", msg.get("req"));
            switch (msg.get("req") + "") {
                case "GET.ReportHistory":
                    processReportHistory(conn, msg);
                    break;
                case "GET.HarsData":
                    processHarsDataRequest(conn, msg);
                    break;
                case "GET.RefData":
                    processReferenceData(conn, msg);
                    break;
                case "GET.ReferenceList":
                    processReferenceList(conn, msg);
                    break;
                case "SAVE.HarRef":
                    saveHarRef(conn, msg);
                    break;
                case "DELETE.Har":
                    deleteHar(conn, msg);
                    break;
                case "CLR.SelectedHars":
                    conf.put("selectedHars", new JSONArray());
                    updateConfig();
                    break;
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    public static synchronized void processReportHistory(HarAdapter conn, JSONObject req) {
        req.put("dataset", getReportHistory());
        req.put("refList", getRefHars());
        req.put("config", conf);
        conn.send(toMessage(req, "SET.ReportHistory"));
    }

    public static synchronized void processReferenceList(HarAdapter conn, JSONObject req) {
        req.put("refHars", getRefHars());
        conn.send(toMessage(req, "SET.ReferenceList"));
    }

    public static synchronized void processReferenceData(HarAdapter conn, JSONObject req) {
        req.put("har", getRefData(req));
        conf.put("ref", req.get("name"));
        conn.send(toMessage(req, "SET.RefData"));
    }

    public static synchronized void processHarsDataRequest(HarAdapter conn, JSONObject req) {
        req.put("harsData", getHarsData(req));
        conn.send(toMessage(req, "SET.HarsData"));
    }

    private static String toMessage(Object data, String action) {
        JSONObject msg = new JSONObject();
        msg.put("DATA", data);
        msg.put("res", action);
        return msg.toString();
    }

    public static JSONArray getRefHars() {
        JSONArray refHarList = new JSONArray();
        try {
            File refhars = DashBoardData.refHars();
            if (!refhars.exists()) {
                refhars.mkdirs();
                return refHarList;
            }
            File[] list = refhars.listFiles((File pathname) -> pathname.getName().endsWith(".har.ref"));
            if (list != null) {
                for (File f : list) {
                    JSONObject har = new JSONObject();
                    har.put("name", f.getName().substring(0, f.getName().length() - 8));
                    har.put("fname", f.getName());
                    refHarList.add(har);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while reading ref. har file list", ex);
        }
        return refHarList;
    }

    public static JSONObject getRefData(JSONObject req) {
        try {
            String data = Tools.readFile(new File(DashBoardData.refHars(), req.get("name").toString() + ".har.ref"));
            return (JSONObject) JSONValue.parse(data);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while reading har ref file", ex);
        }
        return null;
    }

    static JSONArray getReportHistory() {
        JSONArray dataset = new JSONArray();
        try {
            File rHF = DashBoardData.hars();
            File[] list = rHF.listFiles(new FileFilter() {
                @Override
                public boolean accept(File report) {
                    if (report.isDirectory()) {
                        File[] l = report.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.getName().endsWith(".har");
                            }
                        });
                        return l != null && l.length > 0;
                    }
                    return false;
                }
            });
            if (list != null) {
                for (File f : list) {
                    JSONObject page = new JSONObject();
                    page.put("name", f.getName());
                    page.put("hars", getHars(f, f.getName()));
                    dataset.add(page);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while reading report history", ex);
        }

        return dataset;
    }

    static Object getHars(File p, String page) {
        JSONArray dataset = new JSONArray();
        try {
            File[] list = p.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".har");
                }
            });
            if (list != null) {
                for (File f : list) {
                    JSONObject har = new JSONObject();
                    har.put("name", f.getName().substring(0, f.getName().length() - 4));
                    har.put("loc", f.getName());
                    har.put("pageName", page);
                    dataset.add(har);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while reading report history", ex);
        }

        return dataset;
    }

    static JSONArray getHarsData(JSONObject req) {
        JSONArray harList = new JSONArray();
        try {
            JSONArray sel = (JSONArray) req.get("selectedHars");
            for (Object o : sel) {
                try {
                    JSONObject sHar = (JSONObject) o;
                    File harF = new File(DashBoardData.hars(), sHar.get("pageName")
                            + File.separator + sHar.get("name") + ".har");
                    JSONObject har = new JSONObject();
                    har.put("har", JSONValue.parse(Tools.readFile(harF)));
                    har.put("name", sHar.get("name"));
                    har.put("report", sHar.get("report"));
                    har.put("page", sHar.get("pageName"));
                    harList.add(har);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Error while reading har file", ex);
                }
            }
            conf.put("selectedHars", sel);
            updateConfig();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while reading har file list", ex);
        }
        return harList;
    }

    public static void init() throws Exception {

        conf = new JSONObject();
        if (DashBoardData.config().exists()) {
            conf = (JSONObject) JSONValue.parse(Tools.readFile(DashBoardData.config()));
        } else if (DashBoardData.config().getParentFile().exists()) {
            DashBoardData.config().createNewFile();
        }
    }

    public static void updateConfig() {
        new Thread("server:dashboard:harCompare:updateConfig") {
            @Override
            public void run() {
                Tools.writeFile(DashBoardData.config(), conf.toString());
            }
        }.start();
    }

    private void saveHarRef(HarAdapter conn, JSONObject msg) {
        boolean stat = false;
        JSONObject res = new JSONObject();
        try {
            File refhars = DashBoardData.refHars();
            if (!refhars.exists()) {
                refhars.mkdirs();
            }
            JSONObject data = (JSONObject) msg.get("data");
            res.put("name", data.get("name"));
            File toSave = new File(refhars, data.get("name") + ".har.ref");
            Tools.writeFile(toSave, data.get("har").toString());
            stat = true;
            conf.put("ref", data.get("name"));
            updateConfig();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error while saving ref. har file", ex);
        }
        res.put("stat", stat);
        res.put("callback", msg.get("callback"));
        res.put("refList", getRefHars());
        conn.send(toMessage(res, "RES.SaveHarRef"));
    }

    private void deleteHar(HarAdapter conn, JSONObject msg) {
        JSONArray data = (JSONArray) msg.get("data");
        int counter = 0;
        for (Object i : data) {
            try {
                (new File(DashBoardData.hars(), i.toString())).delete();
                counter++;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error while deleting har file", ex);
            }
        }
        if (counter == data.size()) {
            msg.put("msg", "All files deleted successfully.");
            msg.put("stat", true);
        } else {
            msg.put("msg", "Only " + counter + " file/s deleted.");
            msg.put("stat", false);
        }
        conn.send(toMessage(msg, "DELETE.Har"));
    }
    private static final List<WebSocketAdapter> CLIENTS = new ArrayList<>();

    public static synchronized void onConnect(WebSocketAdapter client) {
        CLIENTS.add(client);
        ((HarAdapter) client).serHandler(new HarCompareHandler());
    }

    public static synchronized void onClose(WebSocketAdapter client, String reason) {
        CLIENTS.remove(client);
    }

    public static synchronized void closeAll(int code, String reason) {
        try {
            CLIENTS.stream().filter((client) -> (client != null && client.getSession() != null
                    && client.getSession().isOpen())).forEachOrdered((client) -> {
                        client.getSession().close(code, reason);
            });
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
