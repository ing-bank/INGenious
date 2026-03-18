package com.ing.ingenious.api.exception.mobile;

/**
 * Exception thrown when element operations fail in mobile or web automation.
 * This is a framework exception with zero vendor dependencies, making it safe
 * for use in the API module and by plugin developers.
 * 
 * @since 3.0
 */
public class ElementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Types of element exceptions that can occur during automation.
     */
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
    }

    /**
     * Creates a new ElementException with the specified type and object name.
     * 
     * @param type the exception type
     * @param objectName the name of the element that caused the exception
     */
    public ElementException(ExceptionType type, String objectName) {
        super(type.toString().replace("{{Name}}", objectName));
    }
}
