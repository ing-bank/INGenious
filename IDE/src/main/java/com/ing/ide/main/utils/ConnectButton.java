
package com.ing.ide.main.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import com.ing.ide.main.fx.INGIcons;

/**
 *
 * 
 */
public class ConnectButton extends JButton implements ActionListener{

    private static final javax.swing.Icon DEFAULT_ICON = INGIcons.swingColored("icon.bulb_yellow", 16);
    private static final javax.swing.Icon PASS_ICON = INGIcons.swingColored("icon.bulb_green", 16);
    private static final javax.swing.Icon FAIL_ICON = INGIcons.swingColored("icon.bulb_red", 16);

    public ConnectButton() {
        super("Test Connection");
        reset();
        setHorizontalTextPosition(JButton.RIGHT);
        addActionLis();
    }
    
    private void addActionLis(){
        addActionListener(this);
    }

    public final void success() {
        setIcon(PASS_ICON);
    }

    public final void failure() {
        setIcon(FAIL_ICON);
    }

    public final void reset() {
        setIcon(DEFAULT_ICON);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        
    }

}
