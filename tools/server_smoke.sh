#!/bin/bash
# Boot the dedicated server, wait for it to finish loading, shut it down CLEANLY,
# and fail if the mod produced any error.
#
# Why this is a script and not three shell lines: killing the JVM with pkill
# leaves run/world/session.lock behind, and the NEXT boot then dies on the lock
# before it ever loads a mod -- which looks exactly like "no errors found". A
# check that cannot reach the thing it is checking will only ever report success.
# See docs/LESSONS.md #5, #7 and #10.
#
# bash, not sh: a /bin/sh version of this file split command strings on the wrong
# characters because IFS=$'\n' is not a newline under dash. Same class of bug as
# the one that made check_all.sh print ALL CHECKS PASSED while broken.
set -euo pipefail
cd "$(dirname "$0")/.."
LOG=${LOG:-/tmp/interregnum-server.log}
TIMEOUT=${TIMEOUT:-420}

pkill -x java 2>/dev/null || true
sleep 1
rm -f run/world/session.lock
# The script creates its ENTIRE run environment. run/ is gitignored, so anything
# it does not write itself does not exist on a fresh checkout -- which is how the
# first CI run failed while passing locally: server.properties existed on the dev
# machine because it had been created by hand once. A smoke test that inherits
# state from the machine it was written on is not testing the shipped repository.
# A FRESH world every run. The world directory persists otherwise, and a world
# created by an earlier boot -- possibly before server.properties existed, and so
# not flat at all -- silently invalidates every worldgen check made against it.
# That is exactly how `place feature` came to "fail" at one coordinate and succeed
# at another on supposedly identical flat terrain.
# Set KEEP_WORLD=1 to reuse it when testing persistence across restarts.
mkdir -p run
if [ -z "${KEEP_WORLD:-}" ]; then
    rm -rf run/world
fi
echo "eula=true" > run/eula.txt
cat > run/server.properties <<'PROPS'
# Written by tools/server_smoke.sh. Tuned for a fast, deterministic boot:
# a small view distance loads in a fraction of the time.
max-players=1
online-mode=false
spawn-protection=0
view-distance=4
simulation-distance=4
sync-chunk-writes=false
enable-status=false
# THE WORLD MUST KEEP TICKING. A dedicated server's default is
# `pause-when-empty-seconds=60`: with nobody connected it ticks for a minute and then
# STOPS -- no warning, no lag message, no error. Every check here runs with no players,
# so any check that waits longer than a minute of server time was quietly measuring a
# frozen world from that point on.
#
# It was found by `exodus_check.sh`, the first check to wait more than sixty seconds:
# sand placed after the wait never fell, gametime read 1199 before a four-second pause
# and 1199 after, and the check reported that the mod's law was broken. `turning_check.sh`
# waits forty-three seconds and has been living just under the edge.
#
# Zero disables the pause. Set here rather than per-check so no future check has to know.
pause-when-empty-seconds=0
# RCON is how commands reach the server. Piping to stdin does NOT work under
# Gradle's runServer -- see tools/rcon.py and docs/LESSONS.md #10.
enable-rcon=true
rcon.port=25575
rcon.password=interregnum
broadcast-rcon-to-ops=true
PROPS

# level-type and level-seed go OUTSIDE the heredoc, because it is quoted
# (<<'PROPS') and therefore expands nothing. An earlier version put ${LEVEL_TYPE}
# inside it; the literal text was written to the file, so the shrine rate probe
# ran against a flat world and reported 100% terrain acceptance -- true of a flat
# world, meaningless anywhere else. Default stays flat: level ground makes every
# other check deterministic. LEVEL_TYPE=minecraft:normal gets real terrain.
{
    printf 'level-type=%s\n' "${LEVEL_TYPE:-minecraft\\:flat}"
    printf 'level-seed=%s\n' "${LEVEL_SEED:-interregnum}"
} >> run/server.properties

FIFO=$(mktemp -u)
mkfifo "$FIFO"
rm -f "$LOG"

# Hold the fifo open so the server does not see EOF on stdin and exit early.
sleep "$TIMEOUT" > "$FIFO" &
HOLD=$!

