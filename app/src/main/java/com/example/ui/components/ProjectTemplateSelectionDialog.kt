package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TemplateBoilerplate(
    val id: String,
    val name: String,
    val category: String, // "Frontend", "Styling", "Vanilla", "Apps"
    val tagline: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val tags: List<String>,
    val defaultProjectTitle: String,
    val defaultProjectDesc: String,
    val files: Map<String, String>
)

object TemplateBoilerplateLibrary {
    val templates = listOf(
        TemplateBoilerplate(
            id = "react_spa",
            name = "React 18 Single Page App",
            category = "Frontend",
            tagline = "React 18 + Babel + JSX + Hooks",
            description = "Complete React SPA setup running via CDN with Babel JSX transformer, stateful hooks, and component architecture.",
            icon = Icons.Default.Code,
            iconBgColor = Color(0xFF61DAFB),
            tags = listOf("React 18", "Babel", "JSX", "Hooks", "Component-Based"),
            defaultProjectTitle = "My React App",
            defaultProjectDesc = "Modern single page application built with React 18 and Babel JSX.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>React 18 Boilerplate</title>
  <!-- React 18 & ReactDOM -->
  <script src="https://unpkg.com/react@18/umd/react.development.js" crossorigin></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js" crossorigin></script>
  <!-- Babel for JSX -->
  <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="root"></div>
  <script type="text/babel" src="script.jsx"></script>
</body>
</html>""",
                "style.css" to """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #0f172a;
  color: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}
.card {
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 32px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
  text-align: center;
}
.react-logo {
  font-size: 48px;
  margin-bottom: 16px;
  animation: spin 10s linear infinite;
  display: inline-block;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
h1 {
  color: #38bdf8;
  font-size: 24px;
  margin-bottom: 8px;
}
p {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 24px;
}
.counter-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}
.btn {
  background: #0284c7;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn:hover {
  background: #0369a1;
  transform: translateY(-2px);
}
.badge {
  display: inline-block;
  background: #1e293b;
  border: 1px solid #38bdf8;
  color: #38bdf8;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
}""",
                "script.jsx" to """function App() {
  const [count, setCount] = React.useState(0);
  const [items, setItems] = React.useState(['State Management', 'JSX Components', 'Babel Runtime']);

  return (
    <div className="card">
      <div className="react-logo">⚛️</div>
      <h1>React 18 Application</h1>
      <p>Running natively in Local AI IDE</p>
      
      <div className="counter-box">
        <button className="btn" onClick={() => setCount(c => c - 1)}>-</button>
        <span style={{ fontSize: '28px', fontWeight: 'bold', minWidth: '40px' }}>{count}</span>
        <button className="btn" onClick={() => setCount(c => c + 1)}>+</button>
      </div>

      <div style={{ textAlign: 'left', marginTop: '20px' }}>
        <p style={{ fontWeight: 'bold', color: '#e2e8f0' }}>Included Features:</p>
        <ul style={{ paddingLeft: '20px', color: '#cbd5e1', fontSize: '13px' }}>
          {items.map((item, idx) => <li key={idx} style={{ marginBottom: '4px' }}>{item}</li>)}
        </ul>
      </div>

      <div style={{ marginTop: '24px' }}>
        <span className="badge">React 18.2 Ready</span>
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);"""
            )
        ),
        TemplateBoilerplate(
            id = "tailwind_dashboard",
            name = "Tailwind CSS v3 Dashboard",
            category = "Styling",
            tagline = "Tailwind CSS + Glassmorphism UI",
            description = "Responsive dark-mode analytical dashboard styled using Tailwind CSS v3 utility classes.",
            icon = Icons.Default.Palette,
            iconBgColor = Color(0xFF38BDF8),
            tags = listOf("Tailwind CSS", "Utility First", "Responsive", "Dashboard"),
            defaultProjectTitle = "Tailwind Dashboard",
            defaultProjectDesc = "Analytical stats dashboard powered by Tailwind CSS v3.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en" class="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Tailwind CSS Dashboard</title>
  <!-- Tailwind CSS v3 CDN -->
  <script src="https://cdn.tailwindcss.com"></script>
  <script>
    tailwind.config = {
      darkMode: 'class',
      theme: {
        extend: {
          colors: {
            brand: { 500: '#3b82f6', 600: '#2563eb' }
          }
        }
      }
    }
  </script>
  <link rel="stylesheet" href="style.css">
</head>
<body class="bg-slate-900 text-slate-100 min-h-screen font-sans">
  <div class="flex h-screen overflow-hidden">
    <!-- Sidebar -->
    <aside class="w-64 bg-slate-800/80 border-r border-slate-700/50 p-6 flex flex-col justify-between">
      <div>
        <div class="flex items-center gap-3 mb-8">
          <div class="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center font-bold text-xl shadow-lg shadow-blue-500/30">⚡</div>
          <span class="font-bold text-lg tracking-wide">Tailwind IDE</span>
        </div>
        <nav class="space-y-2">
          <a href="#" class="flex items-center gap-3 px-4 py-3 rounded-xl bg-blue-600/20 text-blue-400 font-medium">📊 Dashboard</a>
          <a href="#" class="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-700/50 transition">📁 Projects</a>
          <a href="#" class="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-700/50 transition">⚙️ Settings</a>
        </nav>
      </div>
      <div class="p-4 rounded-xl bg-slate-700/30 border border-slate-700 text-xs text-slate-400">
        Status: <span class="text-emerald-400 font-semibold">Online (Local AI)</span>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto p-8">
      <header class="flex justify-between items-center mb-8">
        <div>
          <h1 class="text-2xl font-bold">Project Overview</h1>
          <p class="text-sm text-slate-400">Real-time metrics & local engine stats</p>
        </div>
        <button id="refreshBtn" class="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl shadow-lg shadow-blue-600/30 transition">
          Refresh Metrics
        </button>
      </header>

      <!-- Stat Cards Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="p-6 rounded-2xl bg-slate-800/60 border border-slate-700/50 backdrop-blur-sm">
          <p class="text-xs font-medium text-slate-400 uppercase">CPU Usage</p>
          <p class="text-3xl font-extrabold text-blue-400 mt-2" id="cpuStat">18%</p>
          <p class="text-xs text-emerald-400 mt-1">↑ Normal Operation</p>
        </div>
        <div class="p-6 rounded-2xl bg-slate-800/60 border border-slate-700/50 backdrop-blur-sm">
          <p class="text-xs font-medium text-slate-400 uppercase">RAM Allocated</p>
          <p class="text-3xl font-extrabold text-emerald-400 mt-2">1.2 GB</p>
          <p class="text-xs text-slate-400 mt-1">GGUF Quantized Model</p>
        </div>
        <div class="p-6 rounded-2xl bg-slate-800/60 border border-slate-700/50 backdrop-blur-sm">
          <p class="text-xs font-medium text-slate-400 uppercase">Tokens / Sec</p>
          <p class="text-3xl font-extrabold text-purple-400 mt-2" id="tokStat">42.5</p>
          <p class="text-xs text-purple-300 mt-1">High Speed Inference</p>
        </div>
      </div>
    </main>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """/* Custom glassmorphism utilities */
.backdrop-blur-sm {
  backdrop-filter: blur(8px);
}""",
                "script.js" to """document.getElementById('refreshBtn').addEventListener('click', () => {
  const cpu = Math.floor(Math.random() * 25) + 10;
  const tok = (Math.random() * 15 + 35).toFixed(1);
  document.getElementById('cpuStat').textContent = cpu + '%';
  document.getElementById('tokStat').textContent = tok;
  console.log('Metrics updated:', { cpu, tok });
});"""
            )
        ),
        TemplateBoilerplate(
            id = "vanilla_js",
            name = "Vanilla ES6 Modular JS",
            category = "Vanilla",
            tagline = "Pure HTML5 + CSS3 + ES6 Modules",
            description = "Clean lightweight web architecture with semantic HTML, modern CSS design tokens, and modular JavaScript.",
            icon = Icons.Default.DeveloperMode,
            iconBgColor = Color(0xFFF7DF1E),
            tags = listOf("Vanilla JS", "ES6+", "CSS3 Variables", "Zero Dependencies"),
            defaultProjectTitle = "Vanilla JS App",
            defaultProjectDesc = "Lightweight zero-dependency web application.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Vanilla JS Web App</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="app-container">
    <header class="app-header">
      <div class="badge">Vanilla ES6</div>
      <h1>Lightweight Web Application</h1>
      <p>Zero external runtime dependencies. Built for speed.</p>
    </header>

    <main class="app-card">
      <div class="input-group">
        <input type="text" id="taskInput" placeholder="Add a new feature..." />
        <button id="addBtn" class="btn btn-primary">Add Item</button>
      </div>

      <ul id="itemList" class="item-list">
        <!-- Dynamic items populated by script.js -->
      </ul>
    </main>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """:root {
  --primary: #6366f1;
  --primary-hover: #4f46e5;
  --bg: #0f172a;
  --card-bg: #1e293b;
  --text: #f8fafc;
  --text-muted: #94a3b8;
  --border: #334155;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: var(--bg);
  color: var(--text);
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.app-container {
  width: 100%;
  max-width: 520px;
}
.app-header {
  text-align: center;
  margin-bottom: 24px;
}
.badge {
  display: inline-block;
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
  border: 1px solid var(--primary);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}
h1 { font-size: 24px; margin-bottom: 6px; }
p { color: var(--text-muted); font-size: 14px; }
.app-card {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.4);
}
.input-group {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
input[type="text"] {
  flex: 1;
  background: #0f172a;
  border: 1px solid var(--border);
  color: var(--text);
  padding: 12px 16px;
  border-radius: 8px;
  outline: none;
}
input[type="text"]:focus {
  border-color: var(--primary);
}
.btn {
  background: var(--primary);
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn:hover { background: var(--primary-hover); }
.item-list { list-style: none; }
.item-list li {
  background: #0f172a;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--border);
}""",
                "script.js" to """document.addEventListener('DOMContentLoaded', () => {
  const taskInput = document.getElementById('taskInput');
  const addBtn = document.getElementById('addBtn');
  const itemList = document.getElementById('itemList');

  const defaultItems = ['Local AI Code Completion', 'Offline Live Preview', 'File Explorer Tree'];

  function renderItem(text) {
    const li = document.createElement('li');
    li.textContent = text;
    const deleteBtn = document.createElement('span');
    deleteBtn.textContent = '✕';
    deleteBtn.style.cursor = 'pointer';
    deleteBtn.style.color = '#ef4444';
    deleteBtn.onclick = () => li.remove();
    li.appendChild(deleteBtn);
    itemList.appendChild(li);
  }

  defaultItems.forEach(renderItem);

  addBtn.addEventListener('click', () => {
    const val = taskInput.value.trim();
    if (val) {
      renderItem(val);
      taskInput.value = '';
    }
  });
});"""
            )
        ),
        TemplateBoilerplate(
            id = "vue_app",
            name = "Vue 3 Composition API",
            category = "Frontend",
            tagline = "Vue 3 + Reactive State + Composition API",
            description = "Vue 3 standalone application powered by Vue Global CDN with ref state, computed getters, and reactive lists.",
            icon = Icons.Default.Layers,
            iconBgColor = Color(0xFF42B883),
            tags = listOf("Vue 3", "Composition API", "CDN", "Reactive"),
            defaultProjectTitle = "My Vue App",
            defaultProjectDesc = "Vue 3 application using Composition API.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Vue 3 App</title>
  <!-- Vue 3 CDN -->
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div id="app"></div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """body {
  font-family: system-ui, sans-serif;
  background: #111827;
  color: #f3f4f6;
  padding: 40px 20px;
  display: flex;
  justify-content: center;
}
.vue-card {
  background: #1f2937;
  border: 1px solid #374151;
  border-radius: 16px;
  padding: 32px;
  max-width: 460px;
  width: 100%;
  text-align: center;
}
.vue-logo {
  font-size: 40px;
  margin-bottom: 12px;
}
.btn-vue {
  background: #10b981;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
}""",
                "script.js" to """const { createApp, ref, computed } = Vue;

createApp({
  setup() {
    const title = ref('Vue 3 Composition App');
    const count = ref(1);

    const doubleCount = computed(() => count.value * 2);

    function increment() {
      count.value++;
    }

    return { title, count, doubleCount, increment };
  },
  template: `
    <div class="vue-card">
      <div class="vue-logo">🟢</div>
      <h2>{{ title }}</h2>
      <p style="color: #9ca3af; margin: 12px 0;">Reactive Counter Example</p>
      <div style="font-size: 24px; margin: 16px 0;">
        Count: <strong>{{ count }}</strong> (Double: {{ doubleCount }})
      </div>
      <button class="btn-vue" @click="increment">Increment Count</button>
    </div>
  `
}).mount('#app');"""
            )
        ),
        TemplateBoilerplate(
            id = "bootstrap_dashboard",
            name = "Bootstrap 5 Admin Panel",
            category = "Styling",
            tagline = "Bootstrap 5.3 + Responsive Grid",
            description = "Responsive dashboard layout with Bootstrap 5 navbar, sidebar, stats cards, and table component.",
            icon = Icons.Default.Build,
            iconBgColor = Color(0xFF7952B3),
            tags = listOf("Bootstrap 5", "Admin Panel", "Responsive", "Components"),
            defaultProjectTitle = "Bootstrap Dashboard",
            defaultProjectDesc = "Admin dashboard template built with Bootstrap 5.3.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Bootstrap 5 Dashboard</title>
  <!-- Bootstrap 5 CSS -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="style.css">
</head>
<body class="bg-dark text-light">
  <nav class="navbar navbar-expand-lg navbar-dark bg-secondary border-bottom border-secondary">
    <div class="container-fluid">
      <a class="navbar-brand font-monospace" href="#">⚡ Bootstrap Admin</a>
    </div>
  </nav>

  <div class="container mt-4">
    <div class="row g-4">
      <div class="col-md-4">
        <div class="card bg-secondary text-white border-0 shadow">
          <div class="card-body">
            <h5 class="card-title text-info">Total Users</h5>
            <h2 class="display-6 fw-bold">1,248</h2>
            <p class="card-text text-light-50">Active monthly subscribers</p>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card bg-secondary text-white border-0 shadow">
          <div class="card-body">
            <h5 class="card-title text-success">Server Uptime</h5>
            <h2 class="display-6 fw-bold">99.9%</h2>
            <p class="card-text text-light-50">Local Docker runtime</p>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card bg-secondary text-white border-0 shadow">
          <div class="card-body">
            <h5 class="card-title text-warning">Pending Builds</h5>
            <h2 class="display-6 fw-bold">0</h2>
            <p class="card-text text-light-50">All tasks completed</p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>""",
                "style.css" to """body { font-family: 'Segoe UI', Tahoma, sans-serif; }""",
                "script.js" to """console.log('Bootstrap 5 admin initialized');"""
            )
        ),
        TemplateBoilerplate(
            id = "game_arcade",
            name = "HTML5 Canvas Space Arcade",
            category = "Apps",
            tagline = "Canvas 2D Game Engine + Audio Synthesizer",
            description = "Interactive HTML5 retro arcade space game with player ship movement, laser shooting, score tracking, and Web Audio API sounds.",
            icon = Icons.Default.PlayArrow,
            iconBgColor = Color(0xFFEC4899),
            tags = listOf("HTML5 Canvas", "Game Loop", "Web Audio API", "Arcade"),
            defaultProjectTitle = "Space Arcade Game",
            defaultProjectDesc = "Retro HTML5 arcade space shooter game.",
            files = mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Space Shooter Arcade</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="game-container">
    <div class="hud">
      <div>SCORE: <span id="scoreVal">0</span></div>
      <div>HEALTH: <span id="healthVal">100</span>%</div>
    </div>
    <canvas id="gameCanvas" width="600" height="400"></canvas>
    <div class="controls-hint">Use Left / Right Arrows or Touch to Move & Space to Shoot</div>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
                "style.css" to """body {
  background: #050515;
  color: #00f0ff;
  font-family: 'Courier New', Courier, monospace;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  margin: 0;
}
.game-container {
  text-align: center;
}
.hud {
  display: flex;
  justify-content: space-between;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  padding: 0 10px;
}
canvas {
  border: 2px solid #00f0ff;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 240, 255, 0.4);
  background: #02020a;
}
.controls-hint {
  font-size: 12px;
  color: #888;
  margin-top: 10px;
}""",
                "script.js" to """const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const scoreVal = document.getElementById('scoreVal');

let score = 0;
let player = { x: 280, y: 350, w: 40, h: 20, speed: 6 };
let lasers = [];
let enemies = [];

function spawnEnemy() {
  enemies.push({ x: Math.random() * (canvas.width - 30), y: 0, w: 30, h: 20, speed: 2 });
}
setInterval(spawnEnemy, 1200);

let keys = {};
window.addEventListener('keydown', e => keys[e.code] = true);
window.addEventListener('keyup', e => keys[e.code] = false);

window.addEventListener('keydown', e => {
  if (e.code === 'Space') {
    lasers.push({ x: player.x + player.w / 2 - 2, y: player.y, w: 4, h: 10, speed: 8 });
  }
});

function update() {
  if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
  if (keys['ArrowRight'] && player.x + player.w < canvas.width) player.x += player.speed;

  lasers.forEach(l => l.y -= l.speed);
  lasers = lasers.filter(l => l.y > 0);

  enemies.forEach(e => e.y += e.speed);

  // Collision
  lasers.forEach((l, lIdx) => {
    enemies.forEach((e, eIdx) => {
      if (l.x < e.x + e.w && l.x + l.w > e.x && l.y < e.y + e.h && l.y + l.h > e.y) {
        score += 10;
        scoreVal.textContent = score;
        enemies.splice(eIdx, 1);
        lasers.splice(lIdx, 1);
      }
    });
  });

  enemies = enemies.filter(e => e.y < canvas.height);
}

function draw() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Player
  ctx.fillStyle = '#00f0ff';
  ctx.fillRect(player.x, player.y, player.w, player.h);

  // Lasers
  ctx.fillStyle = '#ff0055';
  lasers.forEach(l => ctx.fillRect(l.x, l.y, l.w, l.h));

  // Enemies
  ctx.fillStyle = '#00ff88';
  enemies.forEach(e => ctx.fillRect(e.x, e.y, e.w, e.h));
}

function gameLoop() {
  update();
  draw();
  requestAnimationFrame(gameLoop);
}
gameLoop();"""
            )
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectTemplateSelectionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCreateProject: (title: String, description: String, templateType: String, files: Map<String, String>) -> Unit
) {
    if (!isOpen) return

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTemplate by remember { mutableStateOf(TemplateBoilerplateLibrary.templates.first()) }
    var projectTitle by remember { mutableStateOf(selectedTemplate.defaultProjectTitle) }
    var projectDesc by remember { mutableStateOf(selectedTemplate.defaultProjectDesc) }

    val categories = listOf("All", "Frontend", "Styling", "Vanilla", "Apps")

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") TemplateBoilerplateLibrary.templates
        else TemplateBoilerplateLibrary.templates.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Initialize New Project", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Select a starter boilerplate template", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("template_category_$cat")
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Template Cards List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTemplates) { tmpl ->
                        val isSelected = tmpl.id == selectedTemplate.id
                        Card(
                            onClick = {
                                selectedTemplate = tmpl
                                projectTitle = tmpl.defaultProjectTitle
                                projectDesc = tmpl.defaultProjectDesc
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("template_card_${tmpl.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tmpl.iconBgColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tmpl.icon,
                                        contentDescription = null,
                                        tint = tmpl.iconBgColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(tmpl.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = tmpl.tagline,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = tmpl.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        tmpl.tags.take(3).forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = "#$tag",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Project Inputs section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input")
                    )
                    OutlinedTextField(
                        value = projectDesc,
                        onValueChange = { projectDesc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_description_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectTitle.isNotBlank()) {
                        onCreateProject(
                            projectTitle,
                            projectDesc,
                            selectedTemplate.name,
                            selectedTemplate.files
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("confirm_create_template_project_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Initialize Project", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_template_project_button")
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}
