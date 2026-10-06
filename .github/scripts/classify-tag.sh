#!/usr/bin/env bash
# Определяет тип релиза по тегу и проверяет, что коммит лежит в нужной ветке.
# Использование: classify-tag.sh <tag> <commit-sha>
# stdout: "alpha" (X.Y.Z-alpha, коммит в develop) или "release" (X.Y.Z, коммит в master).
# Коды выхода: 0 - ок; 1 - коммит не в нужной ветке; 2 - тег не релизный (пропустить).
set -euo pipefail

tag="${1:?tag is required}"
sha="${2:?commit sha is required}"
develop_ref="${DEVELOP_REF:-origin/develop}"
master_ref="${MASTER_REF:-origin/master}"

if [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+-alpha$ ]]; then
  kind=alpha
  ref="$develop_ref"
elif [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  kind=release
  ref="$master_ref"
else
  echo "Tag '$tag' is not a release tag, skipping" >&2
  exit 2
fi

if ! git rev-parse --verify --quiet "$ref" >/dev/null; then
  echo "Reference '$ref' not found; fetch full history (fetch-depth: 0)" >&2
  exit 1
fi

if ! git merge-base --is-ancestor "$sha" "$ref"; then
  echo "Tag '$tag' ($kind) must point to a commit on $ref, but $sha is not reachable from it" >&2
  exit 1
fi

echo "$kind"
