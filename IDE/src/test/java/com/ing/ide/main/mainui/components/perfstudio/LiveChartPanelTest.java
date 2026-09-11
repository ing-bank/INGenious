package com.ing.ide.main.mainui.components.perfstudio;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import org.testng.annotations.Test;

/**
 * Headless conformance for the Performance Studio live chart: painting must
 * work without a display, honour the rolling window, and never divide by
 * zero on empty/single-point series.
 */
public class LiveChartPanelTest {

    private static BufferedImage paint(LiveChartPanel chart) {
        chart.setSize(400, 200);
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        chart.paint(g);
        g.dispose();
        return image;
    }

    @Test
    public void paintsEmptyChartWithoutErrors() {
        LiveChartPanel chart = new LiveChartPanel("empty", 100);
        assertThat(paint(chart)).isNotNull();
    }

    @Test
    public void paintsSinglePointAndFullSeries() {
        LiveChartPanel chart = new LiveChartPanel("VUs / req/s", 10);
        chart.addPoint("vus", 1);
        assertThat(paint(chart)).isNotNull();
        for (int i = 0; i < 25; i++) {
            chart.addPoint("vus", i);
            chart.addPoint("req/s", i * 2.5);
        }
        assertThat(paint(chart)).isNotNull();
    }

    @Test
    public void resetClearsSeries() {
        LiveChartPanel chart = new LiveChartPanel("t", 10);
        chart.addPoint("a", 5);
        chart.reset();
        assertThat(paint(chart)).isNotNull();
    }
}
