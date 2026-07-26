package com.example.util

data class BoilerplateTemplate(
    val id: String,
    val title: String,
    val language: String, // "html", "css", "js", "python", "json", "markdown", "text"
    val category: String, // "Skeleton", "Components", "Layout", "Form", "Animation", "Async", "Storage", "Script", "Config", "Docs"
    val description: String,
    val suggestedFileName: String,
    val code: String
)

object BoilerplateTemplates {

    val allTemplates = listOf(
        // HTML Templates
        BoilerplateTemplate(
            id = "html_app_shell",
            title = "HTML5 Application Shell",
            language = "html",
            category = "Skeleton",
            description = "Complete standard HTML5 page structure with semantic header, main container, and script links.",
            suggestedFileName = "index.html",
            code = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Local AI Web App</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
  <header class="app-header">
    <div class="logo">⚡ Local AI App</div>
    <nav class="nav-links">
      <a href="#features">Features</a>
      <a href="#about">About</a>
    </nav>
  </header>

  <main class="container">
    <h1>Welcome to Mobile Web IDE</h1>
    <p>Build, test, and render offline web applications.</p>
    <button id="mainBtn" class="btn">Click Me</button>
  </main>

  <script src="js/script.js"></script>
</body>
</html>"""
        ),
        BoilerplateTemplate(
            id = "html_navbar_hero",
            title = "Responsive Navbar & Hero Banner",
            language = "html",
            category = "Components",
            description = "Flexbox navigation header bar with brand logo, nav links, CTA button, and hero banner.",
            suggestedFileName = "index.html",
            code = """<header class="navbar">
  <div class="nav-brand">🌐 BrandLogo</div>
  <ul class="nav-menu">
    <li><a href="#">Home</a></li>
    <li><a href="#">Projects</a></li>
    <li><a href="#">Contact</a></li>
  </ul>
  <button class="cta-btn">Get Started</button>
</header>

<section class="hero">
  <div class="hero-content">
    <h1 class="hero-title">Build High-Performance Apps</h1>
    <p class="hero-subtitle">Offline code generation with real-time live preview.</p>
    <div class="hero-actions">
      <button class="btn btn-primary">Launch Project</button>
      <button class="btn btn-secondary">Learn More</button>
    </div>
  </div>
</section>"""
        ),

        // Python Templates
        BoilerplateTemplate(
            id = "py_cli_script",
            title = "Python CLI Utility Script",
            language = "python",
            category = "Script",
            description = "Python command line interface script with argument parsing, logging, and entry point.",
            suggestedFileName = "main.py",
            code = """#!/usr/bin/env python3
import sys
import json
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def process_data(data_dict: dict) -> dict:
    \"\"\"Process input dataset and compute metrics.\"\"\"
    logging.info("Processing data dict...")
    results = {
        "status": "success",
        "processed_keys": len(data_dict.keys()),
        "summary": "Processed via Local Mobile IDE"
    }
    return results

def main():
    print("⚡ Python Script running in Mobile IDE environment")
    sample_payload = {"app": "Local AI IDE", "version": "2.0.0"}
    output = process_data(sample_payload)
    print(json.dumps(output, indent=2))

if __name__ == "__main__":
    main()"""
        ),
        BoilerplateTemplate(
            id = "py_data_class",
            title = "Python Dataclass & Helper Utilities",
            language = "python",
            category = "Script",
            description = "Modern Python dataclasses for modeling structured objects and JSON serialization.",
            suggestedFileName = "models.py",
            code = """from dataclasses import dataclass, asdict
from typing import List, Optional
import json

@dataclass
class ProjectFile:
    filename: str
    extension: str
    content: str
    size_bytes: int

@dataclass
class Workspace:
    project_id: str
    title: str
    files: List[ProjectFile]

    def to_json(self) -> str:
        return json.dumps(asdict(self), indent=2)

if __name__ == "__main__":
    p1 = ProjectFile("main.py", "py", "print('hello')", 14)
    ws = Workspace("ws_01", "Mobile IDE Demo", [p1])
    print(ws.to_json())"""
        ),

        // JSON Templates
        BoilerplateTemplate(
            id = "json_manifest",
            title = "Web App Manifest & Config JSON",
            language = "json",
            category = "Config",
            description = "Standard PWA web app manifest with colors, icons, and display settings.",
            suggestedFileName = "manifest.json",
            code = """{
  "short_name": "MobileIDE",
  "name": "Offline Local AI Mobile IDE",
  "icons": [
    {
      "src": "images/icon-192.png",
      "type": "image/png",
      "sizes": "192x192"
    }
  ],
  "start_url": "./index.html",
  "background_color": "#0f172a",
  "theme_color": "#6366f1",
  "display": "standalone"
}"""
        ),
        BoilerplateTemplate(
            id = "json_package",
            title = "Project Metadata package.json",
            language = "json",
            category = "Config",
            description = "NPM style project dependencies and build scripts package declaration.",
            suggestedFileName = "package.json",
            code = """{
  "name": "mobile-local-ai-workspace",
  "version": "1.0.0",
  "description": "Offline Mobile IDE Project Workspace",
  "main": "js/script.js",
  "scripts": {
    "start": "echo 'Running project preview...'",
    "build": "echo 'Exporting .zip archive...'"
  },
  "keywords": ["ai", "mobile", "ide", "offline"],
  "author": "Local AI IDE User"
}"""
        ),

        // Markdown Templates
        BoilerplateTemplate(
            id = "md_readme",
            title = "Project Documentation README.md",
            language = "markdown",
            category = "Docs",
            description = "Structured README documentation with headers, installation instructions, and code blocks.",
            suggestedFileName = "README.md",
            code = """# 🚀 Local AI Mobile IDE Project

Welcome to your offline workspace created inside **Local AI Mobile IDE**.

## 📌 Features
- **Multi-Format Support**: `.html`, `.css`, `.js`, `.py`, `.json`, `.md`
- **Sub-folder Structure**: Organize assets into `/css`, `/js`, and `/images`
- **Zip Export & Import**: Compress or unpack complete web projects
- **On-Device AI Engine**: GGUF LLM code generation running 100% locally

## 🛠️ Usage
1. Edit your source files in the **Code Editor**.
2. Switch to **Live Preview** to render live HTML5/CSS3.
3. Open **Boilerplate Library** in the drawer to insert ready-to-use code snippets.

```bash
# Export project to zip
Click 'Zip Export' in the workspace drawer menu.
```
"""
        ),

        // CSS Templates
        BoilerplateTemplate(
            id = "css_reset_tokens",
            title = "CSS Reset & Design System Tokens",
            language = "css",
            category = "Skeleton",
            description = "CSS custom variables for theme colors, typography reset, and dark canvas defaults.",
            suggestedFileName = "css/style.css",
            code = """/* Modern CSS Reset & Design Tokens */
:root {
  --primary-color: #6366f1;
  --primary-hover: #4f46e5;
  --bg-color: #0f172a;
  --surface-color: #1e293b;
  --text-color: #f8fafc;
  --text-muted: #94a3b8;
  --border-color: #334155;
  --radius-md: 12px;
  --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  background-color: var(--bg-color);
  color: var(--text-color);
  font-family: var(--font-sans);
  line-height: 1.6;
  padding: 20px;
}"""
        ),

        // JS Templates
        BoilerplateTemplate(
            id = "js_dom_events",
            title = "DOM Initialization & Event Handlers",
            language = "javascript",
            category = "Skeleton",
            description = "DOMContentLoaded listener with element query selectors and button click toggle handlers.",
            suggestedFileName = "js/script.js",
            code = """// Wait for DOM to load
document.addEventListener('DOMContentLoaded', () => {
  console.log('⚡ DOM Content Loaded!');

  const mainBtn = document.getElementById('mainBtn');
  if (mainBtn) {
    mainBtn.addEventListener('click', () => {
      mainBtn.classList.toggle('active');
      console.log('Button clicked! Local IDE JavaScript executing.');
    });
  }
});"""
        )
    )
}
