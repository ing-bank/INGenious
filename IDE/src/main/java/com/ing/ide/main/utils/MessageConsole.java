
package com.ing.ide.main.utils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;


/*
 *  Create a simple console to display text messages.
 *
 *  Messages can be directed here from different sources. Each source can
 *  have its messages displayed in a different color.
 *
 *  Messages can either be appended to the console or inserted as the first
 *  line of the console
 *
 *  You can limit the number of lines to hold in the Document.
 */
public class MessageConsole {

    private static final Color DARK_GREEN = new Color(0x00, 0xC0, 0x00);

    private JTextComponent textComponent;
    private Document document;
    private boolean isAppend;
    private DocumentListener limitLinesListener;

    public MessageConsole(JTextComponent textComponent) {
        this(textComponent, true);
    }

    /*
     *	Use the text component specified as a simply console to display
     *  text messages.
     *
     *  The messages can either be appended to the end of the console or
     *  inserted as the first line of the console.
     */
    public MessageConsole(JTextComponent textComponent, boolean isAppend) {
        this.textComponent = textComponent;
        this.document = textComponent.getDocument();
        this.isAppend = isAppend;
        textComponent.setEditable(false);
    }

    /*
     *  Redirect the output from the standard output to the console
     *  using the default text color and null PrintStream
     */
    public void redirectOut() {
        redirectOut(null, null);
    }

    /*
     *  Redirect the output from the standard output to the console
     *  using the specified color and PrintStream. When a PrintStream
     *  is specified the message will be added to the Document before
     *  it is also written to the PrintStream.
     */
    public void redirectOut(Color textColor, PrintStream printStream) {
        ConsoleOutputStream cos = new ConsoleOutputStream(textColor, printStream);
        System.setOut(new PrintStream(cos, true));
    }

    /*
     *  Redirect the output from the standard error to the console
     *  using the default text color and null PrintStream
     */
    public void redirectErr() {
        redirectErr(null, null);
    }

    /*
     *  Redirect the output from the standard error to the console
     *  using the specified color and PrintStream. When a PrintStream
     *  is specified the message will be added to the Document before
     *  it is also written to the PrintStream.
     */
    public void redirectErr(Color textColor) {
        redirectErr(textColor, null);
    }

    /*
     *  Redirect the output from the standard error to the console
     *  using the specified color and PrintStream. When a PrintStream
     *  is specified the message will be added to the Document before
     *  it is also written to the PrintStream.
     */
    public void redirectErr(Color textColor, PrintStream printStream) {
        ConsoleOutputStream cos = new ConsoleOutputStream(textColor, printStream);
        System.setErr(new PrintStream(cos, true));
    }

    /*
     *  To prevent memory from being used up you can control the number of
     *  lines to display in the console
     *
     *  This number can be dynamically changed, but the console will only
     *  be updated the next time the Document is updated.
     */
    public void setMessageLines(int lines) {
        if (limitLinesListener != null) {
            document.removeDocumentListener(limitLinesListener);
        }

        limitLinesListener = new LimitLinesDocumentListener(lines, isAppend);
        document.addDocumentListener(limitLinesListener);
    }

    /*
     *	Class to intercept output from a PrintStream and add it to a Document.
     *  The output can optionally be redirected to a different PrintStream.
     *  The text displayed in the Document can be color coded to indicate
     *  the output source.
     */
    class ConsoleOutputStream extends ByteArrayOutputStream {

        private final String EOL = System.getProperty("line.separator");
        private SimpleAttributeSet attributes, attr_PASS, attr_FAIL;
        private PrintStream printStream;
        private StringBuffer buffer = new StringBuffer(80);
        private boolean isFirstLine;

        /*
         *  Specify the option text color and PrintStream
         */
        public ConsoleOutputStream(Color textColor, PrintStream printStream) {
            if (textColor != null) {
                attributes = new SimpleAttributeSet();
                StyleConstants.setForeground(attributes, textColor);
            }
            attr_PASS = new SimpleAttributeSet();
            attr_FAIL = new SimpleAttributeSet();
            StyleConstants.setForeground(attr_PASS, DARK_GREEN);
            StyleConstants.setForeground(attr_FAIL, Color.RED);
            this.printStream = printStream;
            if (isAppend) {
                isFirstLine = true;
            }
        }

        /*
         *  Override this method to intercept the output text. Each line of text
         *  output will actually involve invoking this method twice:
         *
         *  a) for the actual text message
         *  b) for the newLine string
         *
         *  The message will be treated differently depending on whether the line
         *  will be appended or inserted into the Document
         */
        @Override
        public void flush() {
            String message = toString();

            if (message.length() == 0) {
                return;
            }

            if (isAppend) {
                handleAppend(message);
            } else {
                handleInsert(message);
            }

            reset();
        }

