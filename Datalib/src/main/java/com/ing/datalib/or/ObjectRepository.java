
package com.ing.datalib.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageOR;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.web.WebOR;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class ObjectRepository {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final Project sProject;

    private WebOR webOR;
    private MobileOR mobileOR;
    private ImageOR imageOR;

    public ObjectRepository(Project sProject) {
        this.sProject = sProject;
        init();
    }

    private void init() {
        try {
            if (new File(getORLocation()).exists()) {
                webOR = XML_MAPPER.readValue(new File(getORLocation()), WebOR.class);
                webOR.setName(sProject.getName());
            } else {
                webOR = new WebOR(sProject.getName());
            }
            if (new File(getMORLocation()).exists()) {
                mobileOR = XML_MAPPER.readValue(new File(getMORLocation()), MobileOR.class);
                mobileOR.setName(sProject.getName());
            } else {
                mobileOR = new MobileOR(sProject.getName());
            }
            if (new File(getIORLocation()).exists()) {
                imageOR = XML_MAPPER.readValue(new File(getIORLocation()), ImageOR.class);
                imageOR.setName(sProject.getName());
            } else {
                imageOR = new ImageOR(sProject.getName());
            }
            webOR.setObjectRepository(this);
            webOR.setSaved(true);
            mobileOR.setObjectRepository(this);
            imageOR.setObjectRepository(this);
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getORLocation() {
        return sProject.getLocation() + File.separator + "OR.object";
    }

    public String getIORLocation() {
        return sProject.getLocation() + File.separator + "IOR.object";
    }

    public String getMORLocation() {
        return sProject.getLocation() + File.separator + "MOR.object";
    }

    public String getORRepLocation() {
        return sProject.getLocation() + File.separator + "ObjectRepository";
    }

    public String getIORRepLocation() {
        return sProject.getLocation() + File.separator + "ImageObjectRepository";
    }

    public String getMORRepLocation() {
        return sProject.getLocation() + File.separator + "MobileObjectRepository";
    }

    public Project getsProject() {
        return sProject;
    }

    public WebOR getWebOR() {
        return webOR;
    }

    public MobileOR getMobileOR() {
        return mobileOR;
    }

    public ImageOR getImageOR() {
        return imageOR;
    }

    public void save() {
        try {
            if (!webOR.isSaved()) {
                XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getORLocation()), webOR);
            }
            XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getIORLocation()), imageOR);
            XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getMORLocation()), mobileOR);
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean isObjectPresent(String pageName, String objectName) {
        Boolean present = false;
        if (webOR.getPageByName(pageName) != null) {
            present = webOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
        }
        if (!present) {
            if (imageOR.getPageByName(pageName) != null) {
                present = imageOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
            }
        }
        if (!present) {
            if (mobileOR.getPageByName(pageName) != null) {
                present = mobileOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
            }
        }
        return present;
    }

    public void renameObject(ObjectGroup group, String newName) {
        sProject.refactorObjectName(group.getParent().getName(), group.getName(), newName);
    }

    public void renamePage(ORPageInf page, String newName) {
        sProject.refactorPageName(page.getName(), newName);
    }

}
