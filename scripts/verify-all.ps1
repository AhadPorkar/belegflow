# Full verification using an isolated Docker database; no host Java or Node installation needed.
param([switch]$StrictCoverage)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if ($StrictCoverage) { $env:BF_STRICT_COVERAGE = "true" }
New-Item -ItemType Directory -Force -Path quality-results | Out-Null
# Explicitly remove only the isolated quality stack, never the demo/production stack.
docker compose -f compose.quality.yml down
if ($LASTEXITCODE -ne 0) { throw "Could not prepare the isolated test stack." }
docker compose -f compose.quality.yml up --build --abort-on-container-exit --exit-code-from verifier
$Code = $LASTEXITCODE
docker compose -f compose.quality.yml down
if ($Code -ne 0) { throw "Verification failed (exit $Code). Inspect quality-results and the console output." }
Write-Host "Verification completed. Read quality-results before approving a release."
