#!/usr/bin/env bash
set -euxo pipefail
SEED_OUT="${1:-/tmp/pdfbox-seeds}"
mkdir -p "$SEED_OUT"
# If mutool (mupdf) is available in Dockerfile, use it to normalize & split.
find /usr/share -type f -iname '*.pdf' 2>/dev/null | head -n 100 | while read -r f; do
  cp "$f" "$SEED_OUT"/
done
# Fallback: copy any example PDFs from the build tree if present
[ -d /src/pdfbox/examples ] && cp -r /src/pdfbox/examples/*.pdf "$SEED_OUT"/ || true
zip -j "$SEED_OUT"/seed-min.zip "$SEED_OUT"/*.pdf || true
echo "Seeds at $SEED_OUT"
