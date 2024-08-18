
package com.ing.engine.execution.exception.element;

/**
 *
 * 
 */
public class ElementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum ExceptionType {

        Element_Not_Found,
        Element_Not_Visible,
        Element_Not_Enabled,
        Element_Not_Selected,
        Not_Found_on_Screen,
        Empty_Group;

        @Override
        public String toString() {
            switch (this) {
                case Element_Not_Found:
                    return "Seems Like the Element [{{Name}}] is Not Present/Found in the page Try Adding wait or heal it";
                case Element_Not_Visible:
                    return "Seems Like the Element [{{Name}}] is Not Visible or hidden at the moment";
                case Element_Not_Enabled:
                    return "Seems Like the Element [{{Name}}] is Not Enabled";
                case Element_Not_Selected:
                    return "Seems Like the Element [{{Name}}] is Not Selected";
                case Not_Found_on_Screen:
                    return " not Found on the Screen. ";
                case Empty_Group:
                    return " -- Object Group is Empty. ";
            }
            return "";
        }
    };

    public ElementException(ExceptionType type,
            String objectName) {
        super(type.toString().replace("{{Name}}", objectName));
    }
}
