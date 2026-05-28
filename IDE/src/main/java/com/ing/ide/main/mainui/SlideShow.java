
package com.ing.ide.main.mainui;

import java.awt.CardLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SlideShow extends JPanel {

    private CardLayout card;

    private final Map<String, JComponent> cards = new HashMap();

    private String currentCard;
    
    /**
     * Listener for slide change events.
     * Called before switching away from a slide.
     */
    private final List<SlideChangeListener> slideChangeListeners = new ArrayList<>();

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
            // Notify listeners before switching away from the current slide
            notifySlideLeaving(currentCard);
            
            currentCard = slideName;
            new SlideListener(slideName).start();
        }
    }
    
    /**
     * Registers a listener to be notified when leaving a slide.
     */
    public void addSlideChangeListener(SlideChangeListener listener) {
        slideChangeListeners.add(listener);
    }
    
    /**
     * Unregisters a listener.
     */
    public void removeSlideChangeListener(SlideChangeListener listener) {
        slideChangeListeners.remove(listener);
    }
    
    /**
     * Notifies all listeners that we're leaving the specified slide.
     */
    private void notifySlideLeaving(String slideName) {
        for (SlideChangeListener listener : slideChangeListeners) {
            listener.onSlideLeaving(slideName);
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
    
    /**
     * Listener interface for slide change events.
     * Allows components to be notified when leaving a slide.
     */
    public interface SlideChangeListener {
        /**
         * Called when leaving a slide (before switching to a new one).
         * 
         * @param slideName the name of the slide being left
         */
        void onSlideLeaving(String slideName);
    }
}
