#!/usr/bin/env bash
set -euo pipefail

echo "Preparing Claude Code scaffold in: $(pwd)"

# Create directories
mkdir -p \
  .ai/research \
  .ai/plans \
  .ai/reviews \
  .claude/skills/research-ticket \
  .claude/skills/implement-plan \
  .claude/skills/payment-safety-rules \
  .claude/skills/mifosx-legacy-patterns \
  .claude/skills/shared-contract-safety \
  .claude/skills/kmp-surface-impact \
  .claude/skills/api-contract-check \
  .claude/agents \
  docs/architecture \
  docs/api \
  docs/flows \
  docs/decisions \
  cmp-shared \
  feature \
  core \
  android \
  ios \
  build-logic \
  scripts

# Create empty files
touch \
  CLAUDE.md \
  .claude/settings.json \
  .claude/skills/research-ticket/SKILL.md \
  .claude/skills/implement-plan/SKILL.md \
  .claude/skills/payment-safety-rules/SKILL.md \
  .claude/skills/mifosx-legacy-patterns/SKILL.md \
  .claude/skills/shared-contract-safety/SKILL.md \
  .claude/skills/kmp-surface-impact/SKILL.md \
  .claude/skills/api-contract-check/SKILL.md \
  .claude/agents/researcher.md \
  .claude/agents/planner.md \
  .claude/agents/reviewer.md \
  .claude/agents/android-implementer.md \
  .claude/agents/ios-implementer.md \
  cmp-shared/CLAUDE.md \
  feature/CLAUDE.md \
  core/CLAUDE.md \
  android/CLAUDE.md \
  ios/CLAUDE.md \
  build-logic/CLAUDE.md \
  scripts/CLAUDE.md \
  .ai/research/.gitkeep \
  .ai/plans/.gitkeep \
  .ai/reviews/.gitkeep \
  docs/architecture/.gitkeep \
  docs/api/.gitkeep \
  docs/flows/.gitkeep \
  docs/decisions/.gitkeep

echo
echo "Done."
echo
echo "Created/ensured:"
printf '%s\n' \
  "  CLAUDE.md" \
  "  cmp-shared/CLAUDE.md" \
  "  feature/CLAUDE.md" \
  "  core/CLAUDE.md" \
  "  android/CLAUDE.md" \
  "  ios/CLAUDE.md" \
  "  build-logic/CLAUDE.md" \
  "  scripts/CLAUDE.md" \
  "  .claude/settings.json" \
  "  .claude/skills/*" \
  "  .claude/agents/*" \
  "  .ai/research" \
  "  .ai/plans" \
  "  .ai/reviews" \
  "  docs/architecture" \
  "  docs/api" \
  "  docs/flows" \
  "  docs/decisions"
