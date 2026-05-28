package com.ing.engine.drivers;

import com.ing.engine.drivers.SAPObject.SAPFindType;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.engine.core.Control;
import com.ing.engine.core.CommandControl;
import java.util.HashMap;
import java.util.Map;


import com.ing.engine.constants.ObjectProperty;
public class SAPObject {
	
	ActiveXComponent session;
	String pageName;
	String objectName;
	SAPFindType findType;
	
	public static HashMap<String, Map<String, Map<String, String>>> dynamicValue = new HashMap<>();
	public static HashMap<String, String> globalDynamicValue = new HashMap<>();
	public static String Action = "";
	
	public enum SAPFindType {
		GLOBAL_OBJECT, DEFAULT;

		public static SAPFindType fromString(String val) {
			switch (val.toLowerCase()) {
			case "globalobject":
				return GLOBAL_OBJECT;
			default:
				return DEFAULT;
			}
		}
	}
	
	public SAPObject(CommandControl cc) {
		super();
	}
	
	public SAPObject(ActiveXComponent Session) {
		this.session = Session;
	}
	
	public void setSession(ActiveXComponent session) {
		this.session = session;

	}
	
	public Dispatch findSAPElement(String objectKey, String pageKey, SAPFindType condition) {
		pageName = pageKey;
		objectName = objectKey;
		findType=condition;
		// Check if object exists in SAP OR before attempting to find properties
		if (getSapObject(pageKey, objectKey) == null) {
			System.out.println("Error: SAP object [" + objectKey + "] not found on page [" + pageKey + "] - possible non-SAP action on SAP browser");
			return null;
		}
		String id=getRuntimeValue(getObjectProperty(pageKey, objectKey, ObjectProperty.Id));	
		String text = getRuntimeValue(getObjectProperty(pageKey, objectKey, ObjectProperty.Text));			
		if (id == null) {
			System.out.println("Error: Element ID is null for object [" + objectKey + "] on page [" + pageKey + "]");
			return null;
		}
		Dispatch parentElement= session.invoke("FindById", id).toDispatch();		
		if(text == null || text.isEmpty())
		{
                 return parentElement;
		}
		else
		{		
		Dispatch childrenElement = Dispatch.call(parentElement, "Children").toDispatch();
		int count = Dispatch.get(childrenElement, "Count").getInt();
		boolean isTextPresent = false;
		for(int i = 0; i < count; i++){
			
			Dispatch child = Dispatch.call(childrenElement, "Item", i).toDispatch();
			String elementText = Dispatch.get(child, "Text").toString();
			if(text.equals(elementText)) {
				isTextPresent = true;
				return child;
			}			
		}
		if(!isTextPresent)
		{
			System.out.println("Warning : Element not exist with text : "+text);
		}
		}
		return null;
	}
	
		
	public String getObjectProperty(String pageName, String objectName, String propertyName) {
		SapORObject sapObj = getSapObject(pageName, objectName);
		if (sapObj == null) {
			System.out.println("Warning: SAP object [" + objectName + "] not found on page [" + pageName + "]. Returning null.");
			return null;
		}
		return sapObj.getAttributeByName(propertyName);
	}
	
	public SapORObject getSapObject(String page, String object) {
		ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
		
		try {
			ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(page);
			ResolvedSapObject resolved = objRep.resolveSapObject(ref, object);
			if (resolved != null && resolved.getGroup() != null && !resolved.getGroup().getObjects().isEmpty()) {
				return (SapORObject) resolved.getGroup().getObjects().get(0);
			}
		} catch (Exception ignore) { }
		
		// Check SAP project OR
		if (objRep.getSapOR() != null) {
			if (objRep.getSapOR().getPageByName(page) != null) {
				ObjectGroup<SapORObject> group = objRep.getSapOR().getPageByName(page).getObjectGroupByName(object);
				if (group != null && !group.getObjects().isEmpty()) {
					return group.getObjects().get(0);
				}
			}
		}
		// Check SAP shared OR
		if (objRep.getSapSharedOR() != null) {
			if (objRep.getSapSharedOR().getPageByName(page) != null) {
				ObjectGroup<SapORObject> group = objRep.getSapSharedOR().getPageByName(page).getObjectGroupByName(object);
				if (group != null && !group.getObjects().isEmpty()) {
					return group.getObjects().get(0);
				}
			}
		}
		return null;
	}

	public ObjectGroup<SapORObject> getSapObjects(String page, String object) {
		ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
		
		try {
			ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(page);
			ResolvedSapObject resolved = objRep.resolveSapObject(ref, object);
			if (resolved != null && resolved.getGroup() != null) {
				return (ObjectGroup<SapORObject>) resolved.getGroup();
			}
		} catch (Exception ignore) { }
		
		if (objRep.getSapOR() != null && objRep.getSapOR().getPageByName(page) != null) {
			ObjectGroup<SapORObject> group = objRep.getSapOR().getPageByName(page).getObjectGroupByName(object);
			if (group != null) {
				return group;
			}
		} else if (objRep.getSapSharedOR() != null && objRep.getSapSharedOR().getPageByName(page) != null) {
			ObjectGroup<SapORObject> group = objRep.getSapSharedOR().getPageByName(page).getObjectGroupByName(object);
			if (group != null) {
				return group;
			}
		}
		System.out.println("Warning: SAP object group [" + object + "] not found on page [" + page + "]. Returning null.");
		return null;
	}

	private String getRuntimeValue(String value) {
		if (value == null) {
			return null;
		}
		if (findType != null && findType.equals(SAPFindType.GLOBAL_OBJECT)) {
			for (String Key : globalDynamicValue.keySet()) {
				value = value.replace(Key, globalDynamicValue.get(Key));
			}
		}
		if (dynamicValue.containsKey(pageName) && dynamicValue.get(pageName).containsKey(objectName)) {
			for (String Key : dynamicValue.get(pageName).get(objectName).keySet()) {
				value = value.replace(Key, dynamicValue.get(pageName).get(objectName).get(Key));
			}
		}
		return value;
	}	
}
