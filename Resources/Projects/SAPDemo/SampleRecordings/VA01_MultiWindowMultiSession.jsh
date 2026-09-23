// =====================================================================
// SAP GUI Scripting recording (Java / JACOB)
//
// Exercises BOTH multi-window (wnd[0]/wnd[1]/wnd[2] popups) AND
// multi-session (session/session2) in one recording - the Java/JACOB
// translation of VA01_MultiWindowMultiSession.ps1. Stress-tests
// SapScriptParser's object-naming disambiguation (w1_/w2_ window
// prefixes combined with sess_s1_ session prefixes).
//
// Session 1 (primary) - VA01 Create Sales Order:
//   wnd[0] main screen -> F4 on Sold-To Party opens wnd[1] (customer
//   search-help popup) -> double-click a result row to select and close
//   the popup -> back to wnd[0] to finish the header/item data.
//
// Session 2 (secondary) - VA02 Change Sales Order:
//   wnd[0] main screen -> Save triggers wnd[1] (incomplete-data log
//   popup) -> closing it without fixing everything triggers a NESTED
//   wnd[2] (Yes/No "exit processing?" confirmation) -> confirm Yes.
//
// For testing INGenious's Tools -> Import SAP Recording -> Java
// (.java, .jsh) feature - see SapParserLangJava.java / SapScriptParser.java.
// =====================================================================

ActiveXComponent SapGuiAuto = new ActiveXComponent("SAPGUI");
Dispatch application = SapGuiAuto.invokeGetProperty("GetScriptingEngine").toDispatch();
Dispatch connection = Dispatch.call(application, "Children", 0).toDispatch();
Dispatch session = Dispatch.call(connection, "Children", 0).toDispatch();

// --- Session 1: VA01, with an F4 search-help popup (wnd[1]) -----------
session.startTransaction("VA01");

obj1 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtVBAK-AUART").toDispatch());
obj1.setProperty("text", "OR");

obj2 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtKUAGV-KUNNR").toDispatch());
obj2.invoke("setFocus");

obj3 = new ActiveXComponent(session.invoke("findById", "wnd[0]").toDispatch());
obj3.invoke("sendVKey", 4);

// --- wnd[1]: customer search-help popup (still session 1) -------------
obj4 = new ActiveXComponent(session.invoke("findById", "wnd[1]/usr/txtV-LOW").toDispatch());
obj4.setProperty("text", "100032");

obj5 = new ActiveXComponent(session.invoke("findById", "wnd[1]/tbar[0]/btn[0]").toDispatch());
obj5.invoke("press");

obj6 = new ActiveXComponent(session.invoke("findById", "wnd[1]/usr/cntlGRID1/shellcont/shell").toDispatch());
obj6.invoke("doubleClick");

// --- back to wnd[0] (session 1) ----------------------------------------
obj7 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/ctxtVBAK-VKORG").toDispatch());
obj7.setProperty("text", "1000");

obj8 = new ActiveXComponent(session.invoke("findById", "wnd[0]/tbar[0]/btn[0]").toDispatch());
obj8.invoke("press");

obj9 = new ActiveXComponent(session.invoke("findById", "wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").toDispatch());
obj9.setProperty("text", "5");
obj9.invoke("setFocus");

obj10 = new ActiveXComponent(session.invoke("findById", "wnd[0]").toDispatch());
obj10.invoke("sendVKey", 0);

// --- Session 2: VA02, with a nested popup (wnd[1] -> wnd[2]) -----------
Dispatch connection2 = Dispatch.call(application, "Children", 0).toDispatch();
Dispatch session2 = Dispatch.call(connection2, "Children", 1).toDispatch();
session2.startTransaction("VA02");

obj11 = new ActiveXComponent(session2.invoke("findById", "wnd[0]/usr/ctxtVBAK-VBELN").toDispatch());
obj11.setProperty("text", "0000012345");

obj12 = new ActiveXComponent(session2.invoke("findById", "wnd[0]").toDispatch());
obj12.invoke("sendVKey", 0);

obj13 = new ActiveXComponent(session2.invoke("findById", "wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").toDispatch());
obj13.setProperty("text", "8");

obj14 = new ActiveXComponent(session2.invoke("findById", "wnd[0]/tbar[0]/btn[11]").toDispatch());
obj14.invoke("press");

// --- wnd[1]: incomplete-data log popup (session 2) ---------------------
obj15 = new ActiveXComponent(session2.invoke("findById", "wnd[1]/tbar[0]/btn[3]").toDispatch());
obj15.invoke("press");

// --- wnd[2]: nested "exit processing?" confirmation (session 2) --------
obj16 = new ActiveXComponent(session2.invoke("findById", "wnd[2]/usr/btnSPOP-OPTION1").toDispatch());
obj16.invoke("press");

// --- Back to the primary session to save the order ----------------------
obj17 = new ActiveXComponent(session.invoke("findById", "wnd[0]/tbar[0]/btn[11]").toDispatch());
obj17.invoke("press");
