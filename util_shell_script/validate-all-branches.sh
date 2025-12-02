#!/bin/bash

echo "🔍 모든 로컬 브랜치 네이밍 검증 시작..."
echo "=================================================="
echo ""

# 브랜치 네이밍 패턴 정의
PATTERN="^(feat|feature|fix|bug|bugfix|hot|hotfix|docs|ref|refactor|test|chore|rel|release)/([0-9]+|[A-Z]+-[0-9]+)/[a-z0-9-]+$"

# 제외할 브랜치들 (보호된 브랜치)
EXCLUDED_BRANCHES=("main" "master" "dev" "develop" "staging" "production" "release")

# 결과 카운터
VALID_COUNT=0
INVALID_COUNT=0
EXCLUDED_COUNT=0

echo "📋 검증 결과:"
echo ""

# git branch 명령어로 브랜치 목록 가져오기 (현재 브랜치 표시 * 제거)
git branch | sed 's/^[* ] //' | while IFS= read -r branch; do
  # 빈 라인 스킵
  if [ -z "$branch" ]; then
    continue
  fi
  
  echo "🔍 검사 중: '$branch'"
  
  # 제외 브랜치 확인
  is_excluded=false
  for excluded in "${EXCLUDED_BRANCHES[@]}"; do
    if [ "$branch" = "$excluded" ]; then
      echo "⚪ $branch (보호된 브랜치 - 검증 제외)"
      is_excluded=true
      break
    fi
  done
  
  # 제외 브랜치가 아닌 경우 검증
  if [ "$is_excluded" = false ]; then
    if [[ "$branch" =~ $PATTERN ]]; then
      echo "✅ $branch (규칙 준수)"
    else
      echo "❌ $branch (네이밍 규칙 위반)"
    fi
  fi
  
  echo ""
done

echo "=================================================="
echo ""
echo "📋 올바른 네이밍 규칙:"
echo "   패턴: 타입/이슈번호/설명"
echo "   타입: feat, feature, fix, bug, bugfix, hot, hotfix, docs, ref, refactor, test, chore, rel, release"
echo "   이슈: 123 또는 ABC-123"
echo "   설명: 소문자, 숫자, 하이픈만 사용"
echo ""
echo "✨ 올바른 예시:"
echo "   • feat/123/add-user-login"
echo "   • fix/456/resolve-login-error"
echo "   • docs/789/update-readme"
echo "   • test/ABC-123/add-unit-tests"
echo ""
echo "💡 브랜치 리네임 방법:"
echo "   git branch -m old-name new-name"
