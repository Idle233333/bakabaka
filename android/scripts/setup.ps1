$ErrorActionPreference = "Stop"

Write-Output "=== Extracting Gradle ==="
if (Test-Path C:\gdl) { Remove-Item C:\gdl -Recurse -Force }
New-Item -ItemType Directory -Path C:\gdl -Force | Out-Null
tar -xf C:\gradle.zip -C C:\gdl
$gdir = Get-ChildItem C:\gdl -Directory | Select-Object -First 1
Write-Output ("Gradle dir: " + $gdir.Name)
if (Test-Path C:\gradle) { Remove-Item C:\gradle -Recurse -Force }
Move-Item $gdir.FullName C:\gradle
Write-Output "Gradle installed at C:\gradle"

Write-Output "=== Extracting JDK21 ==="
if (Test-Path C:\jdk21tmp) { Remove-Item C:\jdk21tmp -Recurse -Force }
New-Item -ItemType Directory -Path C:\jdk21tmp -Force | Out-Null
tar -xf C:\jdk21.zip -C C:\jdk21tmp
$jdir = Get-ChildItem C:\jdk21tmp -Directory | Select-Object -First 1
Write-Output ("JDK dir: " + $jdir.Name)
if (Test-Path C:\jdk21) { Remove-Item C:\jdk21 -Recurse -Force }
Move-Item $jdir.FullName C:\jdk21
Write-Output "JDK21 installed at C:\jdk21"

Write-Output "=== Verify ==="
& "C:\jdk21\bin\java.exe" -version 2>&1 | Out-String | Write-Output
