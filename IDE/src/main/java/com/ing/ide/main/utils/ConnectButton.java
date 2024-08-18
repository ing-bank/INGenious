
package com.ing.ide.main.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 *
 * 
 */
public class ConnectButton extends JButton implements ActionListener{

    private static final ImageIcon DEFAULT_ICON
            = new ImageIcon(ConnectButton.class.getResource("/ui/resources/toolbar/bulb_yellow.png"));
    private static final ImageIcon PASS_ICON
            = new ImageIcon(ConnectButton.class.getResource("/ui/resources/toolbar/bulb_green.png"));
    private static final ImageIcon FAIL_ICON
            = new ImageIcon(ConnectButton.class.getResource("/ui/resources/toolbar/bulb_red.png"));

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
