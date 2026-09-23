// =====================================================================
// SAP GUI Scripting recording (Java / JACOB)
//
// Sample: VA01 (Create Sales Order), cross-checked in a second,
// parallel session with VA03 (Display Sales Order).
//
// For testing INGenious's Tools -> Import SAP Recording -> Java
// (.java, .jsh) feature - see SapParserLangJava.java. Same scenario as
// VA01_CreateSalesOrder.vbs / .ps1, translated to the JACOB
// ActiveXComponent idiom (new ActiveXComponent(session.invoke(...)),
// obj.invoke(...), obj.setProperty(...)).
//
// Note: no tab-select step here (unlike the .vbs sample) - the Java
// parser has the same gap as the PowerShell one: a parameterless
// .invoke("select") never reaches addAction() in
// SapParserLangJava.parseInvokeMethod's "select" case, so it wouldn't
// import as a step anyway.
// =====================================================================

ActiveXComponent SapGuiAuto = new ActiveXComponent("SAPGUI");
Dispatch application = SapGuiAuto.invokeGetProperty("GetScriptingEngine").toDispatch();
Dispatch connection = Dispatch.call(application, "Children", 0).toDispatch();
Dispatch session = Dispatch.call(connection, "Children", 0).toDispatch();

session.startTransaction("VA01");

obj1 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtVBAK-AUART").toDispatch());
obj1.setProperty("text", "OR");

obj2 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtVBAK-VKORG").toDispatch());
obj2.setProperty("text", "1000");

obj3 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtVBAK-VTWEG").toDispatch());
obj3.setProperty("text", "10");

obj4 = new ActiveXComponent(session.invoke("findById", "wnd[0]/tbar[0]/btn[0]").toDispatch());
obj4.invoke("press");

obj5 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtKUAGV-KUNNR").toDispatch());
obj5.setProperty("text", "0000100032");

obj6 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").toDispatch());
obj6.setProperty("text", "10");
obj6.invoke("setFocus");

obj7 = new ActiveXComponent(session.invoke("findById", "wnd[0]").toDispatch());
obj7.invoke("sendVKey", 0);

obj8 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/chkRV45A-SELKZ[6,0]").toDispatch());
obj8.setProperty("selected", true);

// --- Cross-check the order in a second, parallel session ---------------
session2.startTransaction("VA03");

obj9 = new ActiveXComponent(session2.invoke("findById", "wnd[0]/usr/ctxtVBAK-VBELN").toDispatch());
obj9.setProperty("text", "0000012345");

obj10 = new ActiveXComponent(session2.invoke("findById", "wnd[0]").toDispatch());
obj10.invoke("sendVKey", 0);

// --- Back to the primary session to save the order ----------------------
obj11 = new ActiveXComponent(session.invoke("findById", "wnd[0]/tbar[0]/btn[11]").toDispatch());
obj11.invoke("press");
