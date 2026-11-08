#!/usr/bin/env bash
#
# Headless server smoke check.
#
# Boots a dedicated dev server with CCTweaks on the classpath, lets it stop itself after loading
# (`-PsmokeTest` makes the run task write "stop" to the server's stdin a few seconds after FML
# reports "Done"), and then asserts on the server log:
#
#   1. CCTweaks is in the mod list (the mod actually loaded),
#   2. no CCTweaks ERROR/exception lines,
#   3. no unresolved 2.x mappings ("Found a missing id from the world cctweaks:..."),
#   4. no mod-sorting/discovery failure,
#   5. the server shut down cleanly ("Stopping server"),
#   6. the in-world self test passed (see org.squiddev.cctweaks.selfTest).
#
# The self test is the part that covers behaviour which needs a real world: digging with a tool
# host (including drops landing in the turtle), block protection, attacking an entity, and the
# computer/turtle upgrade items. It runs a few blocks above the world spawn and restores the area.
# It cannot cover the client-only config GUI or NEI rendering; those still need a dev client.
#
# Usage:  bash scripts/headless-smoke.sh
# Env:    SMOKE_TIMEOUT  seconds to wait for the server to reach "Done" (default 900)
#         SMOKE_TASK     Gradle run task (default runServer25)
#         SMOKE_STOP_AFTER  seconds after which the server is told to stop, if "Done" was not
#                           seen first (default 300)
#         SMOKE_SELFTEST  1 (default) to run the in-world self test, 0 to skip it
#
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT" || exit 1

TASK=${SMOKE_TASK:-runServer25}
TIMEOUT=${SMOKE_TIMEOUT:-900}
SELFTEST=${SMOKE_SELFTEST:-1}
BOOT_LOG="$ROOT/run/server/logs/latest.log"
# FML redirects mod/Forge logging into its own file; the vanilla log only carries server lines.
LOG="$ROOT/run/server/logs/fml-server-latest.log"
GRADLE_LOG="$ROOT/build/smoke-gradle.log"

mkdir -p "$ROOT/build"
rm -f "$BOOT_LOG" "$LOG" "$GRADLE_LOG"

# A fresh checkout (CI) has no run/server at all. The dev server refuses to start without an
# accepted EULA; this is a local-only GradleStart server with no Mojang services attached, so
# agree on its behalf rather than failing the smoke run on a missing file.
mkdir -p "$ROOT/run/server"
if [ ! -f "$ROOT/run/server/eula.txt" ]; then
    echo "eula=true" >"$ROOT/run/server/eula.txt"
fi

SELFTEST_FLAG=""
if [ "$SELFTEST" != "0" ]; then
    SELFTEST_FLAG="-PselfTest"
fi

echo "==> starting ./gradlew $TASK -PsmokeTest $SELFTEST_FLAG (gradle log: $GRADLE_LOG)"
./gradlew "$TASK" -PsmokeTest $SELFTEST_FLAG -PsmokeStopAfter="${SMOKE_STOP_AFTER:-300}" --console=plain >"$GRADLE_LOG" 2>&1 &
GRADLE_PID=$!
GRADLE_EXIT=1

# Best-effort fallback if the self-stop fails: kill only dev JVMs that appeared after we started,
# so a dev client the developer already had open is never touched.
dev_server_pids() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*)
            powershell -NoProfile -Command \
                "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'GradleStart' } | ForEach-Object { \$_.ProcessId }" \
                2>/dev/null | tr -d '\r' | sort -u ;;
        *) pgrep -f 'GradleStart' 2>/dev/null | sort -u ;;
    esac
}
BEFORE_PIDS=$(dev_server_pids)

stop_server() {
    if kill -0 "$GRADLE_PID" 2>/dev/null; then
        local after
        after=$(dev_server_pids)
        for pid in $after; do
            if ! echo "$BEFORE_PIDS" | grep -qx "$pid"; then
                case "$(uname -s)" in
                    MINGW*|MSYS*|CYGWIN*) taskkill //PID "$pid" //F >/dev/null 2>&1 ;;
                    *) kill -TERM "$pid" 2>/dev/null || true ;;
                esac
            fi
        done
        kill -TERM "$GRADLE_PID" 2>/dev/null || true
    fi
    wait "$GRADLE_PID" 2>/dev/null
    GRADLE_EXIT=$?
}
trap stop_server EXIT

echo "==> waiting up to ${TIMEOUT}s for the server to finish loading"
started=0
for _ in $(seq 1 "$TIMEOUT"); do
    if [ -f "$BOOT_LOG" ] && grep -q 'Done (' "$BOOT_LOG" 2>/dev/null; then
        started=1
        break
    fi
    if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
        echo "!! the server process exited before it finished loading" >&2
        tail -n 60 "$GRADLE_LOG" >&2
        exit 1
    fi
    sleep 1
