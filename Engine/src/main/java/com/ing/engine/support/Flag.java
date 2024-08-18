
package com.ing.engine.support;

/**
 * 
 *
 */
public enum Flag {

    /*
    [offset]
    apply offset(from the image object properties) to the region found on screen
     */
    SET_OFFSET,
    /*
    [offset]
    use match only alternative to SET_OFFSET flag
     */
    REGION_ONLY,
    /*
    [coordinate]
    use static screen coordinates(from the image object properties)
     */
    SET_COORDINATES,
    /*
    [coordinate]
    use match only alternative/defaut to SET_COORDINATES flag
     */
    MATCH_ONLY,
    /*
    [searchmode]
    use image  search only (native image search)
     */
    IMAGE_ONLY,
    /*
    [searchmode]
    use image and text search
     */
    IMAGE_AND_TEXT,
    /*
    [searchmode]
    use text search only (ocr search on screen)
     */
    TEXT_ONLY
}
