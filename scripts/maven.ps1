$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$version = '3.10.0'
$tools = Join-Path $root '.tools'
$homeDir = Join-Path $tools "apache-maven-$version"
if (!(Test-Path "$homeDir/bin/mvn.cmd")) {
  New-Item -ItemType Directory -Force $tools | Out-Null
  $temp = Join-Path $tools ([Guid]::NewGuid().ToString())
  New-Item -ItemType Directory $temp | Out-Null
  try {
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Invoke-WebRequest $url -OutFile "$temp/maven.zip"
    Invoke-WebRequest "$url.sha512" -OutFile "$temp/checksum"
    $expected = ((Get-Content "$temp/checksum" -Raw).Trim() -split '\s+')[0]
    $actual = (Get-FileHash "$temp/maven.zip" -Algorithm SHA512).Hash
    if ($expected.ToLowerInvariant() -ne $actual.ToLowerInvariant()) { throw 'Maven checksum mismatch.' }
    Expand-Archive "$temp/maven.zip" $tools -Force
  } finally { Remove-Item $temp -Recurse -Force }
}
& "$homeDir/bin/mvn.cmd" @args
exit $LASTEXITCODE
