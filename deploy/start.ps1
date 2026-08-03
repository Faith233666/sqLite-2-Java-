$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

& "$Root\deploy\build.ps1"

$env:DB_PATH = Join-Path $Root "data\demo.db"
$env:WEB_ROOT = Join-Path $Root "web"
$Port = if ($env:PORT) { $env:PORT } else { "8080" }

Write-Host "启动服务: http://localhost:$Port"
java -cp "user-api.jar;lib\*" sqlite.UserApiServer $Port
