
package com.ing.storywriter.bdd.editor;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Function;
import java.util.stream.Stream;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.ing.storywriter.parser.BDDTokenMaker;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public abstract class StyledEditor extends RSyntaxTextArea {

    DefaultCompletionProvider provider;

    public StyledEditor() {
        provider = new DefaultCompletionProvider();
    }

    static {
        ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance())
                .putMapping(getStyleDoc(), BDDTokenMaker.class.getName());
    }

    private static String getStyleDoc() {
        return "text/gherkin-bdd";
    }

    private void initProvider() {
        provider.clear();
        addToProvider(keywords());
    }

    private Stream<String> keywords() {
        return Stream.of("Feature: ", "Scenario: ", "Background", "Scenario Outline: ", "Given",
                "When", "Then", "But", "And", "Example");
    }

    private CompletionProvider addToProvider(Stream<String> words) {
        words.map(word -> new BasicCompletion(provider, word))
                .forEach(provider::addCompletion);
        return provider;
    }
    private static boolean isSave(KeyEvent ke) {
        return (ke.isMetaDown() || ke.isControlDown()) && ke.getKeyCode() == KeyEvent.VK_S;
    }

    private static boolean isFormat(KeyEvent ke) {
        return ke.isShiftDown() && ke.getKeyCode() == KeyEvent.VK_F;
    }

    public KeyListener updateProviderOnSave() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (isSave(ke)) {
                    onSave();
                }
            }
        };
    }

    abstract public void onSave();

    public void updateAutoComplete() {
        initProvider();
        addToProvider(Stream.of(getText().split("\n"))
                .map(line -> line.trim())
                .filter(line -> !line.startsWith("#"))
                .map(line -> keywords()
                        .map(word -> (Function<String, String>) inLine -> inLine.replaceAll(word, ""))
                        .reduce(Function.identity(), Function::andThen)
                        .apply(line).trim()));
    }

    private KeyListener formatListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (isFormat(ke)) {
                }
            }
        };
    }

    public StyledEditor setup() {
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
          //  e.printStackTrace();
        }
        setCodeFoldingEnabled(true);
        setSyntaxEditingStyle(getStyleDoc());
        initProvider();
        installAutoComplete();
        addKeyListener(updateProviderOnSave());
        setFont(new Font("ING Me", Font.BOLD, 12));
        return this;
    }

    public RTextScrollPane getScrollView() {
        return new RTextScrollPane(this);
    }

    private void installAutoComplete() {
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(this);
    }

}
