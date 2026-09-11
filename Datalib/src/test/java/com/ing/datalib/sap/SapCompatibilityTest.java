package com.ing.datalib.sap;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.TestStep;
import com.ing.ingenious.api.types.ObjectType;
import java.util.Arrays;
import java.util.Collections;
import org.mockito.Mockito;
import org.testng.annotations.Test;

/** Tests for the shared SAP guardrail matrix - see design doc "Guardrails - what may share a SAP test case". */
public class SapCompatibilityTest {

    @Test
    public void mobileAppKafkaQueueProtractorAreBlocked() {
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.MOBILE)).isTrue();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.APP)).isTrue();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.KAFKA)).isTrue();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.QUEUE)).isTrue();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.PROTRACTORJS)).isTrue();
    }

    @Test
    public void everythingElseAlreadyWorksToday() {
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.DATABASE)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.WEBSERVICE)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.BROWSER)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.PLAYWRIGHT)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.IMAGE)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.GENERAL)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.DATA)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.SAP)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(ObjectType.SAP_OBJECT)).isFalse();
        assertThat(SapCompatibility.isBlockedWithSap(null)).isFalse();
    }

    @Test
    public void isSapObjectTypeMatchesBothSapTypes() {
        assertThat(SapCompatibility.isSapObjectType(ObjectType.SAP)).isTrue();
        assertThat(SapCompatibility.isSapObjectType(ObjectType.SAP_OBJECT)).isTrue();
        assertThat(SapCompatibility.isSapObjectType(ObjectType.BROWSER)).isFalse();
    }

    @Test
    public void blockedReasonMessageNamesTheObjectAndAFix() {
        String msg = SapCompatibility.blockedReasonMessage(ObjectType.MOBILE);
        assertThat(msg).contains("Mobile").contains("device");

        String kafkaMsg = SapCompatibility.blockedReasonMessage(ObjectType.KAFKA);
        assertThat(kafkaMsg).contains("Kafka").contains("broker");
    }

    @Test
    public void hasSapStepDetectsEitherSapObjectType() {
        TestStep sapStep = Mockito.mock(TestStep.class);
        Mockito.when(sapStep.getObject()).thenReturn(ObjectType.SAP);
        TestStep sapObjectStep = Mockito.mock(TestStep.class);
        Mockito.when(sapObjectStep.getObject()).thenReturn(ObjectType.SAP_OBJECT);
        TestStep dbStep = Mockito.mock(TestStep.class);
        Mockito.when(dbStep.getObject()).thenReturn(ObjectType.DATABASE);

        assertThat(SapCompatibility.hasSapStep(Arrays.asList(dbStep, sapStep))).isTrue();
        assertThat(SapCompatibility.hasSapStep(Arrays.asList(dbStep, sapObjectStep))).isTrue();
        assertThat(SapCompatibility.hasSapStep(Collections.singletonList(dbStep))).isFalse();
        assertThat(SapCompatibility.hasSapStep(Collections.emptyList())).isFalse();
        assertThat(SapCompatibility.hasSapStep(null)).isFalse();
    }
}
