$env:JAVA_HOME = "C:\jdk21"
$env:Path = "C:\jdk21\bin;C:\gradle\bin;" + $env:Path
Set-Location "C:\Users\Administrator\touhou-aichat"
& "C:\gradle\bin\gradle.bat" assembleDebug --no-daemon --console=plain
exit $LASTEXITCODE
