$ErrorActionPreference = "Stop"
$NDK = "C:\Android\ndk\26.3.11579264\toolchains\llvm\prebuilt\windows-x86_64\bin"
$SRC = "C:\Users\Administrator\touhou-aichat\app\src\main\cpp\native_token.cpp"
$JNILIBS = "C:\Users\Administrator\touhou-aichat\app\src\main\jniLibs"

New-Item -ItemType Directory -Path "$JNILIBS\arm64-v8a" -Force | Out-Null
New-Item -ItemType Directory -Path "$JNILIBS\armeabi-v7a" -Force | Out-Null

Write-Output "=== Building arm64-v8a ==="
& "$NDK\aarch64-linux-android24-clang++.cmd" -shared -fPIC -nostdlib++ -o "$JNILIBS\arm64-v8a\libnative.so" $SRC
Write-Output ("arm64 exit: " + $LASTEXITCODE)

Write-Output "=== Building armeabi-v7a ==="
& "$NDK\armv7a-linux-androideabi24-clang++.cmd" -shared -fPIC -nostdlib++ -o "$JNILIBS\armeabi-v7a\libnative.so" $SRC
Write-Output ("armv7 exit: " + $LASTEXITCODE)

Write-Output "=== Result ==="
Get-ChildItem -Recurse $JNILIBS | Select-Object FullName, Length