        /*
         *	We don't want to have blank lines in the Document. The first line
         *  added will simply be the message. For additional lines it will be:
         *
         *  newLine + message
         */
        private void handleAppend(String message) {
            //  This check is needed in case the text in the Document has been
            //	cleared. The buffer may contain the EOL string from the previous
            //  message.

            if (document.getLength() == 0) {
                buffer.setLength(0);
            }

            if (EOL.equals(message)) {
                buffer.append(message);
            } else {
                buffer.append(message);
                clearBuffer();
            }

        }

        /*
         *  We don't want to merge the new message with the existing message
         *  so the line will be inserted as:
         *
         *  message + newLine
         */
        private void handleInsert(String message) {
            buffer.append(message);

            if (EOL.equals(message)) {
                clearBuffer();
            }
        }

        /*
         *  The message and the newLine have been added to the buffer in the
         *  appropriate order so we can now update the Document and send the
         *  text to the optional PrintStream.
         */
        private void clearBuffer() {
            //  In case both the standard out and standard err are being redirected
            //  we need to insert a newline character for the first line only

            if (isFirstLine && document.getLength() != 0) {
                buffer.insert(0, "\n");
            }

            isFirstLine = false;
            String line = buffer.toString();

            try {
                if (isAppend) {
                    int offset = document.getLength();
                    document.insertString(offset, line, withDynamicColor(line));
                    textComponent.setCaretPosition(document.getLength());
                } else {
                    document.insertString(0, line, withDynamicColor(line));
                    textComponent.setCaretPosition(0);
                }
            } catch (BadLocationException ble) {
            }

            if (printStream != null) {
                printStream.print(line);
            }

            buffer.setLength(0);
        }

        private SimpleAttributeSet withDynamicColor(String line) {
            try {
                line = Objects.toString(line, "").replaceAll("\r\n", "");
                if (line.startsWith("[PASS]")) {
                    return attr_PASS;
                } else if (line.startsWith("[FAIL]") || line.startsWith("[DEBUG]")) {
                    return attr_FAIL;
                }
            } catch (Exception ex) {
            }
            return attributes;
        }
    }
}

final class LimitLinesDocumentListener implements DocumentListener {

    private static final Logger LOG = Logger.getLogger(LimitLinesDocumentListener.class.getName());

    private int maximumLines;
    private boolean isRemoveFromStart;

    /*
     *  Specify the number of lines to be stored in the Document.
     *  Extra lines will be removed from the start of the Document.
     */
    public LimitLinesDocumentListener(int maximumLines) {
        this(maximumLines, true);
    }

    /*
     *  Specify the number of lines to be stored in the Document.
     *  Extra lines will be removed from the start or end of the Document,
     *  depending on the boolean value specified.
     */
    public LimitLinesDocumentListener(int maximumLines, boolean isRemoveFromStart) {
        setLimitLines(maximumLines);
        this.isRemoveFromStart = isRemoveFromStart;
    }

    /*
     *  Return the maximum number of lines to be stored in the Document
     */
    public int getLimitLines() {
        return maximumLines;
    }

    /*
     *  Set the maximum number of lines to be stored in the Document
     */
    public void setLimitLines(int maximumLines) {
        if (maximumLines < 1) {
            String message = "Maximum lines must be greater than 0";
            throw new IllegalArgumentException(message);
        }

        this.maximumLines = maximumLines;
    }

    //  Handle insertion of new text into the Document
    @Override
    public void insertUpdate(final DocumentEvent e) {
        //  Changes to the Document can not be done within the listener
        //  so we need to add the processing to the end of the EDT

        SwingUtilities.invokeLater(() -> {
            removeLines(e);
        });
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    /*
     *  Remove lines from the Document when necessary
     */
    private void removeLines(DocumentEvent e) {
        //  The root Element of the Document will tell us the total number
        //  of line in the Document.

        Document document = e.getDocument();
        Element root = document.getDefaultRootElement();

        while (root.getElementCount() > maximumLines) {
            if (isRemoveFromStart) {
                removeFromStart(document, root);
            } else {
                removeFromEnd(document, root);
            }
        }
    }

    /*
     *  Remove lines from the start of the Document
     */
    private void removeFromStart(Document document, Element root) {
        Element line = root.getElement(0);
        int end = line.getEndOffset();

        try {
            document.remove(0, end);
        } catch (BadLocationException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /*
     *  Remove lines from the end of the Document
     */
    private void removeFromEnd(Document document, Element root) {
        //  We use start minus 1 to make sure we remove the newline
        //  character of the previous line

        Element line = root.getElement(root.getElementCount() - 1);
        int start = line.getStartOffset();
        int end = line.getEndOffset();

        try {
            document.remove(start - 1, end - start);
        } catch (BadLocationException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

}
