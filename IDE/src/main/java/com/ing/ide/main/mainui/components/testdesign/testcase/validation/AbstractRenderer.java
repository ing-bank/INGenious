
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.awt.Color;
import java.awt.Component;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

public abstract class AbstractRenderer extends DefaultTableCellRenderer {

    private final Border errorBorder = BorderFactory.createLineBorder(Color.RED, 1);

    private final String empty;

    public AbstractRenderer(String empty) {
        this.empty = empty;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        JComponent comp = (JComponent) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);
        if (getTestCase(table) != null) {
            TestStep step = getTestCase(table).getTestSteps().get(row);
            if (!isSelected) {
                if (step.isEmpty()) {
                    setDefault(comp);
                } else {
                    render(comp, step, value);
                }
            } else {
                comp.setForeground(Color.WHITE);
            }
        }
        return comp;
    }

    public abstract void render(JComponent comp, TestStep step, Object value);

    protected void setEmpty(JComponent comp) {
        comp.setBorder(errorBorder);
        comp.setToolTipText(empty);
    }

    protected void setNotPresent(JComponent comp, String notPresent) {
        comp.setForeground(Color.RED);
        comp.setToolTipText(notPresent);
    }
	
    protected void setWebserviceRequest(JComponent comp) {
        comp.setForeground(new Color(0,204,0));
    }
    
    protected void setText(JComponent comp) {
        comp.setForeground(new Color(0,204,0));
    }
    
    protected void setWebserviceStart(JComponent comp) {
        comp.setForeground(Color.BLUE);
    }
    
    protected void setWebserviceStop(JComponent comp) {
        comp.setForeground(new Color(153,102,0));
    }
    
    protected void setReusable(JComponent comp) {
        comp.setForeground(Color.green);
        comp.setToolTipText(null);
    }
	
    protected void setDefault(JComponent comp) {
        
        comp.setForeground(UIManager.getColor("text"));
        comp.setToolTipText(null);
    }

    protected Boolean isEmpty(Object value) {
        return Objects.toString(value, "").trim().isEmpty();
    }

    protected TestCase getTestCase(JTable table) {
        if (table.getModel() instanceof TestCase) {
            return (TestCase) table.getModel();
        }
        return null;
    }

}
