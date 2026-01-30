# Diablo Access automatic installer for Windows
# Fixed version: proper Downloads path, safe extraction, correct EXE detection
# All comments and messages in English

Add-Type -AssemblyName System.Windows.Forms

# GitHub repository info
$repoOwner = "mojsior"
$repoName  = "diablo-access"
$apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

# Correct Downloads folder path (Downloads is NOT a SpecialFolder)
$downloadsDir = Join-Path $env:USERPROFILE "Downloads"

# Ensure Downloads folder exists
if (-not (Test-Path $downloadsDir)) {
    New-Item -ItemType Directory -Path $downloadsDir | Out-Null
}

$installDir = Join-Path $downloadsDir "diablo-access"
$zipPath    = Join-Path $downloadsDir "diablo-access-latest.zip"

Write-Host "Checking latest Diablo Access release on GitHub..." -ForegroundColor Cyan

# Get latest release info
$releaseInfo = Invoke-RestMethod -Uri $apiUrl -Headers @{ "User-Agent" = "PowerShell" }

# Find Windows x64 ZIP asset
$asset = $releaseInfo.assets | Where-Object {
    $_.name -match "windows" -and $_.name -match "x64" -and $_.name -match "\.zip$"
} | Select-Object -First 1

if (-not $asset) {
    Write-Host "No compatible Windows x64 ZIP found." -ForegroundColor Red
    exit 1
}

Write-Host "Latest version found: $($releaseInfo.tag_name)" -ForegroundColor Green
Write-Host "Downloading $($asset.name)..." -ForegroundColor Cyan

# Download ZIP
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zipPath

# Remove previous installation
if (Test-Path $installDir) {
    Remove-Item -Path $installDir -Recurse -Force
}

Write-Host "Extracting files..." -ForegroundColor Cyan

# Extract only if ZIP exists
if (Test-Path $zipPath) {
    Expand-Archive -Path $zipPath -DestinationPath $installDir
} else {
    Write-Host "ZIP file not found. Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Extraction completed." -ForegroundColor Green

# Ask user to select Diablo data file
$dialog = New-Object System.Windows.Forms.OpenFileDialog
$dialog.Title = "Select your Diablo data file (e.g. update.mq)"
$dialog.Filter = "All files (*.*)|*.*"
$dialog.InitialDirectory = [Environment]::GetFolderPath("Desktop")

if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
    Copy-Item -Path $dialog.FileName -Destination $installDir -Force
    Write-Host "Data file copied successfully." -ForegroundColor Green
} else {
    Write-Host "No data file selected. Skipping this step." -ForegroundColor Yellow
}

# Ask user if they want to run the program
$response = Read-Host "Do you want to run Diablo Access now? (y/n)"

if ($response -eq "y") {

    # Prefer executables with 'diablo' in the name, avoid installers
    $exe = Get-ChildItem -Path $installDir -Filter "*.exe" -File |
           Where-Object { $_.Name -match "diablo" -and $_.Name -notmatch "setup|install" } |
           Select-Object -First 1

    # Fallback: any exe
    if (-not $exe) {
        $exe = Get-ChildItem -Path $installDir -Filter "*.exe" -File | Select-Object -First 1
    }

    if ($exe) {
        Write-Host "Launching $($exe.Name)..." -ForegroundColor Cyan
        Start-Process -FilePath $exe.FullName
    } else {
        Write-Host "No executable found in the installation folder." -ForegroundColor Red
    }
}

Write-Host "Done." -ForegroundColor Cyan
