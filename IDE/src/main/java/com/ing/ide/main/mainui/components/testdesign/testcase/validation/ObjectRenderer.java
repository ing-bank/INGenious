
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * Renderer responsible for validating and visually marking the “Object” column
 * of a test step within the Test Design UI.
 * <p>
 * {@code ObjectRenderer} determines whether the object value is empty, refers to
 * a valid page-object in the Object Repository, or matches one of the allowed
 * high-level object types (e.g., Execute, Browser, Mobile, Database). Based on
 * this evaluation, it applies visual cues using foreground color, font style,
 * and error highlighting rules.
 * </p>
 *
 * <p>
 * The renderer also handles special cases such as commented steps, object
 * presence checks via {@link ResolvedWebObject}, and default vs. error states
 * inherited from {@code AbstractRenderer}.
 * </p>
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
        if (ref != null && ref.name != null && ref.scope != null) {
            return repo.resolveWebObject(ref, objectName) != null;
        }
        return repo.resolveWebObjectWithScope(pageToken, objectName) != null;
    }

    private Boolean isValidObject(Object value) {
        return Objects.toString(value, "").trim()
                .matches("Execute|Mobile|Browser|Database|Webservice|Kafka|Synthetic Data|Queue|File|General|String Operations");
    }

}
