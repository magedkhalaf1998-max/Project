# Screenshot Question Helper

A small, user-controlled Python app that captures the primary monitor (or accepts an image file) and asks a vision-capable AI model to explain the question. It is intended as a learning aid, not a way to bypass rules or assessments.

## Privacy and responsible use

- Screen capture is started only when you run the program.
- By default, the captured screen is opened for review and must be explicitly approved before upload.
- The image is sent to the AI provider configured by your API key. Do not send sensitive information; review the provider's privacy and data-retention terms.
- Use this only where screen assistance and AI tools are permitted. Follow your school, workplace, and application rules.
- The AI can be wrong. Check its explanation and answer.

## Setup

Requires Python 3.10+.

```bash
python -m venv .venv
# macOS/Linux
source .venv/bin/activate
# Windows PowerShell
# .venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

Set your API key in the shell (do not commit it):

```bash
# macOS/Linux
export OPENAI_API_KEY="your-api-key"
# Windows PowerShell
$env:OPENAI_API_KEY = "your-api-key"
```

Optionally set `OPENAI_MODEL` to another vision-capable model available to your account.

## Run

Capture and review the primary monitor before sending:

```bash
python main.py
```

Use a saved image instead (no screen capture):

```bash
python main.py --image ./question.png
```

The `--no-preview` option skips opening a preview but still asks for confirmation before upload. The program does not save captured screenshots to disk.
