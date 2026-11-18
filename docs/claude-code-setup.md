<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Claude Code Setup Guide](#claude-code-setup-guide)
  - [Why AI Coding Assistants Matter Now](#why-ai-coding-assistants-matter-now)
    - [Why Learning Multiple Assistants Matters](#why-learning-multiple-assistants-matters)
  - [1. Prerequisites](#1-prerequisites)
  - [2. Install Claude Code](#2-install-claude-code)
    - [Choosing a Plan](#choosing-a-plan)
    - [Option A — Native Install (Recommended)](#option-a--native-install-recommended)
    - [Option B — Homebrew (macOS)](#option-b--homebrew-macos)
    - [Option C — WinGet (Windows)](#option-c--winget-windows)
  - [3. Initialize a Local Git Repository](#3-initialize-a-local-git-repository)
  - [4. Authenticate with Anthropic](#4-authenticate-with-anthropic)
  - [5. Verify Installation](#5-verify-installation)
  - [6. Navigate to Your Project](#6-navigate-to-your-project)
  - [7. Start Claude Code](#7-start-claude-code)
  - [8. Essential Commands & Shortcuts](#8-essential-commands--shortcuts)
    - [Shell Commands (run from your terminal)](#shell-commands-run-from-your-terminal)
    - [Session Commands (type inside Claude Code)](#session-commands-type-inside-claude-code)
    - [Run a Shell Command Without Leaving Claude Code](#run-a-shell-command-without-leaving-claude-code)
    - [Pass a One-Off Prompt Without Entering Interactive Mode](#pass-a-one-off-prompt-without-entering-interactive-mode)
  - [11. Troubleshooting](#11-troubleshooting)
    - [`claude: command not found` (macOS / Linux)](#claude-command-not-found-macos--linux)
    - [`claude` not recognized (Windows)](#claude-not-recognized-windows)
    - [Authentication Expired](#authentication-expired)
    - [Claude Does Not See My Project Files](#claude-does-not-see-my-project-files)
    - [Out of Context / Confused Responses](#out-of-context--confused-responses)
    - [Java / Gradle Errors When Running the App](#java--gradle-errors-when-running-the-app)
    - [Windows: Line Ending Issues (CRLF)](#windows-line-ending-issues-crlf)
  - [Quick Reference Card](#quick-reference-card)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Claude Code Setup Guide

## Why AI Coding Assistants Matter Now

You already know the value of coding with AI — you've experienced it firsthand with **GitHub Copilot**. Building on that, here's why it matters and where Claude Code fits in:

- **You've felt the speed already.** Copilot showed you how fast you move when an assistant handles the boilerplate and you focus on the problem.
- **The industry rewards this instinct.** The developers who ship fastest pair human judgment with AI execution. The question is no longer *whether* to code with AI — it's *how well* you can direct it.
- **Claude Code is agentic by design.** You give it a goal and it reads your entire codebase, reasons about architecture, writes and fixes code, runs tests, and manages Git — carrying out changes across your whole project in plain conversation.
- **Different tools, different strengths.** Copilot lives in your editor and shines at inline, in-flow coding (it has an agent mode too); Claude Code is terminal-first and built around whole-project, agent-driven work. Neither replaces the other.
- **Multi-tool fluency is the real skill.** Knowing which assistant to reach for — and when — is what will define the next decade of software engineering.

> **From here on, Claude Code is our coding assistant for the rest of the course.** You'll use it in every remaining section — exploring APIs, generating boilerplate, debugging integration issues, and reviewing your code. Getting comfortable with it now is not optional; it is part of the curriculum.
>
> It proves its worth on the more involved topics too, like **Observability** (wiring metrics, traces, and dashboards into your Spring AI app) and **MCP** (Model Context Protocol, building tool-calling servers and clients). These involve a lot of moving parts across multiple files, and having Claude read your full codebase and guide you through each step makes the difference between getting stuck and getting it done.

---

### Why Learning Multiple Assistants Matters

No single AI tool wins at everything, and the landscape shifts every few months — so fluency with more than one is now a core engineering skill:

- **Right tool for the job.** Knowing both means you always reach for the one that fits the task.
- **The fundamentals transfer.** Prompting, context, and reviewing output are universal — every tool strengthens skills that outlast any product.
- **Adaptability wins.** Developers who pick up new assistants quickly stay productive whatever their team standardizes on.
- **Better judgment.** Using several tools teaches you where AI excels and where it needs supervision.

> **The goal isn't loyalty to a tool — it's fluency with the workflow.** For the rest of this course, Claude Code is that tool.

---

A step-by-step guide for setting up and using Claude Code in the terminal for the Spring AI course.

> **Official Quickstart:** https://code.claude.com/docs/en/quickstart

---
<!-- TOC -->
* [Claude Code Setup Guide](#claude-code-setup-guide)
  * [1. Prerequisites](#1-prerequisites)
  * [2. Install Claude Code](#2-install-claude-code)
  * [3. GitHub Setup & Claude + GitHub Integration](#3-github-setup--claude--github-integration)
    * [Why Claude and GitHub Work Well Together](#why-claude-and-github-work-well-together)
    * [Step 1 — Install the GitHub CLI](#step-1--install-the-github-cli)
    * [Step 2 — Configure Git Identity](#step-2--configure-git-identity)
    * [Step 3 — Initialize a Local Repository](#step-3--initialize-a-local-repository)
  * [4. Authenticate with Anthropic](#4-authenticate-with-anthropic)
  * [5. Verify Installation](#5-verify-installation)
  * [6. Navigate to Your Project](#6-navigate-to-your-project)
  * [7. Start Claude Code](#7-start-claude-code)
  * [8. Essential Commands & Shortcuts](#8-essential-commands--shortcuts)
    * [Shell Commands (run from your terminal)](#shell-commands-run-from-your-terminal)
    * [Session Commands (type inside Claude Code)](#session-commands-type-inside-claude-code)
    * [Run a Shell Command Without Leaving Claude Code](#run-a-shell-command-without-leaving-claude-code)
    * [Pass a One-Off Prompt Without Entering Interactive Mode](#pass-a-one-off-prompt-without-entering-interactive-mode)
  * [11. Troubleshooting](#11-troubleshooting)
    * [`claude: command not found` (macOS / Linux)](#claude-command-not-found-macos--linux)
    * [`claude` not recognized (Windows)](#claude-not-recognized-windows)
    * [Authentication Expired](#authentication-expired)
    * [Claude Does Not See My Project Files](#claude-does-not-see-my-project-files)
    * [Out of Context / Confused Responses](#out-of-context--confused-responses)
    * [Java / Gradle Errors When Running the App](#java--gradle-errors-when-running-the-app)
    * [Windows: Line Ending Issues (CRLF)](#windows-line-ending-issues-crlf)
  * [Quick Reference Card](#quick-reference-card)
<!-- TOC -->
---

## 1. Prerequisites

Before installing Claude Code, make sure you have the following:

| Tool | Version | Check Command |
|------|---------|---------------|
| Java | 21 or higher | `java --version` |
| Git | Any recent version | `git --version` |

Run each check command in your terminal. If any fail with "command not found", install the missing tool before continuing.

> **No extra tools required.** The recommended native install (Section 2, Option A) bundles everything — just run the one-liner for your OS.

---

## 2. Install Claude Code

> **New to Claude Code?** Read the official overview first to understand how it works: https://code.claude.com/docs/en/overview

### Choosing a Plan

Claude Code runs on either a Claude subscription or pay-as-you-go API credits. Any of these works for the course — pick what fits your usage:

| Plan | Best for | Notes |
|------|----------|-------|
| **Pro** | Light, individual use | Lowest-cost subscription; enough for everyday tasks |
| **Max (5×/20×)** | Heavy daily use | Higher usage limits for long, frequent sessions |
| **Team / Enterprise** | Organizations | Per-seat plans with admin controls |
| **API (Console)** | Pay-as-you-go | Billed per token via pre-paid credits — no subscription |

> **See current plans and pricing:** https://www.claude.com/pricing — compare limits and cost before you subscribe.

---

### Option A — Native Install (Recommended)

The official one-liner installs Claude Code with no additional dependencies and **auto-updates in the background**.

**macOS, Linux, and WSL:**

```bash
curl -fsSL https://claude.ai/install.sh | bash
```

**Windows — PowerShell:**

```powershell
irm https://claude.ai/install.ps1 | iex
```

**Windows — Command Prompt (CMD):**

```batch
curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd && del install.cmd
```

> **Which shell am I in?** Your prompt shows `PS C:\` in PowerShell and `C:\` (no `PS`) in CMD. If you see `The token '&&' is not a valid statement separator`, you are in PowerShell. If you see `'irm' is not recognized`, you are in CMD.

> **Windows Git:** Install [Git for Windows](https://git-scm.com/downloads/win) so Claude Code can use the Bash tool. Without it, Claude falls back to PowerShell. WSL users do not need Git for Windows.

---

### Option B — Homebrew (macOS)

```bash
brew install --cask claude-code
```

> Homebrew does **not** auto-update. Run `brew upgrade claude-code` periodically to get the latest version.

---

### Option C — WinGet (Windows)

```powershell
winget install Anthropic.ClaudeCode
```

> WinGet does **not** auto-update. Run `winget upgrade Anthropic.ClaudeCode` to update.

---

> **Linux package managers:** You can also install with `apt`, `dnf`, or `apk`. See the [official setup guide](https://code.claude.com/docs/en/setup#install-with-linux-package-managers) for details.

---

## 3. Initialize a Local Git Repository

You don't need GitHub or a remote for this course. A **local** Git repository is enough — it gives Claude Code a clean baseline so you can see exactly which files it edits after every change (via `git status` and `git diff`).

If your Spring AI project folder is not yet a Git repository:

```bash
cd ~/Dilip/code-with-dilip/spring-ai   # go to your project
git init                               # initialize an empty local repo
git add .                              # stage all files
git commit -m "initial commit"         # snapshot the starting point
```

If it is already a Git repo (check with `git status`), skip this step.

> **Why this matters:** With a committed baseline, any change Claude Code makes shows up clearly. Run `git status` to see which files changed and `git diff` to review the exact edits before keeping them — no remote or push required.

---

## 4. Authenticate with Anthropic

Claude Code requires an Anthropic account. Start an interactive session and you will be prompted to log in on first use:

```bash
claude
```

On first launch, Claude Code will:
1. Open your browser to `claude.ai`
2. Ask you to log in or create an account
3. Store your credentials locally — you only do this once

**Supported account types:**
- Claude Pro, Max, Team, or Enterprise (recommended)
- Claude Console (API access with pre-paid credits)
- Amazon Bedrock, Google Cloud Vertex AI, or Microsoft Azure

Your credentials are stored at `~/.claude/` (macOS/Linux) or `%USERPROFILE%\.claude\` (Windows).

**To switch accounts or re-authenticate later**, type `/login` inside a running Claude Code session:

```
> /login
```

---

## 5. Verify Installation

Confirm everything is working:

```bash
claude --version
```

The command prints a version number followed by `(Claude Code)`.

---

## 6. Navigate to Your Project

Move into the Spring AI project directory before starting Claude Code.

**macOS / Linux:**

```bash
cd ~/Dilip/code-with-dilip/spring-ai
```

**Windows (PowerShell):**

```powershell
cd C:\Users\<YourUsername>\Dilip\code-with-dilip\spring-ai\mcp
```

Claude Code uses the current working directory as its context — it reads your project files from here.

---

## 7. Start Claude Code

Launch the interactive terminal session:

```bash
claude
```

You will see the Claude Code prompt:

```
Claude Code  (type /help for commands)
> 
```

You are now in an interactive session. Type naturally — ask questions, request code changes, or run commands.

---

## 8. Essential Commands & Shortcuts

### Shell Commands (run from your terminal)

| Command | What it does |
|---------|-------------|
| `claude` | Start interactive session |
| `claude "task"` | Run a one-time task and exit |
| `claude -p "query"` | One-off query, then exit |
| `claude -c` | Continue the most recent conversation |
| `claude -r` | Resume a previous conversation (pick from list) |

### Session Commands (type inside Claude Code)

| Command | What it does |
|---------|-------------|
| `/help` | Show all available commands |
| `/clear` | Clear the current conversation context |
| `/login` | Switch accounts or re-authenticate |
| `/config` | Open settings (model, theme, etc.) |
| `/review` | Code review the current branch diff |
| `/run` | Start the app and verify a change works |
| `/init` | Generate a CLAUDE.md for the project |
| `/exit` or `Ctrl+D` twice | Exit Claude Code |

> **Permission mode shortcut:** Press `Shift+Tab` inside a session to cycle through permission modes — default (asks before each change) → `acceptEdits` (auto-approves file edits) → `plan` (proposes changes without editing).

### Run a Shell Command Without Leaving Claude Code

Prefix any shell command with `!` to run it directly:

```
> ! ./gradlew bootRun          # macOS / Linux
> ! .\gradlew.bat bootRun      # Windows
> ! git status
> ! curl http://localhost:8080/actuator/health
```

### Pass a One-Off Prompt Without Entering Interactive Mode

```bash
# Ask a question and get a response, then exit
claude -p "Explain what MCP transports are in this project"

# Point Claude at a specific file
claude -p "Review this file for issues" --file src/main/java/MyService.java
```

---

## 11. Troubleshooting

### `claude: command not found` (macOS / Linux)

Re-run the native installer — it sets up the PATH automatically:

```bash
curl -fsSL https://claude.ai/install.sh | bash
```

Then open a new terminal window and verify:

```bash
claude --version
```

If it still fails, manually add the install directory to your PATH:

```bash
echo 'export PATH="$HOME/.claude/local:$PATH"' >> ~/.zshrc   # macOS
echo 'export PATH="$HOME/.claude/local:$PATH"' >> ~/.bashrc  # Linux
source ~/.zshrc    # macOS
source ~/.bashrc   # Linux
```

### `claude` not recognized (Windows)

Re-run the native installer in PowerShell:

```powershell
irm https://claude.ai/install.ps1 | iex
```

Then open a new terminal and verify:

```powershell
claude --version
```

If the install fails, see the [official troubleshooting guide](https://code.claude.com/docs/en/troubleshoot-install) for error-specific fixes.

### Authentication Expired

If Claude stops responding with an auth error:

```bash
claude auth login
```

### Claude Does Not See My Project Files

Make sure you launched `claude` from inside the project directory:

```bash
# macOS / Linux
pwd   # should show your project path

# Windows
cd    # shows current directory
```

Then run `claude` from that directory.

### Out of Context / Confused Responses

Run `/clear` to reset the conversation context and start fresh.

### Java / Gradle Errors When Running the App

**macOS / Linux:**

```bash
! ./gradlew clean build -q
```

**Windows:**

```powershell
! .\gradlew.bat clean build -q
```

Paste the error output into Claude Code and ask it to fix it.

### Windows: Line Ending Issues (CRLF)

If `./gradlew` fails with "bad interpreter" on WSL or Git Bash:

```bash
sed -i 's/\r//' gradlew
chmod +x gradlew
```

---

## Quick Reference Card

| Task | macOS / Linux | Windows |
|------|--------------|---------|
| Install (recommended) | `curl -fsSL https://claude.ai/install.sh \| bash` | PowerShell: `irm https://claude.ai/install.ps1 \| iex` |
| Install (Homebrew) | `brew install --cask claude-code` | — |
| Install (WinGet) | — | `winget install Anthropic.ClaudeCode` |
| First login | `claude` (follows browser) | `claude` (follows browser) |
| Go to project | `cd ~/path/to/project` | `cd C:\path\to\project` |
| Start session | `claude` | `claude` |
| Continue last session | `claude -c` | `claude -c` |
| One-off prompt | `claude -p "your question"` | `claude -p "your question"` |
| Run shell cmd | `! ./gradlew bootRun` | `! .\gradlew.bat bootRun` |
| Switch accounts | `/login` (inside session) | `/login` (inside session) |
| Cycle permission mode | `Shift+Tab` | `Shift+Tab` |
| Help | `/help` | `/help` |
| Clear context | `/clear` | `/clear` |
| Exit | `/exit` or `Ctrl+D` twice | `/exit` or `Ctrl+D` twice |
