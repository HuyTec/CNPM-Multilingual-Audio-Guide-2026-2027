$ErrorActionPreference = 'Stop'

$diagramPath = Join-Path $PSScriptRoot '..\docs\en\System_Analysist.drawio.xml'
[xml]$document = Get-Content -Raw $diagramPath

function Add-Vertex($root, $id, $value, $style, [double]$x, [double]$y, [double]$width, [double]$height) {
    if ($root.SelectSingleNode("./mxCell[@id='$id']")) { return }

    $cell = $document.CreateElement('mxCell')
    $cell.SetAttribute('id', $id)
    $cell.SetAttribute('parent', '1')
    $cell.SetAttribute('style', $style)
    $cell.SetAttribute('value', $value)
    $cell.SetAttribute('vertex', '1')
    $geometry = $document.CreateElement('mxGeometry')
    $geometry.SetAttribute('as', 'geometry')
    $geometry.SetAttribute('x', [string]$x)
    $geometry.SetAttribute('y', [string]$y)
    $geometry.SetAttribute('width', [string]$width)
    $geometry.SetAttribute('height', [string]$height)
    [void]$cell.AppendChild($geometry)
    [void]$root.AppendChild($cell)
}

function Add-Edge($root, $id, $source, $target) {
    if ($root.SelectSingleNode("./mxCell[@id='$id']")) { return }

    $cell = $document.CreateElement('mxCell')
    $cell.SetAttribute('id', $id)
    $cell.SetAttribute('parent', '1')
    $cell.SetAttribute('source', $source)
    $cell.SetAttribute('target', $target)
    $cell.SetAttribute('edge', '1')
    $cell.SetAttribute('style', 'edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;')
    $geometry = $document.CreateElement('mxGeometry')
    $geometry.SetAttribute('as', 'geometry')
    $geometry.SetAttribute('relative', '1')
    [void]$cell.AppendChild($geometry)
    [void]$root.AppendChild($cell)
}

$actorCells = @{
    'UC1_activity' = @('vza5PDOPSUm6RJ7DLLyD-9', 'vza5PDOPSUm6RJ7DLLyD-13', 'vza5PDOPSUm6RJ7DLLyD-17', 'vza5PDOPSUm6RJ7DLLyD-19', 'vza5PDOPSUm6RJ7DLLyD-21')
    'UC2_activity' = @('jr7hXPGmXHG0paXlPZda-12', 'jr7hXPGmXHG0paXlPZda-13')
    'UC3_activity' = @('o0_6KubHvzofkxdgklQR-3', 'o0_6KubHvzofkxdgklQR-16')
    'UC4_activity' = @('g45D0m5QW9cc8jciQBO7-2', 'g45D0m5QW9cc8jciQBO7-6', 'g45D0m5QW9cc8jciQBO7-18', 'g45D0m5QW9cc8jciQBO7-52', 'g45D0m5QW9cc8jciQBO7-56', 'uc4-delete-confirm')
    'UC5_activity' = @('zzgoSdxd4NCH2Uh3L3Jj-11')
    'UC6_activity' = @('zETJm5G_Df36_UZA9jXr-2', 'zETJm5G_Df36_UZA9jXr-8', 'zETJm5G_Df36_UZA9jXr-11', 'zETJm5G_Df36_UZA9jXr-13', 'zETJm5G_Df36_UZA9jXr-20', 'zETJm5G_Df36_UZA9jXr-22')
    'UC7_activity' = @('Q8Nx3ErHPsax8sTNVdMS-2', 'Q8Nx3ErHPsax8sTNVdMS-12', 'Q8Nx3ErHPsax8sTNVdMS-17', 'Q8Nx3ErHPsax8sTNVdMS-20', 'Q8Nx3ErHPsax8sTNVdMS-24', 'Q8Nx3ErHPsax8sTNVdMS-27', 'Q8Nx3ErHPsax8sTNVdMS-29')
}

$laneLabels = @{
    'UC1_activity' = @('ACTOR — Tourist', 'SYSTEM — Mobile App / Location Services')
    'UC2_activity' = @('ACTOR — Tourist', 'SYSTEM — Mobile App / Approved Content Source')
    'UC3_activity' = @('ACTOR — Tourist', 'SYSTEM — Mobile App / OS Audio and TTS')
    'UC4_activity' = @('ACTOR — Tourist', 'SYSTEM — Mobile App / Package Server')
    'UC5_activity' = @('ACTOR — Tourist', 'SYSTEM — Mobile App / OS Geofence and Notifications')
    'UC6_activity' = @('ACTOR — Content Admin', 'SYSTEM — Web Admin / Content Service')
    'UC7_activity' = @('ACTOR — System Admin / Product Owner', 'SYSTEM — Web Admin / Analytics and Feedback Services')
}