done

if [ "$started" -ne 1 ]; then
    echo "!! timed out after ${TIMEOUT}s waiting for 'Done (' in the server log" >&2
    { tail -n 40 "$BOOT_LOG" 2>/dev/null || tail -n 40 "$GRADLE_LOG"; } >&2
    exit 1
fi

# Gradle hands the server a "stop" command a few seconds after it sees "Done" in the log (or, if
# reading the log fails while log4j holds it open, after SMOKE_STOP_AFTER seconds). This wait has to
# outlast that fallback, otherwise the script kills a healthy server and reports it as a crash.
STOP_WAIT=$(( ${SMOKE_STOP_AFTER:-300} + 150 ))

echo "==> server reached 'Done', waiting up to ${STOP_WAIT}s for it to stop"
for _ in $(seq 1 "$STOP_WAIT"); do
    if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
        break
    fi
    sleep 1
done
# Reaps the run task and records its exit code. Still alive at this point means the self-stop did
# not work, so this also performs the fallback kill.
stop_server

echo "==> checking the log"
failures=0
fail() {
    echo "!! $1" >&2
    failures=$((failures + 1))
}

if grep -qi 'cctweaks' "$LOG"; then
    echo "   ok: CCTweaks appears in the log (mod list loaded)"
else
    fail "CCTweaks never appears in the server log: the mod did not load"
fi

if grep -i 'ERROR.*cctweaks' "$LOG" | grep -v 'Discarding removed CCTweaks mapping' >/dev/null; then
    fail "CCTweaks logged an ERROR:"
    grep -i 'ERROR.*cctweaks' "$LOG" | grep -v 'Discarding removed CCTweaks mapping' >&2
else
    echo "   ok: no CCTweaks ERROR lines"
fi

if grep -i 'cctweaks' "$LOG" | grep -iE 'exception|IllegalAccessError|NoSuchMethod|NoClassDefFound' >/dev/null; then
    fail "CCTweaks logged an exception:"
    grep -i 'cctweaks' "$LOG" | grep -iE 'exception|IllegalAccessError|NoSuchMethod|NoClassDefFound' >&2
else
    echo "   ok: no CCTweaks exceptions"
fi

if grep -q 'Found a missing id from the world cctweaks:' "$LOG"; then
    fail "a removed 2.x block/item is still present in the dev world and was not remapped"
else
    echo "   ok: no unresolved 2.x mappings"
fi

if grep -iE 'ModSortingException|MissingModsException|Failed to load mod' "$LOG" >/dev/null; then
    fail "mod discovery/sorting failed:"
    grep -iE 'ModSortingException|MissingModsException|Failed to load mod' "$LOG" >&2
else
    echo "   ok: no mod discovery/sorting failures"
fi

# The vanilla lifecycle lines ("Done", "Stopping server") only go to latest.log.
if grep -q 'Stopping server' "$BOOT_LOG"; then
    echo "   ok: the server shut down cleanly"
else
    fail "the server never logged 'Stopping server' (it was killed or crashed)"
fi

if [ "$SELFTEST" != "0" ]; then
    # Both logs are checked: mod logging goes to the FML log, but a hard failure can land in the
    # vanilla one.
    SELFTEST_LOG=$(mktemp)
    cat "$LOG" "$BOOT_LOG" 2>/dev/null | grep 'CCTWEAKS-SELFTEST' >"$SELFTEST_LOG" || true

    if grep -q 'CCTWEAKS-SELFTEST RESULT' "$SELFTEST_LOG"; then
        echo "   ok: $(grep -c 'CCTWEAKS-SELFTEST PASS' "$SELFTEST_LOG") self-test check(s) passed"
    else
        fail "the in-world self test did not report a result (see $LOG)"
    fi

    if grep -q 'CCTWEAKS-SELFTEST FAIL' "$SELFTEST_LOG"; then
        fail "the in-world self test reported failures:"
        grep 'CCTWEAKS-SELFTEST FAIL' "$SELFTEST_LOG" >&2
    else
        echo "   ok: no self-test failures"
    fi

    grep 'CCTWEAKS-SELFTEST RESULT' "$SELFTEST_LOG" >&2 || true
    rm -f "$SELFTEST_LOG"
fi

if [ "$GRADLE_EXIT" -ne 0 ]; then
    fail "the Gradle run task failed (exit $GRADLE_EXIT); see $GRADLE_LOG"
fi

if [ "$failures" -ne 0 ]; then
    echo "==> SMOKE FAILED ($failures problem(s)); server log: $LOG" >&2
    exit 1
fi

echo "==> SMOKE PASSED"
exit 0
