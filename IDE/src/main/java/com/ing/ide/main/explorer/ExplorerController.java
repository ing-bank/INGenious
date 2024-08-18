
package com.ing.ide.main.explorer;


import com.ing.ide.main.explorer.settings.Settings;
import com.ing.ide.main.shr.image.crop.CropUIController;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Utility;
import com.ing.ide.util.WindowMover;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
/**
 *
 * 
 */
public class ExplorerController {

    private final AbstractAction saveCropped, saveScreen, showGallery, showStep, showDefect;
    private AbstractAction action;
    CropUIController cropControl = CropUIController.getInstance();
    private static Robot robot;
    private static final String FORMAT = "png";
    private BaseEditor bedit;
    private ExplorerBar explorerBar;
    private String userDir="";
    private final JFileChooser export = new JFileChooser(this.userDir);
    private Image latestCroped;
    private ImageGallery igalllery = new ImageGallery(FORMAT) {
        @Override
        public void Hide() {
            bedit.setVisible(false);
            explorerBar.setVisible(false);
        }

        @Override
        public void onEmpty() {
            setWindows(false);
        }

        @Override
        public void Show() {
            explorerBar.setVisible(true);
            bedit.setVisible(true);

        }
    };

    /**
     * initiate the controller components
     *
     * @param frame
     */
    public ExplorerController(ExplorerBar frame) throws IOException {
        this.userDir = new File(System.getProperty("user.dir")).getCanonicalPath();
        explorerBar = frame;
        WindowMover.register(frame, frame, WindowMover.MOVE_VERTICAL);
        bedit = BaseEditor.getEditor();
        cropControl.setFrame(frame);
        cropControl.setRightClickDisArm(false);
        export.setLocation(Canvas.Window.screenCenter.width, Canvas.Window.screenCenter.height);
        saveCropped = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (cropControl.getCroppedImage() != null) {
                    if (latestCroped != cropControl.getCroppedImage()) {
                        latestCroped = cropControl.getCroppedImage();
                        saveimage(latestCroped);
                        Notification.show("Image Saved succesfully \nTo view the saved image click on Screenshots");
                    } else {
                        Notification.show("Image already Added!!!");
                    }
                } else {
                    Notification.show("Please Crop any Image to Save!!!");
                }

            }
        };
        saveScreen = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveimage(cropControl.getScreen());
            }
        };
        showGallery = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (igalllery.load(Settings.getScreenShotLoc())) {
                    bedit.setComponent(igalllery);
                    bedit.setIconImage(Canvas.iconToImage(((JButton) e.getSource()).getIcon()));
                } else {
                    ((JButton) e.getSource()).setSelected(false);
                }

            }
        };
        showStep = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                bedit.setComponent(explorerBar.sMainFrame.getTestDesign().getTestCasePanelForExploratory());
                bedit.setTitle("Test Steps");
                bedit.setIconImage(Canvas.iconToImage(((JButton) e.getSource()).getIcon()));
            }

        };
        showDefect = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
//                JPanel p = ReportDefect.getreportDefectPanel(bedit);
//                ReportDefect.setSelectedImages(igalllery.getSelectedImages());
//                ReportDefect.setExplorer(explorerBar);
//                bedit.setComponent(p);
//                bedit.setIconImage(Canvas.iconToImage(((JButton) e.getSource()).getIcon()));
            }

        };

    }

    public void setWindows(boolean visible) {

    }

    /**
     * hides/disposes the UI and controller components
     */
    public void close() {
        explorerBar.sMainFrame.getTestDesign().resetTestCasePanelAfterExploratory();
        bedit.dispose();
    }

    /**
     * saves the image into the local file system
     *
     * @param img
     */
    public void saveimage(Image img) {

        try {
            ImageIO.write((RenderedImage) img, FORMAT, getSaveFile());
        } catch (IOException ex) {
            Logger.getLogger(ExplorerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * initiate the cropping of screen
     */
    public void cropScreen() {
        cropControl.doCrop();
        action = saveCropped;
    }

    /**
     * takes the screen-shot of current screen
     *
     * @param e
     */
    public void saveScreen(ActionEvent e) {
        explorerBar.setVisible(false);
        try {
            Thread.sleep(500);//some time for the bar to hide[in some cases like live meeting]
        } catch (InterruptedException ex) {
            Logger.getLogger(ExplorerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        saveScreen.actionPerformed(e);
        explorerBar.setVisible(true);
    }

    /**
     * hide the cropper UI
     *
     * @param e
     */
    void doneCrop(ActionEvent e) {
        cropControl.cropper.hideCropper();
    }

    /**
     * save the cropped current image
     *
     * @param e
     */
    public void saveCropped(ActionEvent e) {
        action.actionPerformed(e);
    }

    /**
     * displays the image gallery UI
     *
     * @param e
     */
    public void showGallery(ActionEvent e) {
        showGallery.actionPerformed(e);
    }

    /**
     * displays the test steps editor
     *
     * @param evt
     */
    void showStepEdtor(ActionEvent evt) {
        showStep.actionPerformed(evt);
    }

    /**
     * displays the script exporter UI
     *
     * @param evt
     */
    void showScriptExporter(ActionEvent evt) {

        File output = getSaveScriptFile();
        if (output != null) {
            explorerBar.sMainFrame.getStepMap()
                    .convertTestCase(
                            output,
                            explorerBar.testCase);
        }
    }

    /**
     * display the defect report module UI
     *
     * @param evt
     */
    void showDefectReporter(ActionEvent evt) {
        //ReportDefect
        showDefect.actionPerformed(evt);
    }

    /**
     * hide the base window/container
     */
    public void hideBase() {
        bedit.setVisible(false);

    }

    /**
     * capture the current screen using robot
     *
     * @return current screen
     * @throws AWTException
     */
    public BufferedImage getScreen() throws AWTException {
        robot = new Robot();
        return robot.createScreenCapture(new Rectangle(0, 0, Canvas.Window.W, Canvas.Window.H));
    }

    /**
     * compose the file for saving the image
     *
     * @return
     */
    private File getSaveFile() {
        File ssFile = new File(Settings.getScreenShotLoc());
        if (!ssFile.exists()) {
            ssFile.mkdirs();
        }
        return new File(ssFile, getFilename() + "." + FORMAT);
    }

    /**
     * compose and return the file name for the screen-shot
     *
     * @return - file name
     */
    private String getFilename() {
        return "image_" + Utility.getdatetimeString();
    }

    /**
     * prompt for file to save the manual test steps
     *
     * @return - test steps file to save
     */
    private File getSaveScriptFile() {

        File f = new File("ManualTestSteps.csv");
        export.setSelectedFile(f);
        export.setFileFilter(Utility.csvFIlter);
        int loc = export.showSaveDialog(null);
        if (loc == JFileChooser.APPROVE_OPTION) {
            try {
                f = export.getSelectedFile();
            } catch (Exception ex) {
                Logger.getLogger(ExplorerController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            f = null;
        }
        return f;

    }

    /**
     * displays the settings UI window
     */
    void launchSettings() {
    }

}
