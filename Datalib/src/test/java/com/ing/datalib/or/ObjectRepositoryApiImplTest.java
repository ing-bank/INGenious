package com.ing.datalib.or;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link ObjectRepositoryApiImpl} — verifies each existence-check
 * accessor delegates to the wrapped {@link ObjectRepository} instance.
 */
public class ObjectRepositoryApiImplTest {

    @Test
    public void isObjectPresentShouldDelegate() {
        ObjectRepository or = mock(ObjectRepository.class);
        when(or.isObjectPresent("Page1", "Object1")).thenReturn(true);

        assertThat(new ObjectRepositoryApiImpl(or).isObjectPresent("Page1", "Object1")).isTrue();
    }

    @Test
    public void isMobileObjectPresentShouldDelegate() {
        ObjectRepository or = mock(ObjectRepository.class);
        when(or.isMobileObjectPresent("Page1", "Object1")).thenReturn(true);

        assertThat(new ObjectRepositoryApiImpl(or).isMobileObjectPresent("Page1", "Object1"))
            .isTrue();
    }

    @Test
    public void isStructuredDataObjectPresentShouldDelegate() {
        ObjectRepository or = mock(ObjectRepository.class);
        when(or.isStructuredDataObjectPresent("Page1", "Object1")).thenReturn(false);

        assertThat(
                new ObjectRepositoryApiImpl(or).isStructuredDataObjectPresent("Page1", "Object1")
            )
            .isFalse();
    }
}
