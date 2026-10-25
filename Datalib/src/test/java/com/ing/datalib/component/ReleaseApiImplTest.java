package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link ReleaseApiImpl} — verifies the test-set lookup accessor
 * delegates to the wrapped {@link Release} instance.
 */
public class ReleaseApiImplTest {

    @Test
    public void getTestSetByNameShouldWrapTheUnderlyingTestSet() {
        Release release = mock(Release.class);
        TestSet testSet = mock(TestSet.class);
        when(release.getTestSetByName("TestSet1")).thenReturn(testSet);

        assertThat(new ReleaseApiImpl(release).getTestSetByName("TestSet1")).isNotNull();
    }

    @Test
    public void getTestSetByNameShouldReturnNullWhenNotFound() {
        Release release = mock(Release.class);
        when(release.getTestSetByName("Unknown")).thenReturn(null);

        assertThat(new ReleaseApiImpl(release).getTestSetByName("Unknown")).isNull();
    }
}
