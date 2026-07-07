[CmdletBinding()]
param(
    [switch]$SkipGradle,
    [switch]$RequireDevice,
    [switch]$RequireAvd,
    [switch]$Install,
    [switch]$Launch,
    [switch]$CaptureLogcat,
    [switch]$AllowLogcatIssues,
    [switch]$RequirePlaybackReports,
    [string[]]$RequiredPlaybackEvents = @("Started", "Progress", "Stopped"),
    [string]$DeviceSerial,
    [int]$LogcatSeconds = 20,
    [string]$OutputDir,
    [string]$SdkDir
)

$ErrorActionPreference = "Stop"
$script:Failures = 0
$script:Warnings = 0

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$ApplicationId = "com.embytv"
$MainActivity = "com.embytv/.MainActivity"

function Write-Result {
    param(
        [ValidateSet("OK", "WARN", "FAIL")]
        [string]$Status,
        [string]$Name,
        [string]$Detail = ""
    )

    $suffix = if ($Detail) { " - $Detail" } else { "" }
    Write-Host "[$Status] $Name$suffix"
    if ($Status -eq "WARN") {
        $script:Warnings += 1
    }
    if ($Status -eq "FAIL") {
        $script:Failures += 1
    }
}

function Resolve-AndroidSdk {
    if ($SdkDir) {
        return $SdkDir
    }

    $localProperties = Join-Path $RootDir "local.properties"
    if (Test-Path $localProperties) {
        $line = Get-Content $localProperties | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($line) {
            $raw = $line.Substring("sdk.dir=".Length).Trim()
            return ($raw -replace "\\:", ":" -replace "\\\\", "\")
        }
    }

    if ($env:ANDROID_HOME) {
        return $env:ANDROID_HOME
    }
    if ($env:ANDROID_SDK_ROOT) {
        return $env:ANDROID_SDK_ROOT
    }
    return $null
}

function Invoke-CheckedCommand {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "$Name exited with code $LASTEXITCODE"
        }
        Write-Result "OK" $Name
    } catch {
        Write-Result "FAIL" $Name $_.Exception.Message
    }
}

function Get-ReadyAdbDevices {
    param([string]$AdbPath)

    $devicesOutput = & $AdbPath devices
    $ready = @()
    $notReady = @()
    foreach ($line in $devicesOutput) {
        if ($line -match "^(\S+)\s+(device|unauthorized|offline)$") {
            $entry = [pscustomobject]@{
                Serial = $Matches[1]
                State = $Matches[2]
                Raw = $line
            }
            if ($entry.State -eq "device") {
                $ready += $entry
            } else {
                $notReady += $entry
            }
        }
    }
    return [pscustomobject]@{
        Ready = $ready
        NotReady = $notReady
    }
}

function Select-AdbDevice {
    param(
        [object[]]$ReadyDevices,
        [string]$Serial,
        [bool]$Required
    )

    if ($Serial) {
        $selected = $ReadyDevices | Where-Object { $_.Serial -eq $Serial } | Select-Object -First 1
        if ($selected) {
            return $selected.Serial
        }
        if ($Required) {
            Write-Result "FAIL" "Selected ADB device" "serial $Serial is not connected or not ready"
        } else {
            Write-Result "WARN" "Selected ADB device" "serial $Serial is not connected or not ready"
        }
        return $null
    }

    if ($ReadyDevices.Count -eq 1) {
        return $ReadyDevices[0].Serial
    }
    if ($ReadyDevices.Count -gt 1) {
        Write-Result "FAIL" "Selected ADB device" "multiple devices connected; pass -DeviceSerial"
        return $null
    }
    if ($Required) {
        Write-Result "FAIL" "Selected ADB device" "no connected device"
    }
    return $null
}

function Invoke-AdbChecked {
    param(
        [string]$Name,
        [string]$AdbPath,
        [string]$Serial,
        [string[]]$Arguments
    )

    try {
        & $AdbPath -s $Serial @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$Name exited with code $LASTEXITCODE"
        }
        Write-Result "OK" $Name
    } catch {
        Write-Result "FAIL" $Name $_.Exception.Message
    }
}

function Get-AppProcessId {
    param(
        [string]$AdbPath,
        [string]$Serial,
        [string]$PackageName
    )

    $pidOutput = & $AdbPath -s $Serial shell pidof $PackageName 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $null
    }
    $pidText = ($pidOutput -join " ").Trim()
    if (-not $pidText) {
        return $null
    }
    return ($pidText.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) | Select-Object -First 1)
}

