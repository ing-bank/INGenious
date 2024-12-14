
package com.ing.engine.commands.galenCommands;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galenframework.rainbow4j.Rainbow4J;
import com.galenframework.specs.Spec;
import com.galenframework.validation.ImageComparison;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationObject;
import com.galenframework.validation.ValidationResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class Report extends Command {

    ObjectMapper obMapper = new ObjectMapper();

    public Report(CommandControl cc) {
        super(cc);
    }

    public void onSuccess(Spec spec, ValidationResult result) {
        ValidationErrorException exception = new ValidationErrorException().withValidationObjects(result.getValidationObjects()).withMessage(spec.toText());
        onResult(exception.asValidationResult(spec), Status.PASS);
    }

    public void onError(Spec spec, ValidationResult result) {
        if (result.getError().getImageComparison() != null) {
            onResult(result, Status.FAIL, saveImageComparison(result.getError().getImageComparison()));
        } else {
            onResult(result, Status.FAIL);
        }
    }

    public void onError(Exception ex) {
        Report.updateTestLog(Action, ex.getMessage(), Status.DEBUG);
    }

    private void onResult(ValidationResult result, Status status) {
        if (result.getError().getMessages() != null) {
            for (String message : result.getError().getMessages()) {
                Report.updateTestLog(Action, message, status, getObjectAreas(result.getValidationObjects()));
            }
        }
    }

    private void onResult(ValidationResult result, Status status, List<String> imageList) {
        if (result.getError().getMessages() != null) {
            for (String message : result.getError().getMessages()) {
                Report.updateTestLog(Action, message, status, imageList);
            }
        }
    }

    private List<String> getObjectAreas(List<ValidationObject> vObjects) {
        ArrayList<Map<String, String>> objectList = new ArrayList<>();
        if (vObjects != null) {
            for (ValidationObject vobject : vObjects) {
                if (vobject.getArea() != null) {
                    Map<String, String> obMap = new HashMap<>();
                    obMap.put("name", vobject.getName());
                    obMap.put("area", "["
                            + vobject.getArea().getLeft() + ","
                            + vobject.getArea().getTop() + ","
                            + vobject.getArea().getWidth() + ","
                            + vobject.getArea().getHeight() + "]");
                    objectList.add(obMap);
                }
            }
        }
        try {
            if (!objectList.isEmpty()) {
                return Arrays.asList(obMapper.writeValueAsString(objectList));
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private ArrayList<String> saveImageComparison(ImageComparison imageComparison) {
        ArrayList<String> imageList = new ArrayList<>();
        try {
            imageList.add(saveImageComparison(ObjectName + "-expected", Rainbow4J.loadImage(imageComparison.getSampleFilteredImage().getAbsolutePath())));
            imageList.add(saveImageComparison(ObjectName + "-actual", Rainbow4J.loadImage(imageComparison.getOriginalFilteredImage().getAbsolutePath())));
            imageList.add(saveImageComparison(ObjectName + "-map", Rainbow4J.loadImage(imageComparison.getComparisonMap().getAbsolutePath())));
        } catch (IOException e) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, e);
        }
        return imageList;
    }

    private String saveImageComparison(String name, BufferedImage image) {
        try {
            File file = new File(getImageName(name, 0));
            file.mkdirs();
            Rainbow4J.saveImage(image, file);
            return "./img" + File.separator + file.getName();
        } catch (IOException ex) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String getImageName(String name, int count) {
        String imageName = FilePath.getCurrentResultsPath() + File.separator + "img" + File.separator + name + count + ".png";
        File file = new File(imageName);
        if (file.exists()) {
            return getImageName(name, count++);
        }
        return imageName;
    }
}
