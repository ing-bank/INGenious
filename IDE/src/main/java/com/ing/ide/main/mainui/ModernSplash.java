package com.ing.ide.main.mainui;

import com.ing.ide.main.utils.AppIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Modern splash screen with animated circular progress ring, 
 * percentage display, and loading step indicators.
 * Features a smooth purple gradient theme matching INGenious branding.
 */
public class ModernSplash extends JFrame {

    private static final long serialVersionUID = 1L;
    
    // Theme colors
    private static final Color ING_PURPLE = Color.decode("#7724FF");
    private static final Color ING_PURPLE_LIGHT = Color.decode("#A366FF");
    private static final Color ING_PURPLE_DARK = Color.decode("#5A10CC");
    private static final Color BACKGROUND_COLOR = Color.decode("#1E1E2E");
    private static final Color TEXT_COLOR = Color.decode("#FFFFFF");
    private static final Color STEP_INACTIVE_COLOR = Color.decode("#666680");
    private static final Color STEP_ACTIVE_COLOR = Color.decode("#BB86FC");
    
    // Layout constants
    private static final int SPLASH_WIDTH = 480;
    private static final int SPLASH_HEIGHT = 400;
    private static final int RING_SIZE = 180;
    private static final int RING_THICKNESS = 8;
    private static final int LOGO_SIZE = 120;
    
    // Animation
    private int currentProgress = 0;
    private int targetProgress = 0;
    private float animatedProgress = 0f;
    private String currentStep = "Initializing...";
    private Timer animationTimer;
    private float pulsePhase = 0f;
    
    // Components
    private BufferedImage logoImage;
    private SplashPanel splashPanel;
    
    // Loading steps with their progress thresholds
    private static final String[] LOADING_STEPS = {
        "Initializing UI...",
        "Loading plugins...",
        "Setting up workspace...",
        "Loading project data...",
        "Preparing editor...",
        "Ready"
    };
    private static final int[] STEP_THRESHOLDS = {0, 15, 35, 55, 75, 95};
    
    public ModernSplash() {
        super("INGenious Playwright Studio");
        initComponents();
        startAnimation();
    }
    
    private void initComponents() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setSize(SPLASH_WIDTH, SPLASH_HEIGHT);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        
        // Load the INGenious logo image for splash display
        logoImage = AppIcon.getLogoImage(LOGO_SIZE);
        if (logoImage == null) {
            // Fallback to direct resource load
            try {
                logoImage = ImageIO.read(getClass().getResource("/ui/resources/favicon.png"));
            } catch (Exception e) {
                logoImage = null;
            }
        }
        
        // Set window icon using multi-resolution AppIcon
        AppIcon.applyTo(this);
        
        // Create the splash panel
        splashPanel = new SplashPanel();
        setContentPane(splashPanel);
        
