# =====================================================================
# SAP GUI Scripting Tracker recording (PowerShell)
#
# Exercises BOTH multi-window (wnd[0]/wnd[1]/wnd[2] popups) AND
# multi-session (session/session2) in one recording - a stress test for
# SapScriptParser's object-naming disambiguation (w1_/w2_ window
# prefixes combined with sess_s1_ session prefixes).
#
# Session 1 (primary) - VA01 Create Sales Order:
#   wnd[0] main screen -> F4 on Sold-To Party opens wnd[1] (customer
#   search-help popup) -> double-click a result row to select and close
#   the popup -> back to wnd[0] to finish the header/item data.
#
# Session 2 (secondary) - VA02 Change Sales Order:
#   wnd[0] main screen -> Save triggers wnd[1] (incomplete-data log
#   popup) -> closing it without fixing everything triggers a NESTED
#   wnd[2] (Yes/No "exit processing?" confirmation) -> confirm Yes.
#
# For testing INGenious's Tools -> Import SAP Recording -> PowerShell
# (.ps1) feature - see SapParserLangPowerShell.java / SapScriptParser.java.
# =====================================================================

$SapGuiAuto  = [System.Runtime.InteropServices.Marshal]::GetActiveObject("SAPGUI")
$application = $SapGuiAuto.GetScriptingEngine
$connection  = $application.Children.Item(0)
$session     = $connection.Children.Item(0)

# --- Session 1: VA01, with an F4 search-help popup (wnd[1]) -----------
$session.startTransaction("VA01")

$ID = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-AUART")
Set-Property -object $ID -propertyName "text" -propertyValue @("OR")

$ID2 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtKUAGV-KUNNR")
Invoke-Method -object $ID2 -methodName "setFocus"

$ID3 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]")
Invoke-Method -object $ID3 -methodName "sendVKey" -methodParameter @(4)

# --- wnd[1]: customer search-help popup (still session 1) -------------
$ID4 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[1]/usr/txtV-LOW")
Set-Property -object $ID4 -propertyName "text" -propertyValue @("100032")

$ID5 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[1]/tbar[0]/btn[0]")
Invoke-Method -object $ID5 -methodName "press"

$ID6 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[1]/usr/cntlGRID1/shellcont/shell")
Invoke-Method -object $ID6 -methodName "doubleClick"

# --- back to wnd[0] (session 1) ----------------------------------------
$ID7 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-VKORG")
Set-Property -object $ID7 -propertyName "text" -propertyValue @("1000")

$ID8 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/tbar[0]/btn[0]")
Invoke-Method -object $ID8 -methodName "press"

$ID9 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]")
Set-Property -object $ID9 -propertyName "text" -propertyValue @("5")
Invoke-Method -object $ID9 -methodName "setFocus"

$ID10 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]")
Invoke-Method -object $ID10 -methodName "sendVKey" -methodParameter @(0)

# --- Session 2: VA02, with a nested popup (wnd[1] -> wnd[2]) -----------
$connection2 = $application.Children.Item(0)
$session2    = $connection2.Children.Item(1)
$session2.startTransaction("VA02")

$ID11 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-VBELN")
Set-Property -object $ID11 -propertyName "text" -propertyValue @("0000012345")

$ID12 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]")
Invoke-Method -object $ID12 -methodName "sendVKey" -methodParameter @(0)

$ID13 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]")
Set-Property -object $ID13 -propertyName "text" -propertyValue @("8")

$ID14 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]/tbar[0]/btn[11]")
Invoke-Method -object $ID14 -methodName "press"

# --- wnd[1]: incomplete-data log popup (session 2) ---------------------
$ID15 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[1]/tbar[0]/btn[3]")
Invoke-Method -object $ID15 -methodName "press"

# --- wnd[2]: nested "exit processing?" confirmation (session 2) --------
$ID16 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[2]/usr/btnSPOP-OPTION1")
Invoke-Method -object $ID16 -methodName "press"

# --- Back to the primary session to save the order ----------------------
$ID17 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/tbar[0]/btn[11]")
Invoke-Method -object $ID17 -methodName "press"
