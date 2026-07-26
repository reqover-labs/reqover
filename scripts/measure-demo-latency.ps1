param(
    [Parameter(Mandatory = $true)]
    [string] $Url,
    [int] $WarmupRequests = 20,
    [int] $MeasuredRequests = 200,
    [int] $TimeoutSeconds = 5
)

$ErrorActionPreference = "Stop"

function Invoke-TimedRequest {
    param([string] $TargetUrl)

    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-WebRequest -Uri $TargetUrl -UseBasicParsing -TimeoutSec $TimeoutSeconds | Out-Null
    $watch.Stop()
    return $watch.Elapsed.TotalMilliseconds
}

for ($i = 0; $i -lt $WarmupRequests; $i++) {
    Invoke-TimedRequest -TargetUrl $Url | Out-Null
}

$samples = New-Object System.Collections.Generic.List[double]
for ($i = 0; $i -lt $MeasuredRequests; $i++) {
    $samples.Add((Invoke-TimedRequest -TargetUrl $Url))
}

$sorted = $samples | Sort-Object
$average = ($samples | Measure-Object -Average).Average
$p50Index = [Math]::Min($sorted.Count - 1, [Math]::Floor($sorted.Count * 0.50))
$p95Index = [Math]::Min($sorted.Count - 1, [Math]::Floor($sorted.Count * 0.95))
$p99Index = [Math]::Min($sorted.Count - 1, [Math]::Floor($sorted.Count * 0.99))

[PSCustomObject]@{
    url = $Url
    warmupRequests = $WarmupRequests
    measuredRequests = $MeasuredRequests
    averageMs = [Math]::Round($average, 2)
    p50Ms = [Math]::Round($sorted[$p50Index], 2)
    p95Ms = [Math]::Round($sorted[$p95Index], 2)
    p99Ms = [Math]::Round($sorted[$p99Index], 2)
    minMs = [Math]::Round($sorted[0], 2)
    maxMs = [Math]::Round($sorted[$sorted.Count - 1], 2)
} | ConvertTo-Json
