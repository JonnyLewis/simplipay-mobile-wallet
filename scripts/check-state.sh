#!/usr/bin/env bash
# check-state.sh - deterministic consistency checks for the codebot kit.
# Converts the kit's prose gates into machine-verifiable checks.
# Exit 0 = no errors (warnings allowed). Exit 1 = at least one error.
#
# Run from the repo root: scripts/check-state.sh

set -u
cd "$(dirname "$0")/.." || exit 1

ERRORS=0
WARNINGS=0

err()  { printf 'ERROR   %s\n' "$1"; ERRORS=$((ERRORS + 1)); }
warn() { printf 'WARNING %s\n' "$1"; WARNINGS=$((WARNINGS + 1)); }
ok()   { printf 'ok      %s\n' "$1"; }

BOOTSTRAPPED=1
if grep -q '{{PRODUCT_NAME}}' AGENTS.md 2>/dev/null; then
  BOOTSTRAPPED=0
  printf 'note    template mode: AGENTS.md still contains {{PRODUCT_NAME}} (run init-product)\n'
fi

# 1. Root CLAUDE.md must import @AGENTS.md.
if grep -q '@AGENTS.md' CLAUDE.md 2>/dev/null; then
  ok "root CLAUDE.md imports @AGENTS.md"
else
  err "root CLAUDE.md does not import @AGENTS.md"
fi

# 2. Every per-folder AGENTS.md needs a sibling CLAUDE.md shim importing it, and vice versa.
while IFS= read -r agents_file; do
  dir=$(dirname "$agents_file")
  [ "$dir" = "." ] && continue
  shim="$dir/CLAUDE.md"
  if [ ! -f "$shim" ]; then
    err "$dir has AGENTS.md but no CLAUDE.md shim"
  elif ! grep -q '@AGENTS.md' "$shim"; then
    err "$shim does not import @AGENTS.md"
  fi
done < <(find . -name 'AGENTS.md' -not -path './.git/*' -not -path './node_modules/*')

while IFS= read -r shim; do
  dir=$(dirname "$shim")
  [ "$dir" = "." ] && continue
  if [ ! -f "$dir/AGENTS.md" ]; then
    err "$shim exists but $dir/AGENTS.md is missing"
  fi
done < <(find . -name 'CLAUDE.md' -not -path './.git/*' -not -path './node_modules/*')
ok "per-folder AGENTS.md/CLAUDE.md pairing checked"

# 3. Context-file line budgets (root 200, per-folder 200 hard / 150 soft).
while IFS= read -r f; do
  lines=$(wc -l < "$f" | tr -d ' ')
  if [ "$lines" -gt 200 ]; then
    err "$f is $lines lines (budget 200) - split or evict per update-context"
  elif [ "$lines" -gt 150 ] && [ "$f" != "./AGENTS.md" ]; then
    warn "$f is $lines lines (soft budget 150)"
  fi
done < <(find . -name 'AGENTS.md' -not -path './.git/*' -not -path './node_modules/*')
ok "context-file line budgets checked"

# 4. Placeholder sweep (only meaningful after bootstrap).
if [ "$BOOTSTRAPPED" -eq 1 ]; then
  FOUNDATION="AGENTS.md README.md SESSION_STATE.md DEV_PLAN.md docs/01-product-vision.md docs/05-mvp-scope.md engineering/AGENTS.md"
  for f in $FOUNDATION; do
    if [ -f "$f" ] && grep -q '{{' "$f"; then
      err "foundation file $f still contains {{placeholders}}"
    fi
  done
  ok "foundation placeholder sweep done"
fi

# 5. Workflow wrappers: every shared workflow body should have a Claude skill wrapper.
for wf in workflows/agent/*.md; do
  name=$(basename "$wf" .md)
  case "$name" in review-lenses) continue ;; esac
  if [ ! -f ".claude/skills/$name/SKILL.md" ]; then
    warn "workflow $name has no .claude/skills/$name/SKILL.md wrapper"
  fi
done
ok "workflow/skill wrapper pairing checked"

# 6. Feature state consistency.
if [ -d engineering/features ]; then
  for ftdir in engineering/features/FT-*/; do
    [ -d "$ftdir" ] || continue
    ftid=$(basename "$ftdir")
    case "$ftid" in FT-000-example) continue ;; esac
    statusfile=""
    [ -f "$ftdir/status.md" ] && statusfile="$ftdir/status.md"
    [ -z "$statusfile" ] && [ -f "$ftdir/FEATURE.md" ] && statusfile="$ftdir/FEATURE.md"
    if [ -z "$statusfile" ]; then
      err "$ftid has neither status.md nor FEATURE.md"
      continue
    fi
    shortid=$(printf '%s' "$ftid" | grep -o 'FT-[0-9]*')
    if [ -f engineering/features/backlog.md ] && ! grep -q "$shortid" engineering/features/backlog.md; then
      err "$shortid missing from engineering/features/backlog.md"
    fi
  done
  ok "feature directories checked against backlog"
fi

# 7. SESSION_STATE next concrete action must exist and be non-template after bootstrap.
if [ -f SESSION_STATE.md ]; then
  next=$(awk '/## 4\. Next concrete action/{flag=1; next} /^## /{flag=0} flag' SESSION_STATE.md | grep -v '^<!--' | grep -v '^$' | head -3)
  if [ -z "$next" ]; then
    err "SESSION_STATE.md section 4 (next concrete action) is empty"
  elif [ "$BOOTSTRAPPED" -eq 1 ] && printf '%s' "$next" | grep -q '{{'; then
    err "SESSION_STATE.md section 4 still contains a placeholder"
  else
    ok "SESSION_STATE.md section 4 present"
  fi
else
  err "SESSION_STATE.md missing"
fi

# 8. Context registry rows must point at real files.
if [ -f engineering/agents/context-index.md ]; then
  while IFS= read -r path; do
    [ -f "$path" ] || err "context-index.md references missing file: $path"
  done < <(grep -o '`[^`]*AGENTS\.md`' engineering/agents/context-index.md | tr -d '\`' | sort -u)
  ok "context registry entries point at real files"
else
  warn "engineering/agents/context-index.md missing (run update-context)"
fi

printf '\n%d error(s), %d warning(s)\n' "$ERRORS" "$WARNINGS"
[ "$ERRORS" -eq 0 ]
