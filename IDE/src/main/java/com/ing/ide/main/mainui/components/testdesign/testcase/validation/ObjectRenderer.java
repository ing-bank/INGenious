
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

/**
 *
 * 
 */
public class ObjectRenderer extends AbstractRenderer {

    String objNotPresent = "Object is not present in the Object Repository";

    public ObjectRenderer() {
        super("Object Shouldn't be empty.It should be one of[Execute,App,Browser or Object]");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented()) {
            if (isEmpty(value)) {
                setEmpty(comp);
            } else if (step.isPageObjectStep()) {
                if (isObjectPresent(step)) {
                    setDefault(comp);
                } else {
                    setNotPresent(comp, objNotPresent);
                }
            } else if (isValidObject(value)) {
                setDefault(comp);
            } else {
                setNotPresent(comp, objNotPresent);
            }
        } else {
            setDefault(comp);
            comp.setForeground(Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }

	private Color getColor(Object value) {
        String val = Objects.toString(value, "").trim();
        switch (val) {
            case "Execute":
                return Color.BLUE;//.darker();
            case "Mobile":
                return Color.CYAN;//.darker();
            case "Browser":
                return Color.RED;//.darker();
            default:
                return new Color(204, 0, 255);
        }
    }

    private Boolean isObjectPresent(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();

        ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(pageToken);
        if (ref != null && ref.name != null && pageToken != null && pageToken.contains("@")) {
            return repo.resolveWebObject(ref, objectName) != null;
        }

        return repo.resolveWebObjectWithScope(pageToken, objectName) != null;
    }

    private Boolean isValidObject(Object value) {
        return Objects.toString(value, "").trim()
                .matches("Execute|Mobile|Browser|Database|Webservice|Kafka|Synthetic Data|Queue|File|General|String Operations");
    }

}
