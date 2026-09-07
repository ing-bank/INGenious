<#
.SYNOPSIS
  INGenious (v3.1.x) project scaffolder for Windows (PowerShell 5.1+).

.DESCRIPTION
  Creates an independent YAML-based INGenious project (YAML Object Repository,
  YAML Reusable Components, YAML Test Cases, YAML Test Lab) from the bundled
  `project-template`, then generates a `.project` (schema 3.1.0) metadata file
  by scanning the project's TestPlan and ReusableComponents folders.

  Requires only Windows PowerShell (built in) — no Node, no extra runtime.

.PARAMETER Name
  (required) Project name / folder name.
.PARAMETER ProjectsRoot
  Output root for projects (default: "Projects").
.PARAMETER Sync
  Only (re)generate .project from existing project files.
.PARAMETER NoSamples
  Scaffold an empty skeleton (omit Sample* content).
.PARAMETER WithEnvHelpers
  Add GetEnv reusable components (env/URL selection).
.PARAMETER WithDbHelpers
  Add DatabaseConnection reusable component.
.PARAMETER Scenarios
  Extra (empty) scenarios to register in .project.
.PARAMETER Tags
  Extra tags to register in .project.

.EXAMPLE
  .\create-project.ps1 -Name LoginSuite
.EXAMPLE
  .\create-project.ps1 -Name LoginSuite -NoSamples -WithEnvHelpers
.EXAMPLE
  .\create-project.ps1 -Name LoginSuite -Sync
#>
param(
  [Parameter(Mandatory = $true)][string]$Name,
  [string]$ProjectsRoot = "Projects",
  [switch]$Sync,
  [switch]$NoSamples,
  [switch]$WithEnvHelpers,
  [switch]$WithDbHelpers,
  [string[]]$Scenarios = @(),
  [string[]]$Tags = @()
)

$ErrorActionPreference = "Stop"

$ProjectTemplate = Join-Path $PSScriptRoot "project-template"
$OptionalHelpers = Join-Path $PSScriptRoot "optional-helpers"
$ProjectPath     = Join-Path $ProjectsRoot $Name

# --- YAML header helpers ----------------------------------------------------
function Get-YamlField {
  param([string]$File, [string]$Key)
  foreach ($line in Get-Content -LiteralPath $File) {
    if ($line -match '^steps:\s*$') { break }
    if ($line -match "^$([regex]::Escape($Key)):\s*(.*)$") {
      return ($matches[1].Trim() -replace '^["'']', '' -replace '["'']$', '')
    }
  }
  return ''
}

function Get-YamlTags {
  param([string]$File)
  $tags = @()
  $inTags = $false
  foreach ($line in Get-Content -LiteralPath $File) {
    if ($line -match '^steps:\s*$') { break }
    if ($inTags) {
      if ($line -match '^\s+-\s*(.*)$') {
        $tags += ($matches[1].Trim() -replace '^["'']', '' -replace '["'']$', '')
        continue
      } else { $inTags = $false }
    }
    if ($line -match '^tags:\s*$') { $inTags = $true }
  }
  return ,$tags
}

# --- .project (schema 3.1.0) generation ------------------------------------
function New-ProjectMetadata {
  $testPlanDir = Join-Path $ProjectPath "TestPlan"
  $reusableDir = Join-Path $ProjectPath "ReusableComponents"

  $scnList = New-Object System.Collections.Specialized.OrderedDictionary
  $tagList = New-Object System.Collections.Specialized.OrderedDictionary
  $data    = New-Object System.Collections.ArrayList

  $collect = {
    param($Dir, $Type)
    if (-not (Test-Path $Dir)) { return }
    $files = Get-ChildItem -Path $Dir -Recurse -File |
      Where-Object { $_.Extension -in '.yaml', '.yml' } | Sort-Object FullName
    foreach ($f in $files) {
      $scn  = Get-YamlField -File $f.FullName -Key 'scenario'
      $name = Get-YamlField -File $f.FullName -Key 'testCase'
      if (-not $name) { $name = Get-YamlField -File $f.FullName -Key 'reusable' }
      if (-not $name) { $name = Get-YamlField -File $f.FullName -Key 'name' }
      if (-not $name) { $name = [System.IO.Path]::GetFileNameWithoutExtension($f.Name) }
      if (-not $scn)  { $scn  = Split-Path $f.DirectoryName -Leaf }

      $fileTags = Get-YamlTags -File $f.FullName
      $entryTags = @()
      foreach ($t in $fileTags) {
        if ($t) {
          if (-not $tagList.Contains($t)) { $tagList.Add($t, $true) }
          $entryTags += [ordered]@{ value = $t }
        }
      }
      if (-not $scnList.Contains($scn)) { $scnList.Add($scn, $true) }

      [void]$data.Add([ordered]@{
        id         = "$scn#$name"
        name       = $name
        tags       = ([object[]]$entryTags)
        attributes = @(
          [ordered]@{ name = 'type';     value = $Type },
          [ordered]@{ name = 'scenario'; value = $scn  }
        )
      })
    }
  }

  & $collect $testPlanDir 'testcase'
  & $collect $reusableDir 'reusable'

  foreach ($s in $Scenarios) { if ($s -and -not $scnList.Contains($s)) { $scnList.Add($s, $true) } }
  foreach ($t in $Tags)      { if ($t -and -not $tagList.Contains($t)) { $tagList.Add($t, $true) } }

  $meta = New-Object System.Collections.ArrayList
  [void]$meta.Add([ordered]@{
    type = 'attribute'; name = 'scenario'
    desc = 'High level classification of test requirement/cases grouped together'
    ref  = 'com.ing.datalib.model.Attribute'; attributes = @(); tags = @()
  })
  foreach ($t in $tagList.Keys) {
    $o = [ordered]@{ type = 'tag'; name = $t }
    if ($t -eq '@smoke') {
      $o.desc = 'Non-exhaustive set of tests that aim at ensuring that the most important functions work'
    }
    $o.ref = 'com.ing.datalib.model.Tag'; $o.attributes = @(); $o.tags = @()
    [void]$meta.Add($o)
  }
  foreach ($s in $scnList.Keys) {
    [void]$meta.Add([ordered]@{
      type = 'scenario'; name = $s; ref = 'com.ing.datalib.model.Attribute'
      attributes = @(); tags = @()
    })
  }

  $project = [ordered]@{
    id = $Name; name = $Name; version = '3.1.0'
    attributes = @(); tags = @(); _meta = ([object[]]$meta); data = ([object[]]$data)
  }

  $json = $project | ConvertTo-Json -Depth 50
  $json = $json -replace '\\u003c', '<' -replace '\\u003e', '>' -replace '\\u0026', '&' -replace '\\u0027', "'"
  # Windows PowerShell 5.1 can serialize empty arrays as "" instead of []; force [].
  $json = $json -replace '"attributes":\s*""', '"attributes": []' `
                -replace '"tags":\s*""', '"tags": []' `
                -replace '"data":\s*""', '"data": []'
  $file = Join-Path $ProjectPath ".project"
  # Write UTF-8 *without* a BOM (a leading BOM can break strict JSON parsers).
  [System.IO.File]::WriteAllText($file, ($json + "`n"), (New-Object System.Text.UTF8Encoding $false))
  return @{ File = $file; Count = $data.Count; Scenarios = @($scnList.Keys) }
}

