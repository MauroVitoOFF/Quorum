@echo off
rem Avvia server e client di sviluppo (QRM + tutti i moduli) in due tab di Windows Terminal.
rem Doppio clic per usarlo. Il server si ferma scrivendo "stop" nel suo tab.
set "ROOT=%~dp0"
where wt >nul 2>nul
if errorlevel 1 (
  echo Windows Terminal ^(wt^) non trovato: apro due finestre PowerShell.
  start "QRM server" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%'; .\gradlew.bat :qrm-dev:runServer"
  start "QRM client" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%'; Start-Sleep -Seconds 8; .\gradlew.bat :qrm-dev:runClient"
  exit /b
)
wt -w new new-tab --title "QRM server" -d "%ROOT%." powershell -NoExit -Command ".\gradlew.bat :qrm-dev:runServer" ; new-tab --title "QRM client" -d "%ROOT%." powershell -NoExit -Command "Start-Sleep -Seconds 2 \; .\gradlew.bat :qrm-dev:runClient"
