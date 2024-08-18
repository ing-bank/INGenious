
package com.ing.ide.main;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.engine.cli.LookUp;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ide.main.cli.UICli;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.Splash;
import com.ing.ide.main.ui.About;
import com.ing.ide.util.logging.UILogger;
import com.ing.util.encryption.Encryption;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.Font;
import java.awt.FontFormatException;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Painter;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.commons.lang3.time.StopWatch;

public class Main {

    private static final StopWatch STOP_WATCH = new StopWatch();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tmS %1$tz [%4$-4s] %2$s:%5$s%6$s%n");
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            commandLineExecution(args);
        } else {
            UILogger.get();
            Logger.getLogger(Main.class.getName()).info("Launching INGenious Playwright Studio");
            STOP_WATCH.start();
            launchUI();
        }
    }

    private static void commandLineExecution(String[] args) {
        initCommonDependencies();
        if (!UICli.exe(args)) {
            LookUp.exe(args);
        }
    }

    private static void initCommonDependencies() {
        TestDataFactory.load();
        About.init();
        Encryption.getInstance();
        SystemDefaults.getClassesFromJar.set(true);
    }

    private static void launchUI() {
        Splash splash = new Splash();
        splash.setVisible(true);
        new Thread(() -> {
            setUpUI("Nimbus");
            splash.progressed(10);
            initDependencies();
            splash.progressed(20);
            AppMainFrame mainFrame = null;
            try {
                mainFrame = new AppMainFrame(splash::progressed);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            mainFrame.setVisible(false);
            mainFrame.setMinimumSize(new Dimension(800, 400));
            mainFrame.setPreferredSize(new Dimension(800, 400));
            mainFrame.setLocationRelativeTo(null);
            splash.progressed(99);
            splash.setVisible(false);
            mainFrame.checkAndLoadRecent();
            mainFrame.setDefaultCloseOperation(AppMainFrame.DO_NOTHING_ON_CLOSE);
            Boolean IS_MAXI_SUPPORTED = Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH);
            if (IS_MAXI_SUPPORTED) {
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            splash.dispose();
            mainFrame.setVisible(true);
        }, "UI:MainUI").start();

    }

    private static void initDependencies() {
        initCommonDependencies();
        MethodInfoManager.load();
        //ByObjectProp.load();
    }

    private static void setUpUI(String ui) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (ui.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    if (ui.equals("Nimbus")) {
                        tweakNimbusUI();
                    }
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static void tweakNimbusUI() {
        
        
        UIManager.put("tableColor", Color.decode("#F5FAFD"));
        UIManager.put("subToolBar", Color.decode("#E5F1FA"));
        UIManager.put("toolBar", Color.decode("#FFFFFF"));
        UIManager.put("execPanel", Color.decode("#FFFEFC"));
        UIManager.put("execToolBar", Color.decode("#F9E6AE"));
        UIManager.put("BDDEditor", Color.decode("#e0cf9c"));
        //UIManager.put("nimbusBase", Color.decode("#E5E5E5"));//#Fae4c6
        UIManager.put("nimbusBase", Color.decode("#Fae4c6"));
        UIManager.put("searchBox", Color.decode("#FFFFFF"));
        UIManager.put("nimbusFocus", new Color(0, 0, 0, 0));
        UIManager.put("shadow", new Color(0, 0, 0, (float) 0.16));
        UIManager.put("exec", Color.decode("#CEA93A"));
//         background of tables and properties
        UIManager.put("nimbusLightBackground", Color.decode("#FAFCFE"));//#939bb7
       //UIManager.put("nimbusLightBackground", Color.decode("#Fae4c6"));
        //UIManager.put("nimbusSelectedText", Color.white);
       // UIManager.put("nimbusSelectedText", Color.decode("#FF6200"));
      //  UIManager.put("nimbusSelectedText", Color.decode("#93C47D"));#4073ff
        UIManager.put("nimbusSelectedText", Color.decode("#0000ff"));
       // UIManager.put("nimbusSelectionBackground", Color.decode("#FF6200"));//ING Orange
        UIManager.put("nimbusSelectionBackground", Color.decode("#ffcfb2"));
       // UIManager.put("text", Color.decode("#2D2D30"));//583A74
        UIManager.put("text", Color.decode("#583A74")); // Top Menu Font Color
   
        UIManager.put("gridColor", Color.decode("#FAFCFE"));//////
        UIManager.put("designTableHeader", Color.decode("#E5F1FA"));
        UIManager.put("execTableHeader", Color.decode("#F8E6B2"));
//        for whole background + tree panels
//        UIManager.put("control", Color.decode("#E0E8EF"));
        UIManager.put("control", Color.decode("#F1ECDE"));
        UIManager.put("execColor", Color.decode("#F1ECDE"));
        UIManager.put("designColor", Color.decode("#E0E8EF"));
        UIManager.put("execTableColor", Color.decode("#FEFDFA"));
//      UIManager.getLookAndFeelDefaults().put("Tree:TreeCell.contentMargins", new Insets(0,40,0,10));

        UIManager.getLookAndFeelDefaults().put("nimbusInfoBlue", Color.decode("#F7FAFC"));
        UIManager.getLookAndFeelDefaults().put("nimbusBlueGrey", Color.decode("#E5F1FA"));

        UIManager.getLookAndFeelDefaults().put("nimbusBorder", UIManager.getColor("control"));
        
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            //e.printStackTrace();
        }
        
 //     UIManager.getLookAndFeelDefaults().put("Menu.font", new Font("Courier", Font.BOLD, 12));
        UIManager.put("TableMenu.font", new Font("ING Me", Font.PLAIN, 11));
        UIManager.put("Table.font", new Font("ING Me", Font.BOLD, 12));
        UIManager.getLookAndFeelDefaults().put("Menu.font", new Font("ING Me", Font.BOLD, 12));
        
        UIManager.getLookAndFeelDefaults().put("ScrollPane[Enabled].borderPainter", new FillPainter1(new Color(0, 0, 0, 0)));

        UIManager.getLookAndFeelDefaults().put("MenuBar[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("Menu[Enabled+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
//      UIManager.getLookAndFeelDefaults().put("MenuBar.background", Color.GREEN);
        UIManager.getLookAndFeelDefaults().put("MenuBar:Menu[Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton[Selected].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Pressed].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton.contentMargins", new Insets(8, 5, 8, 5));

        UIManager.getLookAndFeelDefaults().put("Table.alternateRowColor", UIManager.getColor("execTableColor"));
        UIManager.getLookAndFeelDefaults().put("TableHeader:\"TableHeader.renderer\"[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("TableHeader:\"TableHeader.renderer\"[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.05)));
//      UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver].textForeground", Color.decode("#333335"));
//      UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver+Selected].textForeground",Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));//ING Orange
        UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("MenuItem[Enabled].textForeground", Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("MenuItem[Disabled].textForeground", Color.GRAY);
        UIManager.getLookAndFeelDefaults().put("MenuItem[MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("Menu[Enabled].textForeground", Color.decode("#333335"));
//      UIManager.getLookAndFeelDefaults().put("Tree.font", new java.awt.Font("Segoe UI", Font.PLAIN, 14));
        UIManager.getLookAndFeelDefaults().put("Table.showGrid", true);

        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        //UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", new FillPainter1(Color.decode("#D4E9F7"))); 
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", new FillPainter1(Color.decode("#F7dcd4")));//Added
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+Selected].backgroundPainter", new FillPainter1(Color.decode("#ffcfb2")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#ffcfb2")));
        //UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        //UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Pressed+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled+MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea.contentMargins", new Insets(10, 0, 0, 0));
//      UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab.contentMargins", new Insets(10, 0, 10, 0));

        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[MouseOver+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Pressed+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled+MouseOver].textForeground", Color.decode("#FFFFFF"));//#FF6200

        UIManager.getLookAndFeelDefaults().put("ToolTip[Enabled].backgroundPainter", new FillPainter1(Color.decode("#F4F6F8")));

        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Focused+MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Pressed].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("SplitPane:SplitPaneDivider[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));

        UIManager.getLookAndFeelDefaults().put("TextField.foreground", Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("TextField.background", Color.decode("#FFFFFF"));
//        UIManager.getLookAndFeelDefaults().put("FormattedTextField.inactiveBackground", Color.decode("#007acc"));
        
        
        /**
         * custom tab-area border painter
         */
        Painter tabborder = (Painter) (Graphics2D g, Object object, int width, int height) -> {
            //add code to customize
        };
        //defaults.put("TabbedPane:TabbedPaneTabArea[Disabled].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter", tabborder);
        PopupFactory.setSharedInstance(new PopupFactory() {
            @Override
            public Popup getPopup(Component owner, final Component contents, int x, int y) throws IllegalArgumentException {
                Popup popup = super.getPopup(owner, contents, x, y);
                SwingUtilities.invokeLater(() -> {
                    contents.repaint();
                });
                return popup;
            }
        });
    }
    
    private static void tweakNimbusDarkUI() {
        
        
        UIManager.put("tableColor", Color.decode("#F5FAFD"));
        UIManager.put("subToolBar", Color.decode("#E5F1FA"));
        UIManager.put("toolBar", Color.decode("#FFFFFF"));
        UIManager.put("execPanel", Color.decode("#FFFEFC"));
        UIManager.put("execToolBar", Color.decode("#F9E6AE"));
        UIManager.put("BDDEditor", Color.decode("#e0cf9c"));
        //UIManager.put("nimbusBase", Color.decode("#E5E5E5"));//#Fae4c6
        UIManager.put("nimbusBase", Color.decode("#333335")); // ---> Base Color
        UIManager.put("searchBox", Color.decode("#FFFFFF"));
        UIManager.put("nimbusFocus", new Color(0, 0, 0, 0));
        UIManager.put("shadow", new Color(0, 0, 0, (float) 0.16));
        UIManager.put("exec", Color.decode("#CEA93A"));
//         background of tables and properties
        UIManager.put("nimbusLightBackground", Color.decode("#FAFCFE"));//#939bb7
       //UIManager.put("nimbusLightBackground", Color.decode("#Fae4c6"));
        //UIManager.put("nimbusSelectedText", Color.white);
       // UIManager.put("nimbusSelectedText", Color.decode("#FF6200"));
      //  UIManager.put("nimbusSelectedText", Color.decode("#93C47D"));#4073ff
        UIManager.put("nimbusSelectedText", Color.decode("#0000ff"));
        UIManager.put("nimbusSelectionBackground", Color.decode("#FF6200"));//ING Orange
       // UIManager.put("text", Color.decode("#2D2D30"));//583A74
        UIManager.put("text", Color.decode("#583A74")); // Top Menu Font Color
   
        UIManager.put("gridColor", Color.decode("#FAFCFE"));//////
        UIManager.put("designTableHeader", Color.decode("#E5F1FA"));
        UIManager.put("execTableHeader", Color.decode("#F8E6B2"));
//        for whole background + tree panels
//        UIManager.put("control", Color.decode("#E0E8EF"));
        UIManager.put("control", Color.decode("#333335"));//--> Dark Theme
        UIManager.put("execColor", Color.decode("#F1ECDE"));
        UIManager.put("designColor", Color.decode("#E0E8EF"));
        UIManager.put("execTableColor", Color.decode("#FEFDFA"));
//      UIManager.getLookAndFeelDefaults().put("Tree:TreeCell.contentMargins", new Insets(0,40,0,10));

        UIManager.getLookAndFeelDefaults().put("nimbusInfoBlue", Color.decode("#F7FAFC"));
        UIManager.getLookAndFeelDefaults().put("nimbusBlueGrey", Color.decode("#E5F1FA"));

        UIManager.getLookAndFeelDefaults().put("nimbusBorder", UIManager.getColor("control"));
        
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
          //  e.printStackTrace();
        }
        
 //     UIManager.getLookAndFeelDefaults().put("Menu.font", new Font("Courier", Font.BOLD, 12));
        UIManager.put("TableMenu.font", new Font("ING Me", Font.PLAIN, 11));
        UIManager.put("Table.font", new Font("ING Me", Font.BOLD, 12));
        UIManager.getLookAndFeelDefaults().put("Menu.font", new Font("ING Me", Font.BOLD, 12));
        
        UIManager.getLookAndFeelDefaults().put("ScrollPane[Enabled].borderPainter", new FillPainter1(new Color(0, 0, 0, 0)));

        UIManager.getLookAndFeelDefaults().put("MenuBar[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("Menu[Enabled+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
//      UIManager.getLookAndFeelDefaults().put("MenuBar.background", Color.GREEN);
        UIManager.getLookAndFeelDefaults().put("MenuBar:Menu[Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton[Selected].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Pressed].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:ToggleButton.contentMargins", new Insets(8, 5, 8, 5));

        UIManager.getLookAndFeelDefaults().put("Table.alternateRowColor", UIManager.getColor("execTableColor"));
        UIManager.getLookAndFeelDefaults().put("TableHeader:\"TableHeader.renderer\"[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("TableHeader:\"TableHeader.renderer\"[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.05)));
//      UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver].textForeground", Color.decode("#333335"));
//      UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver+Selected].textForeground",Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));//ING Orange
        UIManager.getLookAndFeelDefaults().put("CheckBoxMenuItem[MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("MenuItem[Enabled].textForeground", Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("MenuItem[Disabled].textForeground", Color.GRAY);
        UIManager.getLookAndFeelDefaults().put("MenuItem[MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("Menu[Enabled].textForeground", Color.decode("#333335"));
//      UIManager.getLookAndFeelDefaults().put("Tree.font", new java.awt.Font("Segoe UI", Font.PLAIN, 14));
        UIManager.getLookAndFeelDefaults().put("Table.showGrid", true);

        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter", new FillPainter1(Color.decode("#FFFFFF")));
        //UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", new FillPainter1(Color.decode("#D4E9F7"))); 
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled].backgroundPainter", new FillPainter1(Color.decode("#F7dcd4")));//Added
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Pressed+Selected].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled+MouseOver].backgroundPainter", new FillPainter1(Color.decode("#FF6200")));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTabArea.contentMargins", new Insets(10, 0, 0, 0));
//      UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab.contentMargins", new Insets(10, 0, 10, 0));

        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[MouseOver+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Focused+MouseOver+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Pressed+Selected].textForeground", Color.decode("#FFFFFF"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab[Enabled+MouseOver].textForeground", Color.decode("#FFFFFF"));//#FF6200

        UIManager.getLookAndFeelDefaults().put("ToolTip[Enabled].backgroundPainter", new FillPainter1(Color.decode("#F4F6F8")));

        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Focused+MouseOver].backgroundPainter", new FillPainter1(new Color(0, 0, 0, (float) 0.0)));
        UIManager.getLookAndFeelDefaults().put("ToolBar:Button[Pressed].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));
        UIManager.getLookAndFeelDefaults().put("SplitPane:SplitPaneDivider[Enabled].backgroundPainter", new FillPainter1(new Color(0, 0, 0, 0)));

        UIManager.getLookAndFeelDefaults().put("TextField.foreground", Color.decode("#333335"));
        UIManager.getLookAndFeelDefaults().put("TextField.background", Color.decode("#FFFFFF"));
//        UIManager.getLookAndFeelDefaults().put("FormattedTextField.inactiveBackground", Color.decode("#007acc"));
        
        
        /**
         * custom tab-area border painter
         */
        Painter tabborder = (Painter) (Graphics2D g, Object object, int width, int height) -> {
            //add code to customize
        };
        //defaults.put("TabbedPane:TabbedPaneTabArea[Disabled].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled+MouseOver].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled+Pressed].backgroundPainter", tabborder);
        //defaults.put("TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter", tabborder);
        PopupFactory.setSharedInstance(new PopupFactory() {
            @Override
            public Popup getPopup(Component owner, final Component contents, int x, int y) throws IllegalArgumentException {
                Popup popup = super.getPopup(owner, contents, x, y);
                SwingUtilities.invokeLater(() -> {
                    contents.repaint();
                });
                return popup;
            }
        });
    }

    public static void finish() {
       STOP_WATCH.stop();
       Logger.getLogger(Main.class.getName()).log(Level.INFO, "INGenious Playwright Studio has been Terminated - [ Total Time : {0} ]", STOP_WATCH.toString());
    }

private static class FillPainter1 implements Painter<JComponent> {

        private final Color color;

        FillPainter1(Color c) {
            color = c;
        }

        @Override
        public void paint(Graphics2D g, JComponent object, int width, int height) {
//            Paint p = new GradientPaint(0.0f, 0.0f, color.brighter(), width, height, color, false);
//            Point2D center = new Point2D.Float(50, 50);
//     float radius = width;
//     float[] dist = {0.0f, 0.2f, 1.0f};
//     Color[] colors = {color.darker(), color, color.brighter()};
//     RadialGradientPaint r =
//         new RadialGradientPaint(center, radius, dist, colors);

            g.setPaint(color);
            g.fillRect(0, 0, width - 1, height - 1);
        }
      }   
     
}

