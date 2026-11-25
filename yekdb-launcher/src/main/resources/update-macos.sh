#!/bin/bash
set -eu
pid="$1"
target="$2"
stage="$3"
backup="$stage/previous.app"
candidate="$stage/YEKDB.app"
[[ "$pid" =~ ^[0-9]+$ ]] || exit 1
[[ "$target" == /*/YEKDB.app && "$stage" == "${target%/*}"/.yekdb-update-* ]] || exit 1
[[ -d "$candidate" && -d "$target" && ! -e "$backup" ]] || exit 1
for ((i=0; i<60; i++)); do
    if ! kill -0 "$pid" 2>/dev/null; then break; fi
    sleep 1
done
if kill -0 "$pid" 2>/dev/null; then echo 'Launcher did not exit; installation cancelled.'; exit 1; fi
/bin/mv "$target" "$backup"
rollback() {
    if [[ -e "$target" ]]; then /bin/mv "$target" "$stage/failed.app"; fi
    /bin/mv "$backup" "$target"
    echo 'Update failed; previous application restored.'
}
if ! /bin/mv "$candidate" "$target"; then rollback; exit 1; fi
if ! /usr/bin/open "$target"; then rollback; /usr/bin/open "$target" || true; exit 1; fi
echo 'Update installed. Previous application retained in staging directory.'