        // Make window shape rounded
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, SPLASH_WIDTH, SPLASH_HEIGHT, 24, 24));
    }
    
    private void startAnimation() {
        animationTimer = new Timer(16, e -> {
            // Smooth but responsive progress animation
            float diff = targetProgress - animatedProgress;
            animatedProgress += diff * 0.15f;  // Faster animation to keep up with progress
            
            // Snap to target when very close
            if (Math.abs(diff) < 0.5f) {
                animatedProgress = targetProgress;
            }
            
            // Pulse animation
            pulsePhase += 0.05f;
            if (pulsePhase > Math.PI * 2) {
                pulsePhase = 0;
            }
            
            // Update current step based on progress
            for (int i = STEP_THRESHOLDS.length - 1; i >= 0; i--) {
                if (targetProgress >= STEP_THRESHOLDS[i]) {
                    currentStep = LOADING_STEPS[i];
                    break;
                }
            }
            
            splashPanel.repaint();
        });
        animationTimer.start();
    }
    
    public void progressed(int val) {
        targetProgress = Math.min(100, Math.max(0, val));
        currentProgress = val;
    }
    
    public void setStep(String step) {
        currentStep = step;
    }
    
    @Override
    public void dispose() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.dispose();
    }
    
    /**
     * Custom panel that draws the modern splash content
     */
    private class SplashPanel extends JPanel {
        
        private static final long serialVersionUID = 1L;
        
        public SplashPanel() {
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Enable anti-aliasing for smooth rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw background with gradient
            drawBackground(g2d);
            
            // Draw progress ring
            drawProgressRing(g2d);
            
            // Draw logo in center
            drawLogo(g2d);
            
            // Draw percentage
            drawPercentage(g2d);
            
            // Draw app name
            drawAppName(g2d);
            
            // Draw current step
            drawCurrentStep(g2d);
            
            // Draw step indicators
            drawStepIndicators(g2d);
            
            g2d.dispose();
        }
        
        private void drawBackground(Graphics2D g2d) {
            // Dark gradient background
            GradientPaint gradient = new GradientPaint(
                0, 0, BACKGROUND_COLOR,
                0, getHeight(), Color.decode("#15151F")
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            
            // Subtle purple glow at top
            float glowIntensity = (float) (0.15 + 0.05 * Math.sin(pulsePhase));
            RadialGradientPaint glow = new RadialGradientPaint(
                getWidth() / 2f, 100,
                200,
                new float[]{0f, 1f},
                new Color[]{
                    new Color(ING_PURPLE.getRed(), ING_PURPLE.getGreen(), ING_PURPLE.getBlue(), (int)(glowIntensity * 255)),
                    new Color(0, 0, 0, 0)
                }
            );
            g2d.setPaint(glow);
            g2d.fillRect(0, 0, getWidth(), 200);
            
            // Border
            g2d.setColor(new Color(119, 36, 255, 80));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
        }
        
        private void drawProgressRing(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int centerY = 130;
            
            // Background ring (track)
            g2d.setColor(Color.decode("#2A2A3A"));
            g2d.setStroke(new BasicStroke(RING_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Ellipse2D.Double(
                centerX - RING_SIZE / 2.0,
                centerY - RING_SIZE / 2.0,
                RING_SIZE,
                RING_SIZE
            ));
            
            // Progress arc with gradient
            if (animatedProgress > 0) {
                // Create gradient for progress arc
                GradientPaint arcGradient = new GradientPaint(
                    centerX - RING_SIZE / 2f, centerY,
                    ING_PURPLE_LIGHT,
                    centerX + RING_SIZE / 2f, centerY,
                    ING_PURPLE
                );
                g2d.setPaint(arcGradient);
                g2d.setStroke(new BasicStroke(RING_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                double extent = -(animatedProgress / 100.0) * 360;
                Arc2D arc = new Arc2D.Double(
                    centerX - RING_SIZE / 2.0,
                    centerY - RING_SIZE / 2.0,
                    RING_SIZE,
                    RING_SIZE,
                    90,
                    extent,
                    Arc2D.OPEN
                );
                g2d.draw(arc);
                
                // Glow effect on leading edge
                if (animatedProgress < 100) {
                    double angle = Math.toRadians(90 + extent);
                    float glowX = (float) (centerX + (RING_SIZE / 2.0) * Math.cos(angle));
                    float glowY = (float) (centerY - (RING_SIZE / 2.0) * Math.sin(angle));
                    
                    RadialGradientPaint edgeGlow = new RadialGradientPaint(
                        glowX, glowY, 15,
                        new float[]{0f, 1f},
                        new Color[]{
                            new Color(ING_PURPLE_LIGHT.getRed(), ING_PURPLE_LIGHT.getGreen(), ING_PURPLE_LIGHT.getBlue(), 180),
                            new Color(0, 0, 0, 0)
                        }
                    );
                    g2d.setPaint(edgeGlow);
                    g2d.fillOval((int)glowX - 15, (int)glowY - 15, 30, 30);
                }
            }
        }
        
        private void drawLogo(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int centerY = 130;
            
            if (logoImage != null) {
                // Draw logo with slight pulse effect
                float scale = (float) (1.0 + 0.02 * Math.sin(pulsePhase * 2));
                int scaledSize = (int) (LOGO_SIZE * scale);
                int x = centerX - scaledSize / 2;
                int y = centerY - scaledSize / 2;
                
                g2d.drawImage(logoImage, x, y, scaledSize, scaledSize, null);
            } else {
                // Fallback: draw a circle with "IN" text
                g2d.setColor(ING_PURPLE);
                int size = LOGO_SIZE - 20;
                g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);
                g2d.setColor(TEXT_COLOR);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "IN";
                int textX = centerX - fm.stringWidth(text) / 2;
                int textY = centerY + fm.getAscent() / 2 - 4;
                g2d.drawString(text, textX, textY);
            }
        }
        
        private void drawPercentage(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int y = 245;
            
            // Draw percentage text
            String percentText = String.format("%d%%", (int) animatedProgress);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
            g2d.setColor(TEXT_COLOR);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = centerX - fm.stringWidth(percentText) / 2;
            g2d.drawString(percentText, textX, y);
        }
        
        private void drawAppName(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int y = 280;
            
            // App name with gradient
            String appName = "INGenious Playwright Studio";
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            
            GradientPaint textGradient = new GradientPaint(
                centerX - fm.stringWidth(appName) / 2f, y,
                ING_PURPLE_LIGHT,
                centerX + fm.stringWidth(appName) / 2f, y,
                ING_PURPLE
            );
            g2d.setPaint(textGradient);
            g2d.drawString(appName, centerX - fm.stringWidth(appName) / 2, y);
        }
        
        private void drawCurrentStep(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int y = 310;
            
            // Current loading step
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2d.setColor(STEP_ACTIVE_COLOR);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = centerX - fm.stringWidth(currentStep) / 2;
            g2d.drawString(currentStep, textX, y);
        }
        
        private void drawStepIndicators(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int y = 350;
            int dotSize = 8;
            int spacing = 16;
            int totalWidth = (LOADING_STEPS.length - 1) * spacing;
            int startX = centerX - totalWidth / 2;
            
            for (int i = 0; i < LOADING_STEPS.length; i++) {
                int x = startX + i * spacing;
                boolean isActive = targetProgress >= STEP_THRESHOLDS[i];
                boolean isCurrent = i < LOADING_STEPS.length - 1 
                    ? targetProgress >= STEP_THRESHOLDS[i] && targetProgress < STEP_THRESHOLDS[i + 1]
                    : targetProgress >= STEP_THRESHOLDS[i];
                
                if (isCurrent) {
                    // Current step - larger with glow
                    g2d.setColor(ING_PURPLE_LIGHT);
                    float pulse = (float) (1.0 + 0.3 * Math.sin(pulsePhase * 3));
                    int pulsedSize = (int) (dotSize * pulse);
                    g2d.fillOval(x - pulsedSize / 2, y - pulsedSize / 2, pulsedSize, pulsedSize);
                    
                    // Glow
                    RadialGradientPaint glow = new RadialGradientPaint(
                        x, y, 12,
                        new float[]{0f, 1f},
                        new Color[]{
                            new Color(ING_PURPLE_LIGHT.getRed(), ING_PURPLE_LIGHT.getGreen(), ING_PURPLE_LIGHT.getBlue(), 100),
                            new Color(0, 0, 0, 0)
                        }
                    );
                    g2d.setPaint(glow);
                    g2d.fillOval(x - 12, y - 12, 24, 24);
                } else if (isActive) {
                    // Completed step
                    g2d.setColor(ING_PURPLE);
                    g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
                } else {
                    // Inactive step
                    g2d.setColor(STEP_INACTIVE_COLOR);
                    g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
                }
            }
        }
    }
}
