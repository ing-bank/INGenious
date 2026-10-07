' =====================================================================
' SAP GUI Scripting Tracker recording (VBScript)
'
' Exercises BOTH multi-window (wnd[0]/wnd[1]/wnd[2] popups) AND
' multi-session (session/session2) in one recording - the VBScript
' translation of VA01_MultiWindowMultiSession.ps1/.jsh. Stress-tests
' SapScriptParser's object-naming disambiguation (w1_/w2_ window
' prefixes combined with sess_s1_ session prefixes).
'
' Session 1 (primary) - VA01 Create Sales Order:
'   wnd[0] main screen -> F4 on Sold-To Party opens wnd[1] (customer
'   search-help popup) -> double-click a result row to select and close
'   the popup -> back to wnd[0] to finish the header/item data.
'
' Session 2 (secondary) - VA02 Change Sales Order:
'   wnd[0] main screen -> Save triggers wnd[1] (incomplete-data log
'   popup) -> closing it without fixing everything triggers a NESTED
'   wnd[2] (Yes/No "exit processing?" confirmation) -> confirm Yes.
'
' For testing INGenious's Tools -> Import SAP Recording -> VBScript
' (.vbs, .vba) feature - see SapParserLangVBScript.java / SapScriptParser.java.
' =====================================================================

If Not IsObject(application) Then
   Set SapGuiAuto  = GetObject("SAPGUI")
   Set application = SapGuiAuto.GetScriptingEngine
End If
If Not IsObject(connection) Then
   Set connection = application.Children(0)
End If
If Not IsObject(session) Then
   Set session    = connection.Children(0)
End If

' --- Session 1: VA01, with an F4 search-help popup (wnd[1]) -----------
session.startTransaction("VA01")

session.findById("wnd[0]/usr/ctxtVBAK-AUART").text = "OR"

session.findById("wnd[0]/usr/ctxtKUAGV-KUNNR").setFocus()

session.findById("wnd[0]").sendVKey 4

' --- wnd[1]: customer search-help popup (still session 1) -------------
session.findById("wnd[1]/usr/txtV-LOW").text = "100032"

session.findById("wnd[1]/tbar[0]/btn[0]").press()

session.findById("wnd[1]/usr/cntlGRID1/shellcont/shell").doubleClick()

' --- back to wnd[0] (session 1) ----------------------------------------
session.findById("wnd[0]/usr/ctxtVBAK-VKORG").text = "1000"

session.findById("wnd[0]/tbar[0]/btn[0]").press()

session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").text = "5"
session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").setFocus()

session.findById("wnd[0]").sendVKey 0

' --- Session 2: VA02, with a nested popup (wnd[1] -> wnd[2]) -----------
session2.startTransaction("VA02")

session2.findById("wnd[0]/usr/ctxtVBAK-VBELN").text = "0000012345"

session2.findById("wnd[0]").sendVKey 0

session2.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").text = "8"

session2.findById("wnd[0]/tbar[0]/btn[11]").press()

' --- wnd[1]: incomplete-data log popup (session 2) ---------------------
session2.findById("wnd[1]/tbar[0]/btn[3]").press()

' --- wnd[2]: nested "exit processing?" confirmation (session 2) --------
session2.findById("wnd[2]/usr/btnSPOP-OPTION1").press()

' --- Back to the primary session to save the order ----------------------
session.findById("wnd[0]/tbar[0]/btn[11]").press()