function Analyze-Logcat {
    param(
        [string]$LogPath,
        [string]$IssuesPath,
        [bool]$AllowIssues
    )

    $patterns = @(
        "FATAL EXCEPTION",
        "ANR in com.embytv",
        "AndroidRuntime",
        "PlaybackException",
        "ExoPlaybackException",
        "ExoPlayerImplInternal",
        "DecoderInitializationException",
        "MediaCodecRenderer",
        "ERROR_CODE_"
    )
    $issues = @(Select-String -Path $LogPath -Pattern $patterns -SimpleMatch)
    if ($issues.Count -eq 0) {
        Write-Result "OK" "Analyze logcat" "no critical player/runtime issues found"
        return
    }

    $issues | Select-Object -First 80 | ForEach-Object { $_.Line } | Out-File -FilePath $IssuesPath -Encoding utf8
    if ($AllowIssues) {
        Write-Result "WARN" "Analyze logcat" "found $($issues.Count) issue line(s); see $IssuesPath"
    } else {
        Write-Result "FAIL" "Analyze logcat" "found $($issues.Count) issue line(s); see $IssuesPath"
    }
}

function Analyze-PlaybackReportLogcat {
    param(
        [string]$LogPath,
        [string[]]$RequiredEvents,
        [bool]$Required
    )

    $reportMatches = @(Select-String -Path $LogPath -Pattern "EmbyTvPlaybackReport" -SimpleMatch)
    if ($reportMatches.Count -eq 0) {
        if ($Required) {
            Write-Result "FAIL" "Playback report diagnostics" "no EmbyTvPlaybackReport lines found"
        } else {
            Write-Result "WARN" "Playback report diagnostics" "no EmbyTvPlaybackReport lines found"
        }
        return
    }

    Write-Result "OK" "Playback report diagnostics" "found $($reportMatches.Count) line(s)"

    $failedReports = @($reportMatches | Where-Object { $_.Line -match "\bfailed\b" })
    if ($failedReports.Count -gt 0) {
        $detail = "found $($failedReports.Count) failed report line(s)"
        if ($Required) {
            Write-Result "FAIL" "Playback report failures" $detail
        } else {
            Write-Result "WARN" "Playback report failures" $detail
        }
    }

    foreach ($eventName in $RequiredEvents) {
        $eventPattern = "succeeded $eventName"
        $eventMatches = @($reportMatches | Where-Object { $_.Line -like "*$eventPattern*" })
        if ($eventMatches.Count -gt 0) {
            Write-Result "OK" "Playback report $eventName" "succeeded line found"
        } elseif ($Required) {
            Write-Result "FAIL" "Playback report $eventName" "missing succeeded line"
        } else {
            Write-Result "WARN" "Playback report $eventName" "missing succeeded line"
        }
    }
}

Set-Location $RootDir

Write-Host "Player runtime preflight"
Write-Host "Workspace: $RootDir"
if ($Install -or $Launch -or $CaptureLogcat) {
    Write-Host "Device actions: install=$Install launch=$Launch captureLogcat=$CaptureLogcat"
}
if ($RequirePlaybackReports -and -not $CaptureLogcat) {
    Write-Result "FAIL" "Playback report diagnostics" "-RequirePlaybackReports requires -CaptureLogcat"
}

if ($env:JAVA_HOME) {
    Write-Result "OK" "JAVA_HOME" $env:JAVA_HOME
} else {
    Write-Result "WARN" "JAVA_HOME" "not set; Gradle wrapper will use PATH java"
}

$sdkPath = Resolve-AndroidSdk
if ($sdkPath -and (Test-Path $sdkPath)) {
    Write-Result "OK" "Android SDK" $sdkPath
} else {
    Write-Result "FAIL" "Android SDK" "not found; set local.properties sdk.dir or ANDROID_HOME"
}

if (-not $SkipGradle) {
    Invoke-CheckedCommand "Player JVM tests" {
        & (Join-Path $RootDir "gradlew.bat") ":app:testDebugUnitTest" "--tests" "com.embytv.ui.player.*"
    }
    Invoke-CheckedCommand "Debug APK build" {
        & (Join-Path $RootDir "gradlew.bat") ":app:assembleDebug"
    }
} else {
    Write-Result "WARN" "Gradle checks" "skipped by -SkipGradle"
}

$apkPath = Join-Path $RootDir "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Result "OK" "Debug APK" $apkPath
} else {
    Write-Result "FAIL" "Debug APK" "missing; run scripts/player-runtime-preflight.ps1 without -SkipGradle"
}

