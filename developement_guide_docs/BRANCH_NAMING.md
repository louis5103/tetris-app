# Branch Naming Validation

This repository includes GitHub Actions workflow for automatic branch naming validation to ensure team consistency and proper branch organization.

## 🎯 Branch Naming Convention

### Pattern
```
타입/이슈번호/설명
```

**Regex Pattern:**
```regex
^(feat|feature|fix|bug|bugfix|hot|hotfix|docs|ref|refactor|test|chore|rel|release)\/([0-9]+|[A-Z]+-[0-9]+)\/[a-z0-9-]+$
```

### Allowed Branch Types

| Full Name | Abbreviation | Description |
|-----------|--------------|-------------|
| `feature` | `feat` | New features |
| `bugfix` | `fix`, `bug` | Bug fixes |
| `hotfix` | `hot` | Urgent fixes |
| `release` | `rel` | Release preparation |
| `docs` | `docs` | Documentation |
| `refactor` | `ref` | Code refactoring |
| `test` | `test` | Testing |
| `chore` | `chore` | Other tasks |

### Issue Number Formats
- **Simple number:** `123`, `456`
- **Jira-style:** `ABC-123`, `DEF-456`

### Description Rules
- Only lowercase letters, numbers, and hyphens
- No spaces or special characters
- Use hyphens to separate words

## ✅ Valid Examples

```bash
# Features
feature/12/shopping-cart
feature/23/payment-integration
feat/35/user-profile-page
feat/41/email-notification

# Bug fixes
bugfix/8/login-validation-error
bug/15/cart-calculation-bug
fix/27/responsive-layout-fix

# Hotfixes
hotfix/99/security-patch
hotfix/101/memory-leak-fix
hot/103/database-connection-timeout

# Other types
docs/5/api-documentation
refactor/18/database-optimization
test/22/unit-test-coverage
chore/31/build-script-update
release/45/v2-1-0
rel/ABC-123/user-authentication
```

## ❌ Invalid Examples

```bash
# Wrong separators
feature-12-shopping-cart

# Uppercase in type
Feature/12/shopping-cart

# Uppercase in description
feat/12/Shopping-Cart

# Underscore not allowed
feat/12/shopping_cart

# Invalid type
new-feature/12/test

# Invalid issue number
feat/abc/test

# Special characters
feat/12/test@special

# Spaces not allowed
feat/12/test with spaces
```

## 🛡️ Protected Branches

The following branches are **excluded** from naming validation:
- `main`
- `master`
- `dev`
- `develop`
- `staging`
- `production`
- `release`

## 🔄 Workflow Triggers

The validation workflow runs on:
- **Push events** (excluding protected branches)
- **Pull requests** to protected branches

## 📝 How It Works

1. **Automatic Detection:** The workflow automatically detects the branch name from GitHub events
2. **Protection Check:** First checks if the branch is in the protected list and skips validation if so
3. **Pattern Validation:** Validates the branch name against the regex pattern
4. **Detailed Feedback:** Provides comprehensive error messages with examples when validation fails
5. **Success Confirmation:** Shows confirmation and pattern details when validation passes

## 🚀 Getting Started

This validation is automatically enabled when you:
1. Create a new branch following the naming convention
2. Push to any branch (except protected branches)
3. Create a pull request to a protected branch

The workflow will automatically validate your branch name and provide feedback in the GitHub Actions tab.