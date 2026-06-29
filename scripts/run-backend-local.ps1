$ErrorActionPreference = 'Stop'

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')
$envFile = Join-Path $root '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing .env file: $envFile"
}

Get-Content -LiteralPath $envFile |
    Where-Object { $_ -and -not $_.StartsWith('#') } |
    ForEach-Object {
        $parts = $_.Split('=', 2)
        if ($parts.Length -eq 2) {
            Set-Item -Path "Env:$($parts[0])" -Value $parts[1]
        }
    }

$env:JAVA_HOME = 'D:\dev\JDK\jdk-21.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Set-Location -LiteralPath (Join-Path $root 'backend')
& 'D:\dev\apache-maven-3.9.10\bin\mvn.cmd' spring-boot:run

