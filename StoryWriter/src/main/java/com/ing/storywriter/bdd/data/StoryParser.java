package com.ing.storywriter.bdd.data;

import com.ing.storywriter.util.Tools;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StoryParser {
    List<Story> stories;

    public StoryParser(File feature) throws Exception {
        stories = new ArrayList();
        Story s = new Story("Imported");
        s.setData(Tools.readFile(feature));
        stories.add(s);
    }

    public List<Story> stories() {
        return stories;
    }
}
