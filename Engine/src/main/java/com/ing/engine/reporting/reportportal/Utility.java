

package com.ing.engine.reporting.reportportal;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Utility {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static String getID(String resp) throws ParseException {
		JSONParser parse = new JSONParser();
		JSONObject jobj = (JSONObject)parse.parse(resp); 
		System.out.println(jobj.get("id"));
		return (String) jobj.get("id");
	}

}
