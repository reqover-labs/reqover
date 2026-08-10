param(
    [ValidateSet("mvc", "webflux")]
    [string] $App = "mvc",
    [ValidateRange(1, 65535)]
    [int] $Port = 8080,
    [switch] $StopAfterReport
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$java = "java"
if ($env:JAVA_HOME) {
    $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $javaFromHome) {
        $java = $javaFromHome
    }
}

Push-Location $repoRoot
try {
    .\gradlew.bat :reqover-agent:shadowJar ":examples:$App-sample:bootJar"

    $agentJar = Join-Path $repoRoot "reqover-agent\build\libs\reqover-agent-0.1.0.jar"
    if ($App -eq "mvc") {
        $appJar = Join-Path $repoRoot "examples\mvc-sample\build\libs\mvc-sample-0.1.0.jar"
        $include = "io.reqover.example.mvc.auto"
        $endpoint = "http://127.0.0.1:$Port/auto/orders/42"
    } else {
        $appJar = Join-Path $repoRoot "examples\webflux-sample\build\libs\webflux-sample-0.1.0.jar"
        $include = "io.reqover.example.webflux.auto"
        $endpoint = "http://127.0.0.1:$Port/auto/reactive/orders/42"
    }

    $portProbe = [System.Net.Sockets.TcpClient]::new()
    try {
        $portProbe.Connect("127.0.0.1", $Port)
        throw "Port $Port is already in use; stop the existing process or choose another port."
    } catch [System.Net.Sockets.SocketException] {
        # Connection refused means the loopback port is available for the demo.
    } finally {
        $portProbe.Dispose()
    }

    $arguments = @(
        "-javaagent:$agentJar=include=$include",
        "-jar",
        $appJar,
        "--server.address=127.0.0.1",
        "--server.port=$Port",
        "--spring.main.banner-mode=off"
    )

    $process = Start-Process -FilePath $java -ArgumentList $arguments -PassThru -WindowStyle Hidden
    try {
        $reportUrl = "http://127.0.0.1:$Port/reqover/report"
        $deadline = (Get-Date).AddSeconds(45)
        $ready = $false
        do {
            $process.Refresh()
            if ($process.HasExited) {
                throw "Sample application exited before it became ready (status $($process.ExitCode))."
            }
            try {
                Invoke-RestMethod -Uri $reportUrl -TimeoutSec 2 | Out-Null
                # Recheck after a short delay so a stale server cannot mask a
                # bind failure in the child process we just started.
                Start-Sleep -Milliseconds 500
                $process.Refresh()
                if ($process.HasExited) {
                    throw "Sample application exited after the readiness probe (status $($process.ExitCode))."
                }
                $ready = $true
                break
            } catch {
                $process.Refresh()
                if ($process.HasExited) {
                    throw "Sample application exited while waiting for readiness (status $($process.ExitCode))."
                }
                Start-Sleep -Milliseconds 500
            }
        } while ((Get-Date) -lt $deadline)

        if (-not $ready) {
            throw "Sample application did not become ready within 45 seconds."
        }

        $process.Refresh()
        if ($process.HasExited) {
            throw "Sample application stopped before the demo request (status $($process.ExitCode))."
        }

        Invoke-RestMethod -Uri $endpoint -TimeoutSec 5 | ConvertTo-Json -Depth 5
        Invoke-RestMethod -Uri $reportUrl -TimeoutSec 5 | ConvertTo-Json -Depth 20
        Write-Host "HTML report: http://127.0.0.1:$Port/reqover/report.html"
        if (-not $StopAfterReport) {
            Read-Host "Press Enter to stop the sample application"
        }
    } finally {
        $process.Refresh()
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
} finally {
    Pop-Location
}
