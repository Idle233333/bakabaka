$ErrorActionPreference = 'Stop'
$dir = 'C:\Users\Administrator\Desktop\ai-chat\server'
$p = Start-Process -FilePath 'node' -ArgumentList 'src/index.js' -WorkingDirectory $dir -WindowStyle Hidden -RedirectStandardOutput "$dir\server.log" -RedirectStandardError "$dir\server.err.log" -PassThru
Write-Output ('started pid=' + $p.Id)
