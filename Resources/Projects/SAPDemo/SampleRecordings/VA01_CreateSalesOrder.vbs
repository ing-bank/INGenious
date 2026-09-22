' =====================================================================
' SAP GUI Scripting Tracker recording
' Recorded via: SAP Logon -> Customize Local Layout (Alt+F12) ->
'               Script Recording and Playback -> Record
'
' Sample: VA01 (Create Sales Order), cross-checked in a second,
' parallel session with VA03 (Display Sales Order).
'
' For testing INGenious's SAP recording importer end-to-end. Note: as
' of this build, .vbs is not yet wired into the IDE's
' Tools -> Import SAP Recording menu (only .ps1 and .java/.jsh are) -
' see SapParserFactory.java. This file parses correctly against
' SapParserLangVBScript directly (as SapLanguageParserSessionTest does);
' use VA01_CreateSalesOrder.ps1 to test the import through the running
' app's menu today.
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
If IsObject(WScript) Then
   WScript.ConnectObject session,     "on"
   WScript.ConnectObject application, "on"
End If

session.findById("wnd[0]").maximize
session.findById("wnd[0]/tbar[0]/okcd").text = "VA01"
session.findById("wnd[0]").sendVKey 0

session.startTransaction("VA01")

session.findById("wnd[0]/usr/ctxtVBAK-AUART").text = "OR"
session.findById("wnd[0]/usr/ctxtVBAK-VKORG").text = "1000"
session.findById("wnd[0]/usr/ctxtVBAK-VTWEG").text = "10"
session.findById("wnd[0]/usr/ctxtVBAK-SPART").text = "00"
session.findById("wnd[0]/tbar[0]/btn[0]").press()

session.findById("wnd[0]/usr/ctxtKUAGV-KUNNR").text = "0000100032"
session.findById("wnd[0]/usr/ctxtVBKD-BSTKD").text = "PO-2026-0917"
session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/ctxtRV45A-MABNR[1,0]").text = "MATERIAL-100"
session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").text = "10"
session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]").setFocus()
session.findById("wnd[0]").sendVKey 0

session.findById("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/chkRV45A-SELKZ[6,0]").selected = true

session.findById("wnd[0]/usr/tabsTAXI_TABSTRIP_OVERVIEW/tabpT\11").select()

' --- Cross-check the order in a second, parallel session --------------
session2.findById("wnd[0]/tbar[0]/okcd").text = "VA03"
session2.findById("wnd[0]").sendVKey 0

session2.startTransaction("VA03")

session2.findById("wnd[0]/usr/ctxtVBAK-VBELN").text = "0000012345"
session2.findById("wnd[0]").sendVKey 0
session2.findById("wnd[0]/usr/tabsTAXI_TABSTRIP_OVERVIEW/tabpT\02").select()

' --- Back to the primary session to save the order ---------------------
session.findById("wnd[0]/tbar[0]/btn[11]").press()
