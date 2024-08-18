
package com.ing.ide.main.utils.dnd;

import com.ing.ide.main.mainui.components.testdesign.testdata.TestDataDetail;
import java.awt.datatransfer.DataFlavor;

/**
 *
 * 
 */
public class DataFlavors {

    public static final DataFlavor TESTDATA_FLAVOR = new DataFlavor(TestDataDetail.class, TestDataDetail.class.getSimpleName());
}