if ($sdkPath) {
    $adbPath = Join-Path $sdkPath "platform-tools\adb.exe"
    $selectedDevice = $null
    if (Test-Path $adbPath) {
        $deviceScan = Get-ReadyAdbDevices -AdbPath $adbPath
        if ($deviceScan.Ready.Count -gt 0) {
            Write-Result "OK" "ADB devices" (($deviceScan.Ready | ForEach-Object { $_.Raw }) -join "; ")
        } elseif ($RequireDevice) {
            Write-Result "FAIL" "ADB devices" "no connected device"
        } else {
            Write-Result "WARN" "ADB devices" "no connected device"
        }
        if ($deviceScan.NotReady.Count -gt 0) {
            Write-Result "WARN" "ADB not-ready devices" (($deviceScan.NotReady | ForEach-Object { $_.Raw }) -join "; ")
        }

        $needsDevice = [bool]($Install -or $Launch -or $CaptureLogcat)
        $selectedDevice = Select-AdbDevice -ReadyDevices $deviceScan.Ready -Serial $DeviceSerial -Required $needsDevice
        if ($selectedDevice) {
            Write-Result "OK" "Selected ADB device" $selectedDevice
        }

        if ($selectedDevice -and $Install) {
            Invoke-AdbChecked "Install debug APK" $adbPath $selectedDevice @("install", "-r", $apkPath)
        }

        if ($selectedDevice -and $CaptureLogcat) {
            Invoke-AdbChecked "Clear logcat" $adbPath $selectedDevice @("logcat", "-c")
        }

        if ($selectedDevice -and $Launch) {
            Invoke-AdbChecked "Launch app" $adbPath $selectedDevice @("shell", "am", "start", "-n", $MainActivity)
        }

        if ($selectedDevice -and $CaptureLogcat) {
            $logDir = if ($OutputDir) { $OutputDir } else { Join-Path $RootDir "build\player-runtime" }
            New-Item -ItemType Directory -Force -Path $logDir | Out-Null
            $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
            $logPath = Join-Path $logDir "logcat-$timestamp.txt"
            Start-Sleep -Seconds 2
            $appPid = Get-AppProcessId -AdbPath $adbPath -Serial $selectedDevice -PackageName $ApplicationId
            if ($appPid) {
                Write-Result "OK" "App process" "$ApplicationId pid=$appPid"
            } elseif ($Launch) {
                Write-Result "FAIL" "App process" "$ApplicationId is not running after launch"
            } else {
                Write-Result "WARN" "App process" "$ApplicationId is not running; capturing full logcat"
            }
            Start-Sleep -Seconds ([Math]::Max(1, $LogcatSeconds))
            if ($appPid) {
                & $adbPath -s $selectedDevice logcat --pid=$appPid -d -v time | Out-File -FilePath $logPath -Encoding utf8
            } else {
                & $adbPath -s $selectedDevice logcat -d -v time | Out-File -FilePath $logPath -Encoding utf8
            }
            if ($LASTEXITCODE -eq 0 -and (Test-Path $logPath)) {
                Write-Result "OK" "Capture logcat" $logPath
                $issuesPath = Join-Path $logDir "logcat-issues-$timestamp.txt"
                Analyze-Logcat -LogPath $logPath -IssuesPath $issuesPath -AllowIssues:$AllowLogcatIssues
                Analyze-PlaybackReportLogcat `
                    -LogPath $logPath `
                    -RequiredEvents $RequiredPlaybackEvents `
                    -Required:$RequirePlaybackReports
            } else {
                Write-Result "FAIL" "Capture logcat" "logcat exited with code $LASTEXITCODE"
            }
        }
    } else {
        Write-Result "FAIL" "ADB" "missing at $adbPath"
    }

    $emulatorPath = Join-Path $sdkPath "emulator\emulator.exe"
    if (Test-Path $emulatorPath) {
        $avds = & $emulatorPath -list-avds
        if ($avds.Count -gt 0) {
            Write-Result "OK" "Android Virtual Devices" ($avds -join "; ")
        } elseif ($RequireAvd) {
            Write-Result "FAIL" "Android Virtual Devices" "no AVD configured"
        } else {
            Write-Result "WARN" "Android Virtual Devices" "no AVD configured"
        }
    } else {
        Write-Result "FAIL" "Android emulator" "missing at $emulatorPath"
    }
}

if ($script:Failures -gt 0) {
    Write-Host "Preflight failed: $script:Failures failure(s), $script:Warnings warning(s)."
    exit 1
}

Write-Host "Preflight passed with $script:Warnings warning(s)."
