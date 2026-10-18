$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!(Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install Docker Desktop with Compose first.' }
if (!(Test-Path .env)) {
  $bytes = New-Object byte[] 32
  $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
  try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
  $secret = -join ($bytes | ForEach-Object { $_.ToString('x2') })
  "BF_DATABASE_PASSWORD=$secret`nBF_DEMO=true`nBF_COOKIE_SECURE=false" | Set-Content .env -Encoding ASCII
}
if (Select-String .env -Pattern 'REPLACE_WITH' -Quiet) { throw 'Replace the placeholder secret in .env first.' }
docker compose up --build -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Open http://localhost:8080. Finance: finance@belegflow.demo / BelegFlow-Demo-2026!'
Write-Host 'Local demo only. Never expose demo credentials to the public internet.'
