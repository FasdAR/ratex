#!/usr/bin/env bash
# Тест classify-tag.sh на временном git-репозитории: m1 (общий предок), d1 (только develop), m2 (только master).
set -uo pipefail

script="$(cd "$(dirname "$0")" && pwd)/classify-tag.sh"
work="$(mktemp -d "${TMPDIR:-/tmp}/classify-tag.XXXXXX")" || { echo "mktemp failed" >&2; exit 1; }
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1

git init -q -b master .
git config user.email test@example.com
git config user.name test
git commit -q --allow-empty -m m1
m1=$(git rev-parse HEAD)
git switch -q -c develop
git commit -q --allow-empty -m d1
d1=$(git rev-parse HEAD)
git switch -q master
git commit -q --allow-empty -m m2
m2=$(git rev-parse HEAD)

export DEVELOP_REF=develop MASTER_REF=master
failures=0

check() {
  local name=$1 expected_code=$2 expected_out=$3 tag=$4 sha=$5 out code
  out=$("$script" "$tag" "$sha" 2>/dev/null)
  code=$?
  if [[ $code -ne $expected_code || $out != "$expected_out" ]]; then
    echo "FAIL: $name (code=$code out='$out', expected code=$expected_code out='$expected_out')"
    failures=$((failures + 1))
  else
    echo "ok: $name"
  fi
}

check "alpha on develop commit" 0 alpha 1.2.0-alpha "$d1"
check "release on master commit" 0 release 1.2.0 "$m2"
check "alpha on common ancestor" 0 alpha 1.2.0-alpha "$m1"
check "release on common ancestor" 0 release 1.2.0 "$m1"
check "alpha on master-only commit" 1 "" 1.2.0-alpha "$m2"
check "release on develop-only commit" 1 "" 1.2.0 "$d1"
check "v prefix is not a release tag" 2 "" v1.2.0 "$m2"
check "alpha with counter is not a release tag" 2 "" 1.2.0-alpha.1 "$d1"
check "two-part version is not a release tag" 2 "" 1.2 "$m2"
check "beta is not a release tag" 2 "" 1.2.0-beta "$d1"
check "shell metacharacters are rejected" 2 "" '1.2.0-alpha;touch pwned' "$d1"

DEVELOP_REF=missing-branch check "missing ref fails" 1 "" 1.2.0-alpha "$d1"

if [[ $failures -ne 0 ]]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "all checks passed"