timeout "$TIMEOUT" gradle --no-daemon --console=plain runServer < "$FIFO" > "$LOG" 2>&1 &
GRADLE=$!

booted=0
i=0
while [ $i -lt $((TIMEOUT / 3)) ]; do
    if grep -qE 'Done \([0-9.]+s\)' "$LOG" 2>/dev/null; then booted=1; break; fi
    if grep -qE 'FatalStartupException|Failed to start the minecraft server' "$LOG" 2>/dev/null; then break; fi
    sleep 3
    i=$((i + 1))
done

if [ "$booted" = "1" ]; then
    # Commands go over RCON. This is how worldgen gets verified without a client:
    # `/place feature ...` either succeeds or answers with a reason, and the reply
    # is captured here rather than merely hoped for.
    RCON_OUT=""
    if [ -n "${COMMANDS:-}" ]; then
        CMDFILE=$(mktemp)
        printf '%s\n' "$COMMANDS" > "$CMDFILE"
        if ! RCON_OUT=$(python3 tools/rcon.py --port 25575 --password interregnum \
                        --file "$CMDFILE" 2>&1); then
            rm -f "$CMDFILE"
            echo "FAIL: could not run commands over RCON:"
            echo "$RCON_OUT"
            kill $HOLD $GRADLE 2>/dev/null || true
            pkill -x java 2>/dev/null || true
            exit 1
        fi
        rm -f "$CMDFILE"
        echo "--- commands ---"
        echo "$RCON_OUT"
    fi
    # A real /stop, over RCON. (This used to be `echo stop > fifo`, which never
    # reached the server; the process died on SIGTERM instead and its shutdown hook
    # saved chunks, which made the log look like a clean stop.)
    python3 tools/rcon.py --port 25575 --password interregnum stop >/dev/null 2>&1 || true
    j=0
    while [ $j -lt 20 ]; do
        grep -qE 'Stopping server|ThreadedAnvilChunkStorage: All dimensions are saved' "$LOG" && break
        sleep 2
        j=$((j + 1))
    done
fi

sleep 2
kill $HOLD 2>/dev/null || true
kill $GRADLE 2>/dev/null || true
pkill -x java 2>/dev/null || true
sleep 1
pkill -9 -x java 2>/dev/null || true   # the runner reported an orphan java otherwise
rm -f "$FIFO"
rm -f run/world/session.lock

if [ "$booted" != "1" ]; then
    echo "FAIL: server did not finish loading. Last lines:"
    tail -25 "$LOG" | grep -v 'Picked up'
    exit 1
fi

echo "--- mod load ---"
grep -E 'Loading [0-9]+ mods|INTERREGNUM' "$LOG" | head -3
grep -E 'Done \([0-9.]+s\)' "$LOG" | head -1
echo

# Attribution is block-based, not line-based -- see tools/server_log_check.py for
# why a grep gets this wrong and why widening its ignore list is the wrong fix.
python3 tools/server_log_check.py "$LOG"

# Booting is not loading. A datapack path off by one directory, or content that
# fails to parse, leaves a server that starts perfectly and contains nothing --
# the "green tests are not a working feature" failure from docs/VERIFICATION.md.
# So assert the content is actually there.
echo
DLG=$(grep -oE 'Loaded [0-9]+ dialogue graph\(s\)' "$LOG" | grep -oE '[0-9]+' | head -1)
if [ -z "$DLG" ]; then
    echo "FAIL: the dialogue loader never reported. Was it registered?"
    exit 1
fi
if [ "$DLG" -lt "${EXPECT_DIALOGUE:-1}" ]; then
    echo "FAIL: loaded $DLG dialogue graph(s), expected at least ${EXPECT_DIALOGUE:-1}"
    exit 1
fi
if grep -q 'rejected)' "$LOG"; then
    echo "FAIL: some dialogue was rejected at load:"
    grep -iE 'is invalid and was not loaded' "$LOG" | head -5
    exit 1
fi
echo "content: $DLG dialogue graph(s) loaded, none rejected"
echo
echo "OK: server booted, mod loaded, content present, clean shutdown"
