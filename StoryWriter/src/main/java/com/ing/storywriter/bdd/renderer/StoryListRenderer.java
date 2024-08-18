
package com.ing.storywriter.bdd.renderer;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import com.ing.storywriter.bdd.data.Story;

/**
 *
 */
public class StoryListRenderer extends BDDListButton implements ListCellRenderer<Story> {

   
    public StoryListRenderer() {
       super();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Story> list,
            Story value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(getRenderedLable(value, index));     
        if (isSelected ) {
            this.setBackground(new java.awt.Color(197, 178, 178));
        } else {
            this.setBackground(new java.awt.Color(255, 255, 255));
        }
        return this;
    }

    private String getRenderedLable(Story value, int index) {
        return Template.replace("[-NAME-]", value.type + " " + ++index)
                .replace("[-DESC-]", value.name);
    }

}
