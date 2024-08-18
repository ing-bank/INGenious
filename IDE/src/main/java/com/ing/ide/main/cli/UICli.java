
package com.ing.ide.main.cli;

import com.ing.engine.cli.CLI.Op;
import static com.ing.engine.cli.LookUp.OPTIONS;
import com.ing.ide.main.ui.About;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 *
 * 
 */
public class UICli {

    public static Boolean exe(String[] args) {
        Boolean handled = true;
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(OPTIONS, args);
            for (Option op : cmd.getOptions()) {
                switch (op.getOpt()) {
                    case Op.B_DATE:
                    case Op.B_TIME:
                        System.out.println(About.getBuildDate());
                        break;
                    case Op.B_VERSION:
                        System.out.println(About.getBuildVersion());
                        break;
                    case Op.V:
                    case Op.VERSION:
                        System.out.println(String.format("%s %s",
                                About.getBuildVersion(), About.getBuildDate()));
                        break;
                    default:
                        handled = false;
                }
            }

        } catch (ParseException ex) {
            Logger.getLogger(UICli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(UICli.class.getName()).log(Level.SEVERE, null, ex);
        }
        return handled;
    }

}
