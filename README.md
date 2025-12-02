# se-teris-team-9
Software Engineering Tetris Game Project

## 🌿 브랜치 네이밍 규칙

우리 프로젝트는 일관된 브랜치 네이밍을 위해 자동 검증 시스템을 사용합니다.

### 📋 네이밍 규칙

**패턴:** `타입/이슈번호/설명`

- **타입:** `feat`, `fix`, `docs`, `test`, `chore`, `refactor`, `hotfix`
- **이슈번호:** `123` 또는 `ABC-123` (Jira 스타일)
- **설명:** 소문자, 숫자, 하이픈만 사용

### ✨ 올바른 예시

```
feat/123/add-user-login
fix/456/resolve-login-error
docs/789/update-readme
test/ABC-123/add-unit-tests
hotfix/999/security-patch
```

### 🔧 브랜치 관리 도구

#### 1. 모든 브랜치 검증
```bash
./validate-all-branches.sh
```

#### 2. 새 브랜치 생성 (자동 검증)
```bash
./create-branch.sh feat/123/your-feature-name
```

#### 3. 브랜치 이름 변경
```bash
git branch -m old-name new-name
```

### 🚫 잘못된 예시

```
feature-123-add-user          # 잘못된 구분자
Feat/123/Add-User            # 대문자 사용
feat/123/add_user            # 언더스코어 사용
feat/123/add.user            # 점 사용
feat/abc/add-user            # 잘못된 이슈번호
new-feature/123/user         # 허용되지 않는 타입
```

### 🤖 자동 검증

- **GitHub Actions:** Push/PR 시 자동으로 브랜치명 검증
- **Git Hook:** Push 전 로컬에서 검증
- **스크립트:** 브랜치 생성 시 즉시 검증
