# =====================================================================
# SAP GUI Scripting Tracker recording (PowerShell)
#
# Sample: VA01 (Create Sales Order), cross-checked in a second,
# parallel session with VA03 (Display Sales Order).
#
# This is the format INGenious's Tools -> Import SAP Recording ->
# PowerShell (.ps1) menu item actually accepts today - see
# SapParserFactory.java / SapParserLangPowerShell.java. Same scenario
# as VA01_CreateSalesOrder.vbs, translated to the Invoke-Method /
# Set-Property COM.ps1 helper idiom.
# =====================================================================

$SapGuiAuto  = [System.Runtime.InteropServices.Marshal]::GetActiveObject("SAPGUI")
$application = $SapGuiAuto.GetScriptingEngine
$connection  = $application.Children.Item(0)
$session     = $connection.Children.Item(0)

$session.startTransaction("VA01")

$ID = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-AUART")
Set-Property -object $ID -propertyName "text" -propertyValue @("OR")

$ID2 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-VKORG")
Set-Property -object $ID2 -propertyName "text" -propertyValue @("1000")

$ID3 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-VTWEG")
Set-Property -object $ID3 -propertyName "text" -propertyValue @("10")

$ID4 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/tbar[0]/btn[0]")
Invoke-Method -object $ID4 -methodName "press"

$ID5 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtKUAGV-KUNNR")
Set-Property -object $ID5 -propertyName "text" -propertyValue @("0000100032")

$ID6 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/txtRV45A-KWMENG[3,0]")
Set-Property -object $ID6 -propertyName "text" -propertyValue @("10")
Invoke-Method -object $ID6 -methodName "setFocus"

$ID7 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]")
Invoke-Method -object $ID7 -methodName "sendVKey" -methodParameter @(0)

$ID8 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/tblSAPMV45ATCTRL_U_ERF_AUFTRAG/chkRV45A-SELKZ[6,0]")
Set-Property -object $ID8 -propertyName "selected" -propertyValue @($true)

$ID9 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/tabsTAXI_TABSTRIP_OVERVIEW/tabpT\11")
Invoke-Method -object $ID9 -methodName "select"

# --- Cross-check the order in a second, parallel session ---------------
$connection2 = $application.Children.Item(0)
$session2    = $connection2.Children.Item(1)
$session2.startTransaction("VA03")

$ID10 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]/usr/ctxtVBAK-VBELN")
Set-Property -object $ID10 -propertyName "text" -propertyValue @("0000012345")

$ID11 = Invoke-Method -object $session2 -methodName "findById" -methodParameter @("wnd[0]")
Invoke-Method -object $ID11 -methodName "sendVKey" -methodParameter @(0)

# --- Back to the primary session to save the order ----------------------
$ID12 = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/tbar[0]/btn[11]")
Invoke-Method -object $ID12 -methodName "press"