# --- scaffolding ------------------------------------------------------------
function Copy-Template {
  if (-not (Test-Path $ProjectTemplate)) { throw "Missing template bundle: $ProjectTemplate" }
  New-Item -ItemType Directory -Path $ProjectPath -Force | Out-Null
  Copy-Item -Path (Join-Path $ProjectTemplate '*') -Destination $ProjectPath -Recurse -Force

  Get-ChildItem -Path $ProjectPath -Recurse -Force -Filter '.gitkeep' -ErrorAction SilentlyContinue |
    Remove-Item -Force
  Remove-Item (Join-Path $ProjectPath '.project') -Force -ErrorAction SilentlyContinue

  Get-ChildItem -Path $ProjectPath -Recurse -File | ForEach-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and $content.Contains('{{PROJECT_NAME}}')) {
      ($content -replace '\{\{PROJECT_NAME\}\}', $Name) |
        Set-Content -LiteralPath $_.FullName -NoNewline
    }
  }
}

function Remove-SampleContent {
  $paths = @(
    'ObjectRepository\Web\SamplePage.yaml',
    'ReusableComponents\Common\Launch.yaml',
    'ReusableComponents\SampleScenario',
    'TestPlan\SampleScenario',
    'TestLab\SampleRelease',
    'TestData\SampleData.csv'
  )
  foreach ($p in $paths) {
    $full = Join-Path $ProjectPath $p
    if (Test-Path $full) { Remove-Item -Recurse -Force $full }
  }
  foreach ($d in @('ObjectRepository\Web', 'ReusableComponents', 'TestPlan', 'TestLab')) {
    New-Item -ItemType Directory -Path (Join-Path $ProjectPath $d) -Force | Out-Null
  }
}

# --- main -------------------------------------------------------------------
if ($Sync) {
  if (-not (Test-Path $ProjectPath)) { throw "Cannot -Sync: project not found at $ProjectPath" }
  $r = New-ProjectMetadata
  Write-Host "Synced metadata ($($r.Count) entries): $($r.File)"
  return
}

if ((Test-Path $ProjectPath) -and (@(Get-ChildItem -Force $ProjectPath).Count -gt 0)) {
  throw "Refusing to scaffold over a non-empty folder: $ProjectPath`nUse -Sync to regenerate .project, or choose a different -Name."
}

Copy-Template

if ($NoSamples) { Remove-SampleContent }

if ($WithEnvHelpers) {
  $dst = Join-Path $ProjectPath 'ReusableComponents\GetEnv'
  New-Item -ItemType Directory -Path $dst -Force | Out-Null
  Copy-Item -Path (Join-Path $OptionalHelpers 'ReusableComponents\GetEnv\*') -Destination $dst -Recurse -Force
}

if ($WithDbHelpers) {
  $dst = Join-Path $ProjectPath 'ReusableComponents\DatabaseConnection'
  New-Item -ItemType Directory -Path $dst -Force | Out-Null
  Copy-Item -Path (Join-Path $OptionalHelpers 'ReusableComponents\DatabaseConnection\*') -Destination $dst -Recurse -Force
}

foreach ($s in $Scenarios) {
  if ($s) {
    New-Item -ItemType Directory -Path (Join-Path $ProjectPath "TestPlan\$s") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $ProjectPath "ReusableComponents\$s") -Force | Out-Null
  }
}

$r = New-ProjectMetadata
Write-Host "Created INGenious project: $ProjectPath"
Write-Host ("  scenarios : " + ($r.Scenarios -join ', '))
Write-Host "  entries   : $($r.Count)"
Write-Host "  .project  : $($r.File)"
Write-Host "Next: add YAML pages to ObjectRepository/Web, reusables to ReusableComponents/<Scenario>, test cases to TestPlan/<Scenario>, then re-run with -Sync to refresh .project."
