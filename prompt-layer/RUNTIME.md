# Runtime Prompt Assembly

Defines how prompts are dynamically assembled from context at runtime.

---

## Runtime Variables

At runtime, the following variables are resolved from project state:

| Variable | Resolved From | Example |
|----------|---------------|---------|
| `{{ACTIVE_FEATURE}}` | claude-product-cycle/CURRENT_WORK.md | `beneficiary` |
| `{{ACTIVE_LAYER}}` | claude-product-cycle/CURRENT_WORK.md | `client` |
| `{{FEATURE_SPEC}}` | claude-product-cycle/design-spec-layer/features/{{ACTIVE_FEATURE}}/SPEC.md | [file contents] |
| `{{FEATURE_API}}` | claude-product-cycle/design-spec-layer/features/{{ACTIVE_FEATURE}}/API.md | [file contents] |
| `{{EXISTING_SERVICES}}` | claude-product-cycle/client-layer/FEATURE_MAP.md | BeneficiaryService, ... |
| `{{EXISTING_REPOS}}` | claude-product-cycle/client-layer/FEATURE_MAP.md | SelfServiceRepository, ... |
| `{{MODULE_EXISTS}}` | claude-product-cycle/feature-layer/MODULES_INDEX.md | true/false |

---

## Dynamic Plan Prompts

### Plan: implement-feature

```
CONTEXT LOAD:
  - Read claude-product-cycle/client-layer/FEATURE_MAP.md → {{EXISTING_SERVICES}}, {{EXISTING_REPOS}}
  - Read claude-product-cycle/feature-layer/MODULES_INDEX.md → {{MODULE_EXISTS}}
  - Read claude-product-cycle/design-spec-layer/features/{{ACTIVE_FEATURE}}/SPEC.md → {{FEATURE_SPEC}}
  - Read claude-product-cycle/design-spec-layer/features/{{ACTIVE_FEATURE}}/API.md → {{FEATURE_API}}

DECISION:
  IF {{EXISTING_SERVICES}} contains {{ACTIVE_FEATURE}}Service:
    SKIP service creation
  ELSE:
    CREATE {{ACTIVE_FEATURE}}Service using PROMPT:new-service
  
  IF {{EXISTING_REPOS}} contains {{ACTIVE_FEATURE}}Repository:
    SKIP repository creation
  ELSE:
    CREATE {{ACTIVE_FEATURE}}RepositoryImpl using PROMPT:new-repository

  IF {{MODULE_EXISTS}} is true:
    UPDATE existing ViewModel/Screen
  ELSE:
    CREATE using PROMPT:new-viewmodel + feature templates

FINALIZE:
  - Update FEATURE_MAP.md
  - Update MODULES_INDEX.md
  - Update SCREENS_INDEX.md
```

### Plan: verify-feature

```
CONTEXT LOAD:
  - Read claude-product-cycle/design-spec-layer/FEATURES_INDEX.md → spec status
  - Read claude-product-cycle/client-layer/FEATURE_MAP.md → expected services/repos
  - Read claude-product-cycle/feature-layer/MODULES_INDEX.md → expected VMs/screens

CHECK:
  FOR EACH expected component:
    - Verify file exists at expected path
    - Verify DI registration
    - Verify navigation registration
    - Check TestTags object exists

REPORT:
  - Generate gap list with P0/P1/P2 severity
  - Calculate verification score
  - Suggest fixes
```

---

## Runtime Execution

Prompts are assembled at the time a command runs:
1. Command reads its O(1) index files
2. Runtime variables are resolved
3. Decision tree selects which sub-prompts to execute
4. Output is generated with real paths and class names

This file documents the intended runtime behavior; actual execution happens via Claude's reasoning on the command files.
