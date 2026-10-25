package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link DataIteratorApiImpl} — verifies each accessor delegates
 * to the wrapped {@link DataIterator} instance.
 */
public class DataIteratorApiImplTest {

    @Test
    public void isResolvedShouldDelegate() {
        DataIterator iterator = mock(DataIterator.class);
        when(iterator.isResolved()).thenReturn(true);

        assertThat(new DataIteratorApiImpl(iterator).isResolved()).isTrue();
    }

    @Test
    public void getMaxIterShouldDelegate() {
        DataIterator iterator = mock(DataIterator.class);
        when(iterator.getMaxIter()).thenReturn(3);

        assertThat(new DataIteratorApiImpl(iterator).getMaxIter()).isEqualTo(3);
    }

    @Test
    public void isIterResolvedShouldDelegate() {
        DataIterator iterator = mock(DataIterator.class);
        when(iterator.isIterResolved("Sheet1")).thenReturn(true);

        assertThat(new DataIteratorApiImpl(iterator).isIterResolved("Sheet1")).isTrue();
        assertThat(new DataIteratorApiImpl(iterator).isIterResolved("Sheet2")).isFalse();
    }
}
