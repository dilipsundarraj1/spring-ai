#!/bin/bash
# Reads PostToolUse hook input from stdin and regenerates TOC if the file is a .md
input=$(cat)
file_path=$(echo "$input" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('tool_input', {}).get('file_path', ''))" 2>/dev/null)

if [[ "$file_path" == *.md ]]; then
  doctoc --github --notitle "$file_path" > /dev/null 2>&1
fi
