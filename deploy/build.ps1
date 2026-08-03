$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

New-Item -ItemType Directory -Force -Path out, data, lib | Out-Null

$SqliteJar = "lib\sqlite-jdbc-3.49.1.0.jar"
$GsonJar = "lib\gson-2.11.0.jar"

if (-not (Test-Path $SqliteJar)) {
    Write-Host "下载 SQLite JDBC..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar" -OutFile $SqliteJar
}

if (-not (Test-Path $GsonJar)) {
    Write-Host "下载 Gson..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" -OutFile $GsonJar
}

Write-Host "编译 Java..."
$Cp = "$SqliteJar;$GsonJar"
javac -encoding UTF-8 -cp $Cp -d out `
  src\sqlite\User.java `
  src\sqlite\UserDao.java `
  src\sqlite\ApiResponse.java `
  src\sqlite\UserApiServer.java

jar cfe user-api.jar sqlite.UserApiServer -C out .

Write-Host "构建完成: user-api.jar"
Write-Host "启动命令: java -cp user-api.jar;lib\* sqlite.UserApiServer 8080"
