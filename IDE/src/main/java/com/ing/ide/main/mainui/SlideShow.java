
package com.ing.ide.main.mainui;

import java.awt.CardLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SlideShow extends JPanel {

    private CardLayout card;

    private final Map<String, JComponent> cards = new HashMap();

    private String currentCard;

    public SlideShow() {
        initComponent();
        currentCard = "TestDesign";
    }

    private void initComponent() {
        card = new CardLayout();
        setLayout(card);
    }

    public void addSlide(String slideName, JComponent component) {
        add(component, slideName);
        cards.put(slideName, component);
    }

    public void showSlide(String slideName) {
        if (!currentCard.equals(slideName)) {
            currentCard = slideName;
            new SlideListener(slideName).start();
        }
    }

    private JComponent getCurrentComponent() {
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            JComponent comp = (JComponent) getComponent(i);
            if (comp.isVisible()) {
                return comp;
            }
        }
        return null;
    }

    public String getCurrentCard() {
        return currentCard;
    }

    class SlideListener implements ActionListener {

        private final int steps = 10;

        private int currentStep = 0;

        private final JComponent currentSlide;
        private final JComponent toSlide;

        private final Timer timer;

        private final String slideName;

        public SlideListener(String slideName) {
            this.slideName = slideName;
            currentSlide = getCurrentComponent();
            toSlide = cards.get(slideName);
            toSlide.setVisible(true);
            timer = new Timer(40, this);
        }

        public void start() {
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            Rectangle bounds = currentSlide.getBounds();
            int shift = bounds.width / this.steps;
            currentSlide.setLocation(bounds.x - shift, bounds.y);
            toSlide.setLocation(bounds.x - shift + bounds.width, bounds.y);
            currentStep++;
            SlideShow.this.repaint();
            if (currentStep == steps) {
                toSlide.setVisible(false);
                card.show(SlideShow.this, slideName);
                timer.stop();
            }
        }
    }
}
