# setup-permissions workflow

Install a safe, pre-approved tool allowlist into a project so agents stop prompting for harmless read-only commands (`ls`, `find`, `grep`, `git status`, ...) on every call. Destructive and state-changing commands intentionally stay prompt-required.

Run it: standalone (`/setup-permissions`), or automatically as part of `init-product` and `import-codebase`.

## What gets installed

`.claude/settings.json` in the project root (Claude Code reads this per-project; commit it so the whole team benefits).

### The canonical safe allowlist

```json
{
  "permissions": {
    "allow": [
      "Bash(ls:*)",
      "Bash(find:*)",
      "Bash(tree:*)",
      "Bash(wc:*)",
      "Bash(head:*)",
      "Bash(tail:*)",
      "Bash(file:*)",
      "Bash(stat:*)",
      "Bash(du:*)",
      "Bash(grep:*)",
      "Bash(rg:*)",
      "Bash(diff:*)",
      "Bash(git status:*)",
      "Bash(git log:*)",
      "Bash(git diff:*)",
      "Bash(git show:*)",
      "Bash(git branch:*)",
      "Bash(git rev-parse:*)",
      "Bash(git remote:*)",
      "Bash(git stash list:*)",
      "Bash(scripts/check-state.sh:*)"
    ]
  }
}
```

Boundary rules:

- **Allow** = read-only inspection plus the kit's own checker. Nothing in the list can modify files, history, or leave the machine.
- **Never allow-list**: `rm`, `mv`, `sed -i`, `chmod`, `curl`/`wget`, `sudo`, `git add/commit/push/reset/checkout`, package installs, or anything network/state-changing. These stay on ask-by-default - that prompt is the control, not noise.
- **No deny entries** by default: deny blocks a command even when the user wants to approve it interactively. Ask-by-default is the right mode for everything outside the allowlist.

## Procedure

1. **Locate the target.** Default: the current project root. `init-product` and `import-codebase` pass their target explicitly.
2. **Merge, never clobber.** If `.claude/settings.json` exists: parse it, union the `permissions.allow` arrays (dedupe, keep every existing entry and any other keys untouched), and show the user the diff before writing. If it does not exist: create it with the canonical list.
3. **Ask about project-specific additions.** Offer the obvious candidates observed in the repo - the test runner and build commands (`npm test`, `mvn -q test`, `go test ./...`, lint commands). Add only what the user approves; these run code, so they are opt-in, not default.
4. **Scope choice.** Default to `.claude/settings.json` (committed, team-shared). If the user wants personal-only rules, put them in `.claude/settings.local.json` instead and ensure it is gitignored.
5. **Validate.** The written file must parse as JSON (`python3 -m json.tool` or equivalent). An invalid settings file silently disables all rules.
6. **Codex side.** Codex permissions are user-level, not per-repo: point the user at `approval_policy` / sandbox settings in `~/.codex/config.toml` (see `.codex/config.toml.example`). Do not write to the user's home config without being asked.

## Known limits (tell the user, don't hide)

- Rules are **prefix-matched**: `Bash(git status:*)` does not match `git -C /path status`. Agents should run git from the repo root; path-prefixed variants will still prompt.
- `find` is allow-listed for search, but `find ... -delete` exists. The allowlist trades that edge for ergonomics; if the project is sensitive, remove `Bash(find:*)` and accept the prompts.
- Compound commands (`a && b`) are evaluated per segment by current Claude Code versions; a non-listed segment still prompts. That is correct behaviour, not a bug.

## Report

- File written (created or merged) and the entries added vs. already present.
- Project-specific commands the user approved.
- Reminder: commit `.claude/settings.json`; never commit `settings.local.json`.

## Rules

- Additive merge only; never remove or weaken an existing entry without explicit instruction.
- Read-only commands only in the default list; anything that executes project code is opt-in.
- Show the diff before writing when a settings file already exists.
- Valid JSON or no write.