foreach ($diagram in $document.mxfile.diagram | Where-Object { $_.name -like '*_activity' }) {
    $root = $diagram.mxGraphModel.root
    $vertices = @($root.mxCell | Where-Object { $_.vertex -eq '1' -and $_.parent -eq '1' -and $_.mxGeometry })
    $minX = [double](($vertices | ForEach-Object { [double]$_.mxGeometry.x } | Measure-Object -Minimum).Minimum)
    $maxX = [double](($vertices | ForEach-Object { [double]$_.mxGeometry.x + [double]$_.mxGeometry.width } | Measure-Object -Maximum).Maximum)
    $minY = [double](($vertices | ForEach-Object { [double]$_.mxGeometry.y } | Measure-Object -Minimum).Minimum)
    $maxY = [double](($vertices | ForEach-Object { [double]$_.mxGeometry.y + [double]$_.mxGeometry.height } | Measure-Object -Maximum).Maximum)
    $actorX = $minX - 680
    $actorWidth = 620
    $systemX = $minX - 30
    $systemWidth = ($maxX - $minX) + 60
    $laneY = $minY - 70
    $laneHeight = ($maxY - $minY) + 150

    $anchor = $root.SelectSingleNode("./mxCell[@id='1']")
    $needsLayout = -not $root.SelectSingleNode("./mxCell[@id='$($diagram.name)-actor-lane']")
    if ($needsLayout) {
        $actorLane = $document.CreateElement('mxCell')
        $actorLane.SetAttribute('id', "$($diagram.name)-actor-lane")
        $actorLane.SetAttribute('parent', '1')
        $actorLane.SetAttribute('style', 'swimlane;horizontal=1;startSize=34;whiteSpace=wrap;html=1;fillColor=#F5F5F5;swimlaneFillColor=#DAE8FC;strokeColor=#6C8EBF;fontStyle=1;fontSize=14;')
        $actorLane.SetAttribute('value', $laneLabels[$diagram.name][0])
        $actorLane.SetAttribute('vertex', '1')
        $geometry = $document.CreateElement('mxGeometry')
        $geometry.SetAttribute('as', 'geometry'); $geometry.SetAttribute('x', [string]$actorX); $geometry.SetAttribute('y', [string]$laneY); $geometry.SetAttribute('width', [string]$actorWidth); $geometry.SetAttribute('height', [string]$laneHeight)
        [void]$actorLane.AppendChild($geometry)
        [void]$root.InsertAfter($actorLane, $anchor)

        $systemLane = $document.CreateElement('mxCell')
        $systemLane.SetAttribute('id', "$($diagram.name)-system-lane")
        $systemLane.SetAttribute('parent', '1')
        $systemLane.SetAttribute('style', 'swimlane;horizontal=1;startSize=34;whiteSpace=wrap;html=1;fillColor=#FFFFFF;swimlaneFillColor=#D5E8D4;strokeColor=#82B366;fontStyle=1;fontSize=14;')
        $systemLane.SetAttribute('value', $laneLabels[$diagram.name][1])
        $systemLane.SetAttribute('vertex', '1')
        $systemGeometry = $document.CreateElement('mxGeometry')
        $systemGeometry.SetAttribute('as', 'geometry'); $systemGeometry.SetAttribute('x', [string]$systemX); $systemGeometry.SetAttribute('y', [string]$laneY); $systemGeometry.SetAttribute('width', [string]$systemWidth); $systemGeometry.SetAttribute('height', [string]$laneHeight)
        [void]$systemLane.AppendChild($systemGeometry)
        [void]$root.InsertAfter($systemLane, $actorLane)
    }

    if ($needsLayout) {
        foreach ($actorId in $actorCells[$diagram.name]) {
            $cell = $root.SelectSingleNode("./mxCell[@id='$actorId']")
            if (-not $cell -or -not $cell.mxGeometry) { continue }
            $oldX = [double]$cell.mxGeometry.x
            $span = [Math]::Max(1, $maxX - $minX)
            $availableWidth = [Math]::Max(80, $actorWidth - [double]$cell.mxGeometry.width - 50)
            $relativeX = ($oldX - $minX) / $span
            $cell.mxGeometry.x = [Math]::Round($actorX + 25 + ($relativeX * $availableWidth), 0)
        }
    }
}

# UC-04 has the only business-valid parallel behavior: downloading package bytes and reporting progress.
$uc4 = $document.mxfile.diagram | Where-Object { $_.name -eq 'UC4_activity' }
$uc4Root = $uc4.mxGraphModel.root
$downloadEdge = $uc4Root.SelectSingleNode("./mxCell[@id='g45D0m5QW9cc8jciQBO7-29']")
if ($downloadEdge) { $downloadEdge.SetAttribute('target', 'uc4-fork-download-progress') }
$downloadStart = $uc4Root.SelectSingleNode("./mxCell[@id='g45D0m5QW9cc8jciQBO7-26']")
if ($downloadStart) { $downloadStart.SetAttribute('value', 'Start package download') }

$barStyle = 'rounded=0;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'
$taskStyle = 'rounded=1;whiteSpace=wrap;html=1;fillColor=#DAE8FC;strokeColor=#6C8EBF;fontSize=12;'
Add-Vertex $uc4Root 'uc4-fork-download-progress' '' $barStyle 475 1140 50 6
Add-Vertex $uc4Root 'uc4-download-bytes' 'Download package bytes' $taskStyle 330 1190 180 50
Add-Vertex $uc4Root 'uc4-report-progress' 'Update download progress on UI' $taskStyle 650 1190 210 50
Add-Vertex $uc4Root 'uc4-join-download-progress' '' $barStyle 565 1280 60 6
Add-Edge $uc4Root 'uc4-fork-to-download' 'uc4-fork-download-progress' 'uc4-download-bytes'
Add-Edge $uc4Root 'uc4-fork-to-progress' 'uc4-fork-download-progress' 'uc4-report-progress'
Add-Edge $uc4Root 'uc4-download-to-join' 'uc4-download-bytes' 'uc4-join-download-progress'
Add-Edge $uc4Root 'uc4-progress-to-join' 'uc4-report-progress' 'uc4-join-download-progress'
Add-Edge $uc4Root 'uc4-join-to-complete' 'uc4-join-download-progress' 'g45D0m5QW9cc8jciQBO7-28'

$settings = New-Object System.Xml.XmlWriterSettings
$settings.Indent = $true
$settings.Encoding = [System.Text.UTF8Encoding]::new($false)
$writer = [System.Xml.XmlWriter]::Create($diagramPath, $settings)
$document.Save($writer)
$writer.Dispose()
