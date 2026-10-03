$env:JAVA_HOME = "C:\jdk21"
$env:Path = "C:\jdk21\bin;" + $env:Path
$bt = "C:\Android\build-tools\34.0.0"
$apk = "C:\Users\Administrator\touhou-aichat\app\build\outputs\apk\debug\app-debug.apk"

Write-Output "=== apksigner verify ==="
& "$bt\apksigner.bat" verify --print-certs $apk

Write-Output "=== zipalign check ==="
& "$bt\zipalign.exe" -c -v 4 $apk
Write-Output ("zipalign exit: " + $LASTEXITCODE)
