Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strPath = fso.GetParentFolderName(WScript.ScriptFullName)

' Kiem tra file bat
strBat = strPath & "\run_server.bat"
If Not fso.FileExists(strBat) Then
    MsgBox "Khong tim thay file run_server.bat tai: " & vbCrLf & strBat, vbCritical, "LanMonitorFX - Loi"
    WScript.Quit 1
End If

' 0 = an hoan toan cua so console den (Hidden Window), False = khong cho tien trinh ket thuc
WshShell.Run """" & strBat & """", 0, False
