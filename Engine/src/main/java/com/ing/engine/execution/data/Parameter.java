
package com.ing.engine.execution.data;


/**
 *
 * 
 */
public class Parameter {

    private int iteration;
    private int subIteration;

    public Parameter() {
        this.iteration = 1;
        this.subIteration = 1;
    }

    public Parameter(int iteration, int subIteration) {
        this.iteration = iteration;
        this.subIteration = subIteration;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public Parameter withIteration(int iteration) {
        this.iteration = iteration;
        return this;
    }

    public int getSubIteration() {
        return subIteration;
    }

    public void setSubIteration(int subIteration) {
        this.subIteration = subIteration;
    }

    public Parameter withSubIteration(int subIteration) {
        this.subIteration = subIteration;
        return this;
    }

    public static int resolveMaxIter(String iter) {
        if (iter == null || iter.isEmpty() || "Single".equalsIgnoreCase(iter)) {
            return 1;
        } else if ("All".equalsIgnoreCase(iter)) {
            return -1;
        } else if (iter.matches("\\d+")) {
            return Integer.valueOf(iter);
        } else if (iter.matches("\\d+:\\d+")) {
            return Integer.valueOf(iter.split(":")[1]);
        } else {
            return -1;
        }
    }

    public static int resolveStartIter(String iter) {
        if (iter == null || iter.isEmpty() || "Single".equalsIgnoreCase(iter)
                || "All".equalsIgnoreCase(iter)) {
            return 1;
        } else if (iter.matches("\\d+")) {
            return Integer.valueOf(iter);
        } else if (iter.matches("\\d+:\\d+")) {
            return Integer.valueOf(iter.split(":")[0]);
        } else {
            return 1;
        }
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="param loops">
    public static boolean startParamRLoop(String condition) {
        return condition.matches("Start (Param|Loop).*");
    }

    public static boolean isLoop(String condition) {
        return condition.matches("(Start|End) Loop.*");
    }

    public static boolean endParamRLoop(String condition) {
        return condition.matches("End (Param|Loop).*");
    }
//</editor-fold>

    @Override
    public String toString(){
    	return String.format("%s:%s", iteration,subIteration);
    }
}
