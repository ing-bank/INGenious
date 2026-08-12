import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class GenerateMacOSIcon {

    private static final Color APP_PURPLE =
        new Color(0x77, 0x24, 0xFF);

    private static final Color APP_PURPLE_LIGHT =
        new Color(0x9B, 0x4D, 0xFF);

    private GenerateMacOSIcon() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println(
                "Usage: java GenerateMacOSIcon.java <logo.png> <output.png>"
            );
            System.exit(1);
        }

        File logoFile = new File(args[0]);
        File outputFile = new File(args[1]);

        if (!logoFile.isFile()) {
            throw new IllegalArgumentException(
                "Logo file does not exist: " + logoFile
            );
        }

        BufferedImage logo = ImageIO.read(logoFile);

        if (logo == null) {
            throw new IllegalArgumentException(
                "Could not read logo image: " + logoFile
            );
        }

        int size = 1024;
        BufferedImage icon =
            new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_ARGB
            );

        Graphics2D graphics = icon.createGraphics();

        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        graphics.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        );

        GradientPaint background = new GradientPaint(
            0,
            0,
            APP_PURPLE,
            size,
            size,
            APP_PURPLE_LIGHT
        );

        graphics.setPaint(background);
        graphics.fillRect(0, 0, size, size);

        GradientPaint highlight = new GradientPaint(
            0,
            0,
            new Color(255, 255, 255, 60),
            0,
            size / 3,
            new Color(255, 255, 255, 0)
        );

        graphics.setPaint(highlight);
        graphics.fillRect(
            0,
            0,
            size,
            size / 2
        );

        int logoSize = (int) (size * 0.7);
        int logoOffset = (size - logoSize) / 2;

        graphics.drawImage(
            logo,
            logoOffset,
            logoOffset,
            logoSize,
            logoSize,
            null
        );

        GradientPaint shadow = new GradientPaint(
            0,
            size - size / 4,
            new Color(0, 0, 0, 0),
            0,
            size,
            new Color(0, 0, 0, 30)
        );

        graphics.setPaint(shadow);
        graphics.fillRect(
            0,
            size - size / 4,
            size,
            size / 4
        );

        graphics.dispose();

        File parent = outputFile.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        ImageIO.write(icon, "png", outputFile);

        System.out.println(
            "Generated macOS icon source: "
                + outputFile.getAbsolutePath()
        );
    }
}
