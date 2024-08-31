
package com.ing.ide.main.mainui;

import com.ing.ide.main.utils.Utils;
import com.ing.ide.util.Canvas;
import java.awt.Insets;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

public class AppMenuBar extends JMenuBar {

    AppActionListener sActionListener;

    Map<String, KeyStroke> shortcuts;

    JMenuItem multiEnv;

    public AppMenuBar(AppActionListener sActionListener) {
        this.sActionListener = sActionListener;
        shortcuts = new HashMap<>();
        init();
    }

    private void init() {
        initShortcut();
        setMargin(new Insets(0,0,0,5));
        add(changeFont(createFileMenu()));
        add(changeFont(createObjectMenu()));
        add(changeFont(createTestDataMenu()));
        add(changeFont(createConfigurationMenu()));
        add(changeFont(createToolsMenu()));
        add(changeFont(createWindowMenu()));
        add(changeFont(createHelpMenu()));
    }

    private void initShortcut() {
        int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        shortcuts.put("New Project", KeyStroke.getKeyStroke(
                KeyEvent.VK_N, SHORTCUT | KeyEvent.SHIFT_DOWN_MASK));
        shortcuts.put("Open Project", KeyStroke.getKeyStroke(
                KeyEvent.VK_O, SHORTCUT | KeyEvent.SHIFT_DOWN_MASK));
        shortcuts.put("Save Project", KeyStroke.getKeyStroke(
                KeyEvent.VK_S, SHORTCUT | KeyEvent.SHIFT_DOWN_MASK));
        shortcuts.put("Quit", KeyStroke.getKeyStroke(
                KeyEvent.VK_X, KeyEvent.ALT_MASK));

        shortcuts.put("Object Spy", KeyStroke.getKeyStroke(
                KeyEvent.VK_P, SHORTCUT | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("Object Heal", KeyStroke.getKeyStroke(
                KeyEvent.VK_H, SHORTCUT | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("Image Spy", KeyStroke.getKeyStroke(
                KeyEvent.VK_I, SHORTCUT | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("Mobile Spy", KeyStroke.getKeyStroke(
                KeyEvent.VK_M, SHORTCUT | KeyEvent.ALT_DOWN_MASK));

        shortcuts.put("Run Settings", KeyStroke.getKeyStroke(
                KeyEvent.VK_S, SHORTCUT | KeyEvent.ALT_DOWN_MASK));

        shortcuts.put("Exploratory", KeyStroke.getKeyStroke(
                KeyEvent.VK_E, SHORTCUT | KeyEvent.SHIFT_DOWN_MASK));
        shortcuts.put("Har Compare", KeyStroke.getKeyStroke(
                KeyEvent.VK_H, SHORTCUT | KeyEvent.SHIFT_DOWN_MASK));

        shortcuts.put("Test Design", KeyStroke.getKeyStroke(
                KeyEvent.VK_N, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("Test Execution", KeyStroke.getKeyStroke(
                KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("Dashboard", KeyStroke.getKeyStroke(
                KeyEvent.VK_D, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        shortcuts.put("AdjustUI", KeyStroke.getKeyStroke(
                KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));

        shortcuts.put("Help", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        shortcuts.put("About", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        shortcuts.put("Show Log", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
    }

    private JMenu changeFont(JMenu menu) {
      //  menu.setFont(new Font("Default", Font.BOLD, 11));
      //  return menu;
      try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
          //  e.printStackTrace();
        }
      
       // menu.setFont(new Font("Courier", Font.BOLD, 12));
       menu.setFont(new Font("ING Me", Font.BOLD, 12));
//        setting color of menu text
        menu.setForeground(UIManager.getColor("text"));
        menu.setMargin(new Insets(5,3,5,3));
       return menu;
    }

    private JMenu createFileMenu() {
        JMenu file = new JMenu("File");
        file.setMnemonic('F');

        file.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("New Project", sActionListener))), 'N'));
        file.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("Open Project", sActionListener))), 'O'));
        file.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("Save Project", sActionListener))), 'S'));
        file.addSeparator();

        file.add(sActionListener.getMainFrame().getRecentItems());
        file.add(sActionListener.getMainFrame().getTestDesign().getTestCaseComp().getTestCaseHistory());

        file.add(Utils.createMenuItem("Restart", sActionListener));

        file.add(withShortCut(
                Utils.createMenuItem("Quit", sActionListener)));

        return file;
    }

    private JMenu createObjectMenu() {
        JMenu object = new JMenu("Automation");
        object.setMnemonic('A');

        object.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("Object Spy", sActionListener))), 'S'));
        object.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("Object Heal", sActionListener))), 'H'));
        object.addSeparator();
        object.add(withMnemonics(
                withShortCut(
                        withIcon(
                                Utils.createMenuItem("Image Spy", sActionListener))), 'I'));
      //  object.add(withMnemonics(
      //          withShortCut(
      //                  withIcon(
      //                          Utils.createMenuItem("Mobile Spy", sActionListener))), 'M'));
        object.addSeparator();
      //  object.add(
      //          withMnemonics(
      //                  withIcon(
      //                          Utils.createMenuItem("Inject Script", sActionListener)), 'n'));
      //  object.add(
      //          withMnemonics(Utils.createMenuItem("Create CM Project", sActionListener), 'C'));
        return object;
    }

    private JMenu createTestDataMenu() {
        JMenu testData = new JMenu("Test Data");

        testData.setMnemonic('D');

        testData.add(withMnemonics(
                Utils.createMenuItem("Import TestData", sActionListener), 'I'));

        JCheckBoxMenuItem item = new JCheckBoxMenuItem("Multiple Environment");
        item.setFont(UIManager.getFont("TableMenu.font"));
        multiEnv = withMnemonics(item, 'M');
        multiEnv.addActionListener(sActionListener);

        testData.add(multiEnv);
        return testData;
    }

    private JMenu createConfigurationMenu() {
        JMenu configure = new JMenu("Configuration");
        configure.setMnemonic('C');

        configure.add(
                withMnemonics(
                        withShortCut(
                                withIcon(
                                        Utils.createMenuItem("Run Settings", sActionListener))), 'R'));
        configure.add(
                withMnemonics(
                        withIcon(
                                Utils.createMenuItem("Browser Configuration", sActionListener)), 'B'));
     //   configure.add(
     //           withMnemonics(
     //                   withIcon(
     //                           Utils.createMenuItem("AzureDevOps TestPlan Configuration", sActionListener)), 'T'));
        configure.addSeparator();

      //  configure.add(
      //          withMnemonics(
      //                  Utils.createMenuItem("Schedule Run", sActionListener), 'S'));
       // configure.add(
         //       withMnemonics(
           //             Utils.createMenuItem("Start Server", sActionListener), 't'));

        configure.addSeparator();
        configure.add(
                withMnemonics(
                        Utils.createMenuItem("Options", sActionListener), 'O'));
        
        
        configure.add(
                withMnemonics(
                        withIcon(
                                Utils.createMenuItem("ToggleMode", sActionListener)), 'M'));
    
     return configure;
    }

    private JMenu createToolsMenu() {
        JMenu tools = new JMenu("Tools");
        tools.setMnemonic('T');

        tools.add(
                withMnemonics(
                        withShortCut(
                                withIcon(
                                        Utils.createMenuItem("Exploratory", sActionListener))), 'E'));
        JMenu optionsMenu = new JMenu("BDD");
        optionsMenu.add(Utils.createMenuItem("Import Feature File", sActionListener));
        optionsMenu.add(Utils.createMenuItem("Open Feature Editor", sActionListener));

        tools.add(optionsMenu);

//        JMenu chromeOptionsMenu = new JMenu("Import Chrome Recording");
//        chromeOptionsMenu.add(withIcon(chromeOptionsMenu));
//        chromeOptionsMenu.add(withMnemonics(chromeOptionsMenu, 'I'));
//        chromeOptionsMenu.add(Utils.createMenuItem("Import JSON", sActionListener));
//        tools.add(chromeOptionsMenu);
        
        JMenu playwrightOptionsMenu = new JMenu("Import Playwright Recording");
        playwrightOptionsMenu.add(withIcon(playwrightOptionsMenu));
        playwrightOptionsMenu.add(withMnemonics(playwrightOptionsMenu, 'I'));
        playwrightOptionsMenu.add(Utils.createMenuItem("Import Playwright Recording", sActionListener));
        tools.add(playwrightOptionsMenu);
        
        tools.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("Har Compare", sActionListener)), 'H'));
        
        return tools;
    }

    private JMenu createHelpMenu() {
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');

        help.add(
                withMnemonics(
                        withEmptyIcon(
                                withShortCut(
                                        Utils.createMenuItem("Help", sActionListener))), 'H'));
        help.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("About", sActionListener)), 'A'));
        help.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("Show Log", sActionListener)), 'o'));

        return help;
    }

    private JMenu createWindowMenu() {
        JMenu window = new JMenu("Window");
        window.setMnemonic('W');

        window.add(
                withMnemonics(
                        withEmptyIcon(
                                withShortCut(Utils.createMenuItem("Test Design", sActionListener))), 'T'));

        window.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("Test Execution", sActionListener)), 'E'));
        window.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("Dashboard", sActionListener)), 'D'));

        window.add(
                withMnemonics(
                        withShortCut(
                                Utils.createMenuItem("AdjustUI", sActionListener)), 'A'));

        return window;
    }

    private JMenuItem withEmptyIcon(JMenuItem menuItem) {
        menuItem.setIcon(Canvas.EmptyIcon);
        return menuItem;
    }

    private JMenuItem withIcon(JMenuItem menuItem) {
        menuItem.setIcon(Utils.getIconByResourceName("/ui/resources/main/" + menuItem.getText().replace(" ", "") + "Menu"));
        return menuItem;
    }

    private JMenuItem withShortCut(JMenuItem menuItem) {
        menuItem.setAccelerator(shortcuts.get(menuItem.getText()));
        return menuItem;
    }

    private JMenuItem withMnemonics(JMenuItem menuItem, char m) {
        menuItem.setMnemonic(m);
        return menuItem;
    }

    public void setMultiEnvironment() {
        Boolean isMulti = sActionListener.getMainFrame().getProject().getTestData().getNoOfEnvironments() > 1;
        if ((isMulti && !multiEnv.isSelected())
                || (!isMulti && multiEnv.isSelected())) {
            multiEnv.doClick();
        }
    }

}
