$apk = "C:\Users\Administrator\touhou-aichat\app\build\outputs\apk\debug\app-debug.apk"
$jar = "C:\jdk21\bin\jar.exe"

Write-Output "=== APK info ==="
Get-Item $apk | Select-Object FullName, Length, LastWriteTime | Format-List

Write-Output "=== Key entries ==="
& $jar tf $apk | Select-String -Pattern "libnative.so|classes.dex|AndroidManifest.xml|resources.arsc|META-INF/" | Select-Object -First 40

Write-Output "=== icon_l1 drawable count in APK ==="
(& $jar tf $apk | Select-String -Pattern "icon_l1").Count

Write-Output "=== libnative.so (arm64) NEEDED deps ==="
& "C:\Android\ndk\26.3.11579264\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe" -d "C:\Users\Administrator\touhou-aichat\app\src\main\jniLibs\arm64-v8a\libnative.so" | Select-String "NEEDED"

Write-Output "=== native method symbols (arm64) ==="
& "C:\Android\ndk\26.3.11579264\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-nm.exe" -D "C:\Users\Administrator\touhou-aichat\app\src\main\jniLibs\arm64-v8a\libnative.so" | Select-String "xorBytes"
