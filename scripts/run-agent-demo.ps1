param(
    [ValidateSet("mvc", "webflux")]
    [string] $App = "mvc",
    [int] $Port = 8080,
    [switch] $StopAfterReport
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not $env:JAVA_HOME -or -not (Test-Path $java)) {
    $java = "java"
}

Push-Location $repoRoot
try {
    .\gradlew.bat :reqover-agent:jar ":examples:$App-sample:bootJar"

    $agentJar = Join-Path $repoRoot "reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar"
    if ($App -eq "mvc") {
        $appJar = Join-Path $repoRoot "examples\mvc-sample\build\libs\mvc-sample-0.1.0-SNAPSHOT.jar"
        $include = "io.reqover.example.mvc.auto"
        $endpoint = "http://localhost:$Port/auto/orders/42"
    } else {
        $appJar = Join-Path $repoRoot "examples\webflux-sample\build\libs\webflux-sample-0.1.0-SNAPSHOT.jar"
        $include = "io.reqover.example.webflux.auto"
        $endpoint = "http://localhost:$Port/auto/reactive/orders/42"
    }

    $arguments = @(
        "-javaagent:$agentJar=include=$include",
        "-jar",
        $appJar,
        "--server.port=$Port",
        "--spring.main.banner-mode=off"
    )

    $process = Start-Process -FilePath $java -ArgumentList $arguments -PassThru -WindowStyle Hidden
    try {
        $reportUrl = "http://localhost:$Port/reqover/report"
        $deadline = (Get-Date).AddSeconds(45)
        do {
            try {
                Invoke-RestMethod -Uri $reportUrl -TimeoutSec 2 | Out-Null
                break
            } catch {
                Start-Sleep -Milliseconds 500
            }
        } while ((Get-Date) -lt $deadline)

        Invoke-RestMethod -Uri $endpoint -TimeoutSec 5 | ConvertTo-Json -Depth 5
        Invoke-RestMethod -Uri $reportUrl -TimeoutSec 5 | ConvertTo-Json -Depth 20
        Write-Host "HTML report: http://localhost:$Port/reqover/report.html"
        if (-not $StopAfterReport) {
            Read-Host "Press Enter to stop the sample application"
        }
    } finally {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
} finally {
    Pop-Location
}
