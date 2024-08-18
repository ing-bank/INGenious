
package com.ing.ide.main.utils.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *
 * 
 * @param <T>
 */
public class TransferableNode<T> implements Transferable {

    private final T sourceNode;
    private final DataFlavor dataFlavor;

    public TransferableNode(T sourceNode, DataFlavor dataFlavor) {
        this.sourceNode = sourceNode;
        this.dataFlavor = dataFlavor;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {

        return new DataFlavor[]{dataFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(dataFlavor);
    }

    @Override
    public T getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (flavor.equals(dataFlavor)) {
            return sourceNode;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
