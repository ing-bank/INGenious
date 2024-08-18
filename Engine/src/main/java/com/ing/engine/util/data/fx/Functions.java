package com.ing.engine.util.data.fx;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class Functions extends FunctionsLib {

    private static final Logger LOG = Logger.getLogger(Functions.class.getName());

    public Object Date(String... args) {
        try {
            int paramLength = args.length, dx;
            switch (paramLength) {
                case 1:
                    dx = Integer.parseInt((args[0]).split("\\.")[0]);
                    return this.getDate(dx);
                case 2:
                    dx = Integer.parseInt((args[0]).split("\\.")[0]);
                    String format = args[1];
                    return this.getDate(dx, format);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return this.getDate(0);
    }

    public Object Round(String... args) {
        try {
            Double val = Double.parseDouble((args[0]));
            return this.getRound(val);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return args[0];
    }

    public Object Random(String... args) {
        try {
            int paramLength = args.length;
            switch (paramLength) {
                case 1:
                    Double len = Double.parseDouble(args[0]);
                    return this.getRandom(len);
                case 2:
                    Double from = Double.parseDouble(args[0]);
                    Double to = Double.parseDouble(args[1]);
                    return this.getRandom(from, to);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return 0;
    }

    public Object Pow(String... args) {
        try {
            Double a = Double.parseDouble(args[0]);
            Double b = Double.parseDouble(args[1]);
            return this.getPow(a, b);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return 0;
    }

    public Object Min(String... args) {
        try {
            Double a = Double.parseDouble(args[0]);
            Double b = Double.parseDouble(args[1]);
            return this.getMin(a, b);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return 0;
    }

    public Object Max(String... args) {
        try {
            Double a = Double.parseDouble(args[0]);
            Double b = Double.parseDouble(args[1]);
            return this.getMax(a, b);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return 0;
    }

    public Object Concat(String... args) {
        String op = "";
        try {
            if (args.length > 0) {
                for (String arg : args) {
                    op += arg;
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return op;
    }

    public Object ToLower(String... args) {
        String op = "";
        try {
            if (args.length > 0) {
                op = args[0].toLowerCase();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return op;
    }

    public Object ToUpper(String... args) {
        String op = "";
        try {
            if (args.length > 0) {
                op = args[0].toUpperCase();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return op;
    }

    public Object getLength(String... args) {
        int length = 0;
        try {
            if (args.length > 0) {
                length = args[0].length();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return length;
    }

    public Object getOccurance(String... args) {
        int count = 0;
        String originalString = args[0];
        String searchString = args[1];
        int index = originalString.indexOf(searchString);

        try {
            if (args.length > 0) {
                while (index != -1) {
                    count++;
                    index = originalString.indexOf(searchString, index + 1);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return count;
    }

    public Object Trim(String... args) {
        String op = "";
        try {
            if (args.length > 0) {
                op = args[0].trim();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return op;
    }

    public Object Replace(String... args) {
        String op = "";
        return op;
    }

    public Object Split(String... args) {
        String op = "";
        return op;
    }

    public Object Substring(String... args) {
        String op = "";
        return op;
    }
}
