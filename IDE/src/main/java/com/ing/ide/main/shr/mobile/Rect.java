
package com.ing.ide.main.shr.mobile;

import com.ing.ide.main.shr.mobile.android.AndroidUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 
 */
public class Rect {

    private static final Pattern BOUNDS_PATTERN = Pattern.compile("\\[-?(\\d+),-?(\\d+)\\]\\[-?(\\d+),-?(\\d+)\\]");

    public static Rect fromString(String bounds) {
        if (bounds != null && !bounds.isEmpty()) {
            Matcher m = BOUNDS_PATTERN.matcher(bounds);
            if (m.matches()) {
                Rect rect = new Rect();
                rect.setX(Integer.parseInt(m.group(1)) * AndroidUtil.get().scaleFactor);
                rect.setY(Integer.parseInt(m.group(2)) * AndroidUtil.get().scaleFactor);
                rect.setWidth((Integer.parseInt(m.group(3)) - Integer.parseInt(m.group(1))) * AndroidUtil.get().scaleFactor);
                rect.setHeight((Integer.parseInt(m.group(4)) - Integer.parseInt(m.group(2))) * AndroidUtil.get().scaleFactor);
                return rect;
            } else {
                throw new RuntimeException("Invalid bounds: " + bounds);
            }
        }
        return null;
    }

    private int x;
    private int y;
    private int width;
    private int height;

    public int getX() {
        return x;
    }

    public void setX(double x) {
        this.x = (int) x;
    }

    public int getY() {
        return y;
    }

    public void setY(double y) {
        this.y = (int) y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = (int) width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = (int) height;
    }

    @Override
    public String toString() {
        return String.format("{x = %d, y = %d, width = %d, height = %d}", x, y, width, height);
    }
}
