param(
    [Parameter(Mandatory = $true)]
    [string] $Url,
    [ValidateRange(0, 1000000)]
    [int] $WarmupRequests = 50,
    [ValidateRange(1, 1000000)]
    [int] $MeasuredRequests = 300,
    [string] $SamplesFile,
    [ValidateRange(1, 300)]
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

function Get-NearestRankIndex {
    param([double] $Percentile)
    return [Math]::Max(0, [Math]::Ceiling($sorted.Count * $Percentile) - 1)
}

$p50Index = Get-NearestRankIndex -Percentile 0.50
$p95Index = Get-NearestRankIndex -Percentile 0.95
$p99Index = Get-NearestRankIndex -Percentile 0.99

if ($SamplesFile) {
    $samplesDirectory = Split-Path -Parent $SamplesFile
    if ($samplesDirectory) {
        New-Item -ItemType Directory -Force -Path $samplesDirectory | Out-Null
    }
    $samples |
        ForEach-Object { $_.ToString("F6", [System.Globalization.CultureInfo]::InvariantCulture) } |
        Set-Content -Encoding utf8 -Path $SamplesFile
}

[PSCustomObject]@{
    url = $Url
    warmupRequests = $WarmupRequests
    measuredRequests = $MeasuredRequests
    percentileMethod = "nearest-rank"
    averageMs = [Math]::Round($average, 2)
    p50Ms = [Math]::Round($sorted[$p50Index], 2)
    p95Ms = [Math]::Round($sorted[$p95Index], 2)
    p99Ms = [Math]::Round($sorted[$p99Index], 2)
    minMs = [Math]::Round($sorted[0], 2)
    maxMs = [Math]::Round($sorted[$sorted.Count - 1], 2)
} | ConvertTo-Json
