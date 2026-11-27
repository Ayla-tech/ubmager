# Menjalankan UBMager dengan database H2 in-memory + data demo untuk uji UI lokal.
# Skema: src/test/resources/schema-demo-ui.sql
# Data : src/test/resources/demo-ui.sql
# Akun demo (password sama): budi / sari / rina, password: demo12345
#
# Contoh:
#   .\tools\ujicoba-ui.ps1
#   .\tools\ujicoba-ui.ps1 -Port 8099
param(
    [int]$Port = 8098
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

$pemakai = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($pemakai) {
    Write-Host "Port $Port sudah dipakai (PID $($pemakai.OwningProcess -join ', '))." -ForegroundColor Red
    Write-Host "Hentikan proses itu atau jalankan skrip ini dengan -Port lain, misalnya -Port 8099." -ForegroundColor Yellow
    exit 1
}

Write-Host 'Menyiapkan classpath...' -ForegroundColor Cyan
if (-not (Test-Path 'target\cp.txt')) {
    & .\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
& .\mvnw.cmd -q test-compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$cp = 'target\classes;target\test-classes;' + (Get-Content 'target\cp.txt' -Raw).Trim()

$argumen = @(
    '-cp', $cp,
    'com.example.ubmager.UbmagerApplication',
    "--server.port=$Port",
    '--spring.datasource.url=jdbc:h2:mem:uidemo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1',
    '--spring.datasource.username=sa',
    '--spring.datasource.password=',
    '--spring.datasource.driver-class-name=org.h2.Driver',
    '--spring.jpa.hibernate.ddl-auto=none',
    '--spring.jpa.show-sql=false',
    '--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect',
    '--spring.sql.init.mode=always',
    '--spring.sql.init.schema-locations=classpath:schema-demo-ui.sql',
    '--spring.sql.init.data-locations=classpath:demo-ui.sql'
)

Write-Host ''
Write-Host "Aplikasi demo: http://127.0.0.1:$Port" -ForegroundColor Green
Write-Host 'Halaman uji  : /login, /jasa, /jasa/PST001, /pemesanan, /pemesanan?peran=penyedia'
Write-Host '               /pembayaran, /pembayaran?peran=penyedia, /pembayaran/PBY001'
Write-Host 'Akun demo    : budi (pembeli), sari (penyedia), rina (penyedia) - password: demo12345'
Write-Host 'Data hanya di memori dan kembali kosong setiap kali skrip dihentikan (Ctrl+C).'
Write-Host ''

& java @argumen
