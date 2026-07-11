# Maestro 카운터 샘플 — 빌드부터 리포트까지 한 번에 실행하는 스크립트
#
# 사용법: 저장소 루트에서
#   powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
#
# 순서: 단말 확인 -> 인코딩 설정 -> 화면 꺼짐 방지 -> 빌드/설치 -> Maestro 테스트 -> HTML 리포트 오픈

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "`n[1/6] 단말 연결 확인" -ForegroundColor Cyan
$devices = adb devices | Select-String "device$"
if (-not $devices) {
    Write-Host "연결된 Android 단말이 없습니다. 'adb devices'로 확인 후 다시 실행하세요." -ForegroundColor Red
    exit 1
}
adb devices -l

Write-Host "`n[2/6] 한글 인코딩 설정 (Windows 콘솔 cp949로 인한 깨짐 방지)" -ForegroundColor Cyan
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

Write-Host "`n[3/6] 화면 꺼짐 방지 (테스트 중 절전모드 진입 방지, OS 레벨 안전장치)" -ForegroundColor Cyan
adb shell svc power stayon usb

Write-Host "`n[4/6] 샘플 앱 빌드 및 단말 설치" -ForegroundColor Cyan
& "$root\gradlew.bat" installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "앱 빌드/설치에 실패했습니다." -ForegroundColor Red
    exit 1
}

Write-Host "`n[5/6] Maestro 테스트 실행 + HTML 리포트 생성" -ForegroundColor Cyan
$maestro = "$env:USERPROFILE\.maestro-cli\maestro\bin\maestro.bat"
if (-not (Test-Path $maestro)) {
    $maestro = "maestro"  # PATH에 등록된 경우
}

New-Item -ItemType Directory -Force -Path "$root\reports" | Out-Null

& $maestro test "$root\.maestro" --format HTML-DETAILED --output "$root\reports\report.html" --debug-output "$root\reports\debug"
$testExitCode = $LASTEXITCODE

# Maestro가 생성하는 리포트에는 <meta charset> 태그가 없어 브라우저가 한글을 깨진 문자로
# 표시하는 경우가 있다. UTF-8로 저장돼 있으므로 charset 메타 태그만 보강해준다.
$reportPath = "$root\reports\report.html"
if (Test-Path $reportPath) {
    $html = Get-Content -Path $reportPath -Raw -Encoding UTF8
    if ($html -notmatch '<meta charset=') {
        $html = $html -replace '<head>', "<head>`n    <meta charset=`"UTF-8`">"
        [System.IO.File]::WriteAllText($reportPath, $html, (New-Object System.Text.UTF8Encoding($false)))
    }
}

Write-Host "`n[6/6] 리포트 열기" -ForegroundColor Cyan
if (Test-Path "$root\reports\report.html") {
    Start-Process "$root\reports\report.html"
} else {
    Write-Host "리포트 파일이 생성되지 않았습니다: $root\reports\report.html" -ForegroundColor Yellow
}

if ($testExitCode -eq 0) {
    Write-Host "`n모든 테스트를 통과했습니다." -ForegroundColor Green
} else {
    Write-Host "`n일부 테스트가 실패했습니다. 리포트를 확인하세요." -ForegroundColor Yellow
}

exit $testExitCode
