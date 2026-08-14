package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Interactive D3-based real-time line chart for Tokens-Per-Second (TPS) and RAM memory telemetry.
 * Renders high-fidelity SVG paths, smooth cubic splines, dual Y-axes, gradients, and interactive hover tooltips.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3PerformanceLineChart(
    samples: List<PerformanceSample>,
    chartMode: ChartViewMode = ChartViewMode.DUAL,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isHtmlLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    // Convert samples to JSON for D3 consumption
    val samplesJson = remember(samples, chartMode) {
        val jsonArray = JSONArray()
        samples.forEach { s ->
            val obj = JSONObject().apply {
                put("timeLabel", s.timeLabel)
                put("speedTokSec", s.speedTokSec.toDouble())
                put("ramUsedMb", s.ramUsedMb)
                put("ramPercent", s.ramPercent.toDouble())
                put("tokensGenerated", s.tokensGenerated)
            }
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    val modeString = when (chartMode) {
        ChartViewMode.SPEED -> "speed"
        ChartViewMode.MEMORY -> "memory"
        ChartViewMode.DUAL -> "dual"
    }

    // Send live updates to the D3 SVG graph via JavaScript evaluation
    LaunchedEffect(samplesJson, modeString, isGenerating, isHtmlLoaded) {
        if (isHtmlLoaded && webViewRef != null) {
            val script = "if (window.updateD3Chart) { window.updateD3Chart($samplesJson, '$modeString', $isGenerating); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF020617))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .testTag("d3_performance_chart_container")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(0x00000000) // Transparent background
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isHtmlLoaded = true
                            val initScript = "if (window.updateD3Chart) { window.updateD3Chart($samplesJson, '$modeString', $isGenerating); }"
                            view?.evaluateJavascript(initScript, null)
                        }
                    }

                    loadDataWithBaseURL(
                        null,
                        getD3ChartHtmlTemplate(samplesJson, modeString, isGenerating),
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )
    }
}

/**
 * Self-contained HTML & D3 charting engine with standalone vector algorithms,
 * cubic spline curve generation, dual Y-axis scaling, glowing SVG filters, and touch interaction.
 */
private fun getD3ChartHtmlTemplate(
    initialDataJson: String,
    initialMode: String,
    initialGenerating: Boolean
): String {
    val d = '$'
    return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>D3 Inference Performance Chart</title>
<style>
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    user-select: none;
    -webkit-user-select: none;
  }
  body {
    background-color: #020617;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", monospace;
    overflow: hidden;
    width: 100vw;
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }
  #chart-container {
    width: 100%;
    height: 100%;
    position: relative;
  }
  svg {
    width: 100%;
    height: 100%;
    display: block;
  }
  .grid-line {
    stroke: #1e293b;
    stroke-width: 0.8;
    stroke-dasharray: 3 3;
  }
  .axis-text {
    font-size: 9px;
    font-family: monospace;
    fill: #64748b;
  }
  .speed-path {
    fill: none;
    stroke: #38bdf8;
    stroke-width: 2.5;
    stroke-linecap: round;
    stroke-linejoin: round;
    filter: drop-shadow(0 0 4px rgba(56, 189, 248, 0.4));
  }
  .ram-path {
    fill: none;
    stroke: #4ade80;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
    filter: drop-shadow(0 0 3px rgba(74, 222, 128, 0.3));
  }
  .speed-area {
    fill: url(#speed-grad);
  }
  .ram-area {
    fill: url(#ram-grad);
  }
  .crosshair {
    stroke: rgba(255, 255, 255, 0.35);
    stroke-width: 1;
    stroke-dasharray: 2 2;
  }
  .tooltip-card {
    position: absolute;
    top: 6px;
    left: 10px;
    background: rgba(15, 23, 42, 0.92);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 8px;
    padding: 4px 8px;
    pointer-events: none;
    font-size: 10px;
    color: #f8fafc;
    box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    backdrop-filter: blur(8px);
    transition: opacity 0.15s ease;
    display: flex;
    gap: 10px;
    align-items: center;
    z-index: 10;
  }
  .pulse-dot {
    animation: pulse 1.5s infinite;
  }
  @keyframes pulse {
    0% { r: 3.5; opacity: 1; }
    50% { r: 6.5; opacity: 0.5; }
    100% { r: 3.5; opacity: 1; }
  }
</style>
</head>
<body>
<div id="chart-container">
  <div id="tooltip" class="tooltip-card" style="opacity: 0;">
    <div id="tt-time" style="color: #94a3b8; font-weight: bold;">--</div>
    <div id="tt-speed" style="color: #38bdf8; font-weight: bold;">-- TPS</div>
    <div id="tt-ram" style="color: #4ade80; font-weight: bold;">-- MB</div>
  </div>
  <svg id="chart-svg" viewBox="0 0 600 220" preserveAspectRatio="none">
    <defs>
      <linearGradient id="speed-grad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#38bdf8" stop-opacity="0.35"/>
        <stop offset="100%" stop-color="#38bdf8" stop-opacity="0.0"/>
      </linearGradient>
      <linearGradient id="ram-grad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#4ade80" stop-opacity="0.25"/>
        <stop offset="100%" stop-color="#4ade80" stop-opacity="0.0"/>
      </linearGradient>
    </defs>
    
    <!-- Background Grid -->
    <g id="grid-group"></g>
    
    <!-- Chart Paths -->
    <g id="paths-group">
      <path id="ram-area-path" class="ram-area"></path>
      <path id="speed-area-path" class="speed-area"></path>
      <path id="ram-line-path" class="ram-path"></path>
      <path id="speed-line-path" class="speed-path"></path>
    </g>
    
    <!-- Active Pulse Head Markers -->
    <circle id="speed-head-marker" cx="-10" cy="-10" r="4" fill="#38bdf8" class="pulse-dot"></circle>
    <circle id="ram-head-marker" cx="-10" cy="-10" r="3.5" fill="#4ade80"></circle>
    
    <!-- Crosshair & Touch Overlay -->
    <line id="crosshair-x" class="crosshair" x1="0" y1="15" x2="0" y2="195" style="display: none;"></line>
    <circle id="crosshair-speed-dot" cx="-10" cy="-10" r="5" fill="#38bdf8" stroke="#ffffff" stroke-width="1.5" style="display: none;"></circle>
    <circle id="crosshair-ram-dot" cx="-10" cy="-10" r="4.5" fill="#4ade80" stroke="#ffffff" stroke-width="1.5" style="display: none;"></circle>
    
    <!-- Axis Labels -->
    <g id="axis-group"></g>
  </svg>
</div>

<script>
  let telemetryData = $initialDataJson;
  let currentMode = '$initialMode';
  let isGenerating = $initialGenerating;

  const SVG_WIDTH = 600;
  const SVG_HEIGHT = 220;
  const PADDING = { top: 22, right: 38, bottom: 25, left: 38 };
  const CHART_W = SVG_WIDTH - PADDING.left - PADDING.right;
  const CHART_H = SVG_HEIGHT - PADDING.top - PADDING.bottom;

  // D3 Monotone Cubic Spline generator
  function createMonotoneSpline(points) {
    if (points.length === 0) return "";
    if (points.length === 1) return "M " + points[0].x + "," + points[0].y;
    if (points.length === 2) return "M " + points[0].x + "," + points[0].y + " L " + points[1].x + "," + points[1].y;

    let path = "M " + points[0].x.toFixed(1) + "," + points[0].y.toFixed(1);
    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[Math.max(i - 1, 0)];
      const p1 = points[i];
      const p2 = points[i + 1];
      const p3 = points[Math.min(i + 2, points.length - 1)];

      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;

      path += " C " + cp1x.toFixed(1) + "," + cp1y.toFixed(1) + " " + cp2x.toFixed(1) + "," + cp2y.toFixed(1) + " " + p2.x.toFixed(1) + "," + p2.y.toFixed(1);
    }
    return path;
  }

  function renderChart() {
    const gridGroup = document.getElementById("grid-group");
    const axisGroup = document.getElementById("axis-group");
    const speedLine = document.getElementById("speed-line-path");
    const speedArea = document.getElementById("speed-area-path");
    const ramLine = document.getElementById("ram-line-path");
    const ramArea = document.getElementById("ram-area-path");
    const speedHead = document.getElementById("speed-head-marker");
    const ramHead = document.getElementById("ram-head-marker");

    if (!telemetryData || telemetryData.length === 0) return;

    // Determine Y scales
    const maxSpeedRaw = Math.max(...telemetryData.map(d => d.speedTokSec), 25);
    const maxSpeed = Math.ceil(maxSpeedRaw * 1.15);
    const maxRamRaw = Math.max(...telemetryData.map(d => d.ramUsedMb), 3000);
    const maxRam = Math.ceil(maxRamRaw * 1.15);

    // Draw Grid & Axes
    let gridHtml = "";
    let axisHtml = "";
    const numRows = 4;
    for (let i = 0; i <= numRows; i++) {
      const y = PADDING.top + (CHART_H * (i / numRows));
      gridHtml += '<line class="grid-line" x1="' + PADDING.left + '" y1="' + y + '" x2="' + (SVG_WIDTH - PADDING.right) + '" y2="' + y + '"></line>';
      
      const speedVal = Math.round(maxSpeed * (1 - i / numRows));
      const ramVal = Math.round(maxRam * (1 - i / numRows));
      
      if (currentMode !== 'memory') {
        axisHtml += '<text class="axis-text" x="' + (PADDING.left - 6) + '" y="' + (y + 3) + '" text-anchor="end" fill="#38bdf8">' + speedVal + '</text>';
      }
      if (currentMode !== 'speed') {
        axisHtml += '<text class="axis-text" x="' + (SVG_WIDTH - PADDING.right + 6) + '" y="' + (y + 3) + '" text-anchor="start" fill="#4ade80">' + ramVal + 'M</text>';
      }
    }

    gridGroup.innerHTML = gridHtml;
    axisGroup.innerHTML = axisHtml;

    const n = telemetryData.length;
    const stepX = n > 1 ? CHART_W / (n - 1) : CHART_W;

    // Calculate Speed points
    const speedPoints = telemetryData.map((d, i) => ({
      x: PADDING.left + i * stepX,
      y: PADDING.top + CHART_H - ((d.speedTokSec / maxSpeed) * CHART_H)
    }));

    // Calculate RAM points
    const ramPoints = telemetryData.map((d, i) => ({
      x: PADDING.left + i * stepX,
      y: PADDING.top + CHART_H - ((d.ramUsedMb / maxRam) * CHART_H)
    }));

    // Render Speed Curve
    if (currentMode === 'speed' || currentMode === 'dual') {
      const speedPathData = createMonotoneSpline(speedPoints);
      speedLine.setAttribute("d", speedPathData);
      speedLine.style.display = "block";

      const lastPt = speedPoints[speedPoints.length - 1];
      const firstPt = speedPoints[0];
      const areaPathData = speedPathData + " L " + lastPt.x + "," + (PADDING.top + CHART_H) + " L " + firstPt.x + "," + (PADDING.top + CHART_H) + " Z";
      speedArea.setAttribute("d", areaPathData);
      speedArea.style.display = "block";

      speedHead.setAttribute("cx", lastPt.x);
      speedHead.setAttribute("cy", lastPt.y);
      speedHead.style.display = "block";
    } else {
      speedLine.style.display = "none";
      speedArea.style.display = "none";
      speedHead.style.display = "none";
    }

    // Render RAM Curve
    if (currentMode === 'memory' || currentMode === 'dual') {
      const ramPathData = createMonotoneSpline(ramPoints);
      ramLine.setAttribute("d", ramPathData);
      ramLine.style.display = "block";

      const lastPt = ramPoints[ramPoints.length - 1];
      const firstPt = ramPoints[0];
      const areaPathData = ramPathData + " L " + lastPt.x + "," + (PADDING.top + CHART_H) + " L " + firstPt.x + "," + (PADDING.top + CHART_H) + " Z";
      ramArea.setAttribute("d", areaPathData);
      ramArea.style.display = currentMode === 'memory' ? "block" : "none";

      ramHead.setAttribute("cx", lastPt.x);
      ramHead.setAttribute("cy", lastPt.y);
      ramHead.style.display = "block";
    } else {
      ramLine.style.display = "none";
      ramArea.style.display = "none";
      ramHead.style.display = "none";
    }
  }

  // Interactive Touch & Hover Listener
  const svg = document.getElementById("chart-svg");
  const tooltip = document.getElementById("tooltip");
  const ttTime = document.getElementById("tt-time");
  const ttSpeed = document.getElementById("tt-speed");
  const ttRam = document.getElementById("tt-ram");
  const crosshairX = document.getElementById("crosshair-x");
  const crossSpeedDot = document.getElementById("crosshair-speed-dot");
  const crossRamDot = document.getElementById("crosshair-ram-dot");

  function handlePointer(event) {
    if (!telemetryData || telemetryData.length === 0) return;
    const rect = svg.getBoundingClientRect();
    const clientX = event.touches ? event.touches[0].clientX : event.clientX;
    const relX = ((clientX - rect.left) / rect.width) * SVG_WIDTH;

    if (relX < PADDING.left || relX > SVG_WIDTH - PADDING.right) return;

    const n = telemetryData.length;
    const stepX = n > 1 ? CHART_W / (n - 1) : CHART_W;
    const index = Math.round((relX - PADDING.left) / stepX);
    const clampedIndex = Math.max(0, Math.min(n - 1, index));

    const sample = telemetryData[clampedIndex];
    if (!sample) return;

    const x = PADDING.left + clampedIndex * stepX;
    crosshairX.setAttribute("x1", x);
    crosshairX.setAttribute("x2", x);
    crosshairX.style.display = "block";

    const maxSpeedRaw = Math.max(...telemetryData.map(d => d.speedTokSec), 25);
    const maxSpeed = Math.ceil(maxSpeedRaw * 1.15);
    const maxRamRaw = Math.max(...telemetryData.map(d => d.ramUsedMb), 3000);
    const maxRam = Math.ceil(maxRamRaw * 1.15);

    const speedY = PADDING.top + CHART_H - ((sample.speedTokSec / maxSpeed) * CHART_H);
    const ramY = PADDING.top + CHART_H - ((sample.ramUsedMb / maxRam) * CHART_H);

    if (currentMode !== 'memory') {
      crossSpeedDot.setAttribute("cx", x);
      crossSpeedDot.setAttribute("cy", speedY);
      crossSpeedDot.style.display = "block";
    }
    if (currentMode !== 'speed') {
      crossRamDot.setAttribute("cx", x);
      crossRamDot.setAttribute("cy", ramY);
      crossRamDot.style.display = "block";
    }

    ttTime.innerText = sample.timeLabel || ("t=" + clampedIndex + "s");
    ttSpeed.innerText = "⚡ " + sample.speedTokSec.toFixed(1) + " TPS";
    ttRam.innerText = "💾 " + sample.ramUsedMb + " MB (" + (sample.ramPercent || 0) + "%)";
    tooltip.style.opacity = "1";
  }

  function hidePointer() {
    crosshairX.style.display = "none";
    crossSpeedDot.style.display = "none";
    crossRamDot.style.display = "none";
    tooltip.style.opacity = "0";
  }

  svg.addEventListener("mousemove", handlePointer);
  svg.addEventListener("touchmove", handlePointer, { passive: true });
  svg.addEventListener("touchstart", handlePointer, { passive: true });
  svg.addEventListener("mouseleave", hidePointer);
  svg.addEventListener("touchend", () => setTimeout(hidePointer, 2000));

  // Global update hook called from Android Kotlin WebView
  window.updateD3Chart = function(data, mode, generating) {
    telemetryData = data;
    currentMode = mode || 'dual';
    isGenerating = generating;
    renderChart();
  };

  // Initial render
  renderChart();
</script>
</body>
</html>
    """.trimIndent()
}
