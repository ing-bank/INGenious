
package com.ing.datalib.testdata.view;

import com.ing.datalib.testdata.model.Record;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Plug-in to data model.,
 * <br><br>
 * <b>supports : </b>
 * <br>
 * <br> find - records for given scenario / test case / iteration /sub iteration
 *
 * <br> find - value for the given field
 * <br> update - the new value/s for the field (single or group of records)
 *
 *
 *
 */
public abstract class TestDataView {

    public final static String ALL = ".*";
    public final Map<String, List<List<String>>> VIEWS = new HashMap<>();

    abstract public List columns();

    abstract public List records();

    abstract public int index(String field);

    abstract public boolean canUpdate(String field);

    abstract public List<String> addRecord(String scenario, String testcase, String iteration, String subIteration);

    public void clear() {
        VIEWS.clear();
    }

    public void put(String key, List records) {
        VIEWS.put(key, records);
    }

    public void add(String key, List<String> records) {
        if (!VIEWS.containsKey(key)) {
            VIEWS.put(key, new ArrayList());
        }
        VIEWS.get(key).add(records);
    }

    /**
     * return the list of data for the given key from the indexed map. It will
     * return null if its not indexed, even if the records for the key exists in
     * actual model.
     * <br>
     * use it only if you know what you are doing
     * <br><br>
     * <b>Recommended Alternative:</b><br>
     * <code>withSubIter(scenario,testcase,iteration,subIter)</code>
     *
     * @see #withSubIter(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     *
     * @param key the indexed key
     * @return the subset of the model
     */
    public List get(String key) {
        return VIEWS.get(key);
    }

    /**
     * return the list of data represent the view
     *
     * @return the subset of the model
     */
    public List get() {
        return records();
    }

    /**
     * return the value from the indexed map. it will return null if its not
     * indexed even if the records for the key exists in actual model
     *
     *
     * @param key the hash key
     * <code>scn_or_gid[#testcase#iteration#subiteration]</code>
     * @param field the column name or field name
     * @return the value from the indexed map (if key exist).<br>
     * null if key is not indexed.
     */
    public String getField(String key, String field) {
        if (VIEWS.containsKey(key)) {
            return getFirst(getFields(get(key), field));
        } else {
            return null;
        }
    }

    /**
     * find and return the value for the given field aka. column from the given
     * view
     *
     * @param field the column name
     * @return the value
     */
    public String getField(String field) {
        return getFirst(getFields(get(), field));
    }

    /**
     * update the field in the current view with given value
     *
     * @param field the column name
     * @param value the new value to update
     * @return status
     */
    public boolean update(String field, String value) {
        return setFields(get(), field, value);
    }

