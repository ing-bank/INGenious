package com.ing.ingenious.api.exception.mobile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementExceptionTest {

    @Test
    void constructorShouldRenderMessageUsingObjectName() {
        ElementException exception = new ElementException(
                ElementException.ExceptionType.Element_Not_Found,
                "LoginButton"
        );

        assertEquals(
                "Seems Like the Element [LoginButton] is Not Present/Found in the page Try Adding wait or heal it",
                exception.getMessage()
        );
    }

    @Test
    void exceptionTypeMessagesShouldMatchDefinitions() {
        assertEquals(
                "Seems Like the Element [{{Name}}] is Not Visible or hidden at the moment",
                ElementException.ExceptionType.Element_Not_Visible.toString()
        );
        assertEquals(
                "Seems Like the Element [{{Name}}] is Not Enabled",
                ElementException.ExceptionType.Element_Not_Enabled.toString()
        );
        assertEquals(
                "Seems Like the Element [{{Name}}] is Not Selected",
                ElementException.ExceptionType.Element_Not_Selected.toString()
        );
        assertEquals(
                " not Found on the Screen. ",
                ElementException.ExceptionType.Not_Found_on_Screen.toString()
        );
        assertEquals(
                " -- Object Group is Empty. ",
                ElementException.ExceptionType.Empty_Group.toString()
        );
    }
}
