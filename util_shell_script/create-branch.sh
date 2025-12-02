#!/bin/bash

# 브랜치 생성 시 네이밍 규칙을 자동으로 검증하는 함수
# 사용법: ./create-branch.sh new-branch-name

if [ $# -eq 0 ]; then
  echo "❌ 브랜치명을 입력하세요."
  echo "사용법: ./create-branch.sh branch-name"
  echo ""
  echo "📋 올바른 네이밍 규칙:"
  echo "   패턴: 타입/이슈번호/설명"
  echo "   예시: feat/123/add-user-login"
  exit 1
fi

BRANCH_NAME="$1"

# 브랜치 네이밍 패턴
PATTERN="^(feat|feature|fix|bug|bugfix|hot|hotfix|docs|ref|refactor|test|chore|rel|release)/([0-9]+|[A-Z]+-[0-9]+)/[a-z0-9-]+$"

# 제외할 브랜치들
EXCLUDED_BRANCHES=("main" "master" "dev" "develop" "staging" "production" "release")

echo "🔍 브랜치명 검증 중: '$BRANCH_NAME'"

# 제외 브랜치 확인
for excluded in "${EXCLUDED_BRANCHES[@]}"; do
  if [ "$BRANCH_NAME" = "$excluded" ]; then
    echo "⚠️  '$BRANCH_NAME'는 보호된 브랜치명입니다. 다른 이름을 사용하세요."
    exit 1
  fi
done

# 브랜치명 검증
if [[ "$BRANCH_NAME" =~ $PATTERN ]]; then
  echo "✅ 브랜치명이 규칙을 준수합니다!"
  
  # 브랜치가 이미 존재하는지 확인
  if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1; then
    echo "⚠️  브랜치 '$BRANCH_NAME'가 이미 존재합니다."
    read -p "체크아웃하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      git checkout "$BRANCH_NAME"
      echo "✅ '$BRANCH_NAME' 브랜치로 전환되었습니다."
    fi
  else
    # 새 브랜치 생성 및 체크아웃
    git checkout -b "$BRANCH_NAME"
    echo "🎉 새 브랜치 '$BRANCH_NAME'가 생성되고 전환되었습니다!"
  fi
else
  echo ""
  echo "❌ 브랜치명 '$BRANCH_NAME'가 네이밍 규칙을 위반합니다!"
  echo ""
  echo "📋 올바른 네이밍 규칙:"
  echo "   패턴: 타입/이슈번호/설명"
  echo "   타입: feat, feature, fix, bug, bugfix, hot, hotfix, docs, ref, refactor, test, chore, rel, release"
  echo "   이슈: 123 또는 ABC-123"
  echo "   설명: 소문자, 숫자, 하이픈만 사용"
  echo ""
  echo "🚫 현재 입력한 브랜치명의 문제점:"
  
  # 구체적인 문제점 분석
  if [[ ! "$BRANCH_NAME" =~ ^(feat|feature|fix|bug|bugfix|hot|hotfix|docs|ref|refactor|test|chore|rel|release)/ ]]; then
    echo "   • 타입이 올바르지 않습니다. (feat, fix, docs 등 사용)"
  fi
  
  if [[ ! "$BRANCH_NAME" =~ /([0-9]+|[A-Z]+-[0-9]+)/ ]]; then
    echo "   • 이슈번호가 올바르지 않습니다. (123 또는 ABC-123 형태)"
  fi
  
  if [[ "$BRANCH_NAME" =~ [A-Z] ]]; then
    echo "   • 대문자가 포함되어 있습니다. (소문자만 사용)"
  fi
  
  if [[ "$BRANCH_NAME" =~ [_\.] ]]; then
    echo "   • 언더스코어(_)나 점(.)이 포함되어 있습니다. (하이픈만 사용)"
  fi
  
  if [[ "$BRANCH_NAME" =~ [[:space:]] ]]; then
    echo "   • 공백이 포함되어 있습니다."
  fi
  
  echo ""
  echo "✨ 올바른 예시:"
  echo "   • feat/123/add-user-login"
  echo "   • fix/456/resolve-login-error"
  echo "   • docs/789/update-readme"
  echo "   • test/ABC-123/add-unit-tests"
  echo ""
  
  exit 1
fi