    private String getFirst(List values) {
        if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0).toString();
        }
    }

    /**
     * find and return the list of data entries matches the key and the field
     * i.e list of rows for the given column(field). It will return null if its
     * not indexed, even if the records for the key exists in actual model.
     * <br>
     * use it only if you know what you are doing
     * <br><br>
     * <b>Recommended Alternative:</b><br>
     * <code>withSubIter(scenario,testcase,iteration,subIter)</code> along with
     * <code>getFields(field)</code>
     *
     * @see #withSubIter(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     * @see #getFields(java.lang.String)
     * @param key key set
     * @param field the column name
     * @return the list of data entries
     */
    public List<String> getFields(String key, String field) {
        if (VIEWS.containsKey(key)) {
            return getFields(get(key), field);
        } else {
            return null;
        }
    }

    public List<String> getFields(String field) {
        return getFields(get(), field);
    }

    /**
     * iteration set of the current view.
     * <br>
     * use it along with <code>withTestcase(scenario,testcase)</code> to get
     * iterations for a scenario,test case.
     *
     * @return the set of iterations
     * @see #withTestcase(java.lang.String, java.lang.String)
     */
    public Set<String> getIterations() {
        Set<String> iters = new LinkedHashSet<>();
        for (Object iter : getFields(records(), Record.HEADERS[2])) {
            iters.add((String) iter);
        }
        return iters;
    }

    /**
     * sub iteration set of the current view.
     * <br>
     * use it along with <code>withIter(scenario,testcase,iteration)</code> to
     * get sub iterations for a scenario,test case,iteration.
     *
     * @return the set of sub iterations
     * @see #withIter(java.lang.String, java.lang.String, java.lang.String)
     */
    public Set<String> getSubIterations() {
        Set<String> iters = new LinkedHashSet<>();
        for (Object iter : getFields(records(), Record.HEADERS[3])) {
            iters.add((String) iter);
        }
        return iters;
    }

    private List<String> getFields(List<List<String>> view, String field) {
        List<String> vals = new ArrayList();
        for (List<String> record : view) {
            if (fieldExist(field)) {
                vals.add(record.get(index(field)));
            }
        }
        return vals;
    }

    private boolean setFields(List<List<String>> view, String field, String value) {
        boolean updated = false;
        for (List<String> record : view) {
            if (canUpdate(field)) {
                record.set(index(field), value);
                updated = true;
            }
        }
        return updated;
    }

    private boolean fieldExist(String field) {
        return index(field) > -1;
    }

    /**
     * finds the records and return view object with that subset of records for
     * the given query args.
     *
     * @param scnOrgid scenario or global id
     * @return the view
     */
    public TestDataView withScenarioOrGID(String scnOrgid) {
        if (!VIEWS.containsKey(scnOrgid)) {
            return index(scnOrgid, scnOrgid);
        } else {
            return toView(get(scnOrgid));
        }
    }

    /**
     * finds the records and return view object with that subset of records for
     * the given query args.
     *
     * <br>
     * use wildcard (.*) if needed
     * <br>
     *
     * @param scn scenario
     * @param tc testcase
     * @return the view
     */
    public TestDataView withTestcase(String scn, String tc) {
        String key = scn + "#" + tc;
        if (!VIEWS.containsKey(key)) {
            return index(key, scn, tc);
        } else {
            return toView(get(key));
        }
    }

    /**
     * finds the records and return view object with that subset of records for
     * the given query args.
     *
     * <br>
     * use wildcard (.*) if needed
     * <br>
     *
     * @param scn scenario
     * @param tc testcase
     * @param iter iteration
     * @return the view
     */
    public TestDataView withIter(String scn, String tc, String iter) {
        return withIter(scn, tc, iter, false);
    }

    public TestDataView withIter(String scn, String tc, String iter, Boolean addIfNotPresent) {
        String key = scn + "#" + tc + "#" + iter;
        if (!VIEWS.containsKey(key)) {
            return index(key, scn, tc, iter);
        } else {
            if (addIfNotPresent && get(key).isEmpty()) {
                add(key, addRecord(scn, tc, iter, "1"));
            }
            return toView(get(key));
        }
    }

    /**
     * finds the records and return view object with that subset of records for
     * the given query args.
     *
     * <br>
     * use wildcard (.*) if needed
     * <br>
     *
     * @param scn scenario
     * @param tc testcase
     * @param iter iteration
     * @param subIter sub iteration
     * @return the view
     */
    public TestDataView withSubIter(String scn, String tc, String iter, String subIter) {
        return withSubIter(scn, tc, iter, subIter, false);
    }

    public TestDataView withSubIter(String scn, String tc, String iter, String subIter, Boolean addIfNotPresent) {
        String key = scn + "#" + tc + "#" + iter + "#" + subIter;
        if (!VIEWS.containsKey(key)) {
            return index(key, scn, tc, iter, subIter);
        } else {
            if (addIfNotPresent && get(key).isEmpty()) {
                add(key, addRecord(scn, tc, iter, subIter));
            }
            return toView(get(key));
        }
    }

    //<editor-fold defaultstate="collapsed" desc="index keys">
    private TestDataView index(String key, String scnOrgid) {
        TestDataView v = getView(scnOrgid);
        VIEWS.put(key, v.records());
        return v;
    }

    private TestDataView index(String key, String scn, String tc) {
        return index(key, scn, tc, ALL, ALL);
    }

    private TestDataView index(String key, String scn, String tc, String iter) {
        return index(key, scn, tc, iter, ALL);
    }

    private TestDataView index(String key, String scn, String tc, String iter, String subIter) {
        TestDataView v = getView(scn, tc, iter, subIter);
        VIEWS.put(key, v.records());
        return v;
    }
//</editor-fold>

    private TestDataView getView(String scnOrgid) {
        return toView(getRecords(records(), scnOrgid));
    }

    private TestDataView getView(String scn, String tc, String iter, String subIter) {
        return toView(getRecords(records(), scn, tc, iter, subIter));
    }

    private List getRecords(List records, String scnOrgid) {
        List<Object> view = new ArrayList();
        for (Object r : records) {
            try {
                if (((List) r).get(0).toString().matches(scnOrgid)) {
                    view.add(r);
                }
            } catch (Exception ex) {

            }
        }
        return view;
    }

    private List getRecords(List records, String scn, String tc, String iter, String subIter) {
        List<Record> view = new ArrayList();
        for (Object rObj : records) {
            Record r = (Record) rObj;
            try {
                if (r.getScenario().matches(scn) && r.getTestcase().matches(tc)
                        && r.getIteration().matches(iter) && r.getSubIteration().matches(subIter)) {
                    view.add(r);
                }
            } catch (Exception ex) {

            }
        }
        return view;
    }

    private TestDataView toView(final List l) {
        final TestDataView parent = this;
        return new TestDataView() {
            @Override
            public List records() {
                return l;
            }

            @Override
            public int index(String field) {
                return parent.index(field);
            }

            @Override
            public boolean canUpdate(String field) {
                return parent.canUpdate(field);
            }

            @Override
            public List columns() {
                return parent.columns();
            }

            @Override
            public List<String> addRecord(String scenario, String testcase, String iteration, String subIteration) {
                return parent.addRecord(scenario, testcase, iteration, subIteration);
            }

        };
    }

}
