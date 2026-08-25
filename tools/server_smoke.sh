#!/bin/sh
# Boot the dedicated server, wait for it to finish loading, shut it down CLEANLY,
# and fail if the mod produced any error.
#
# Why this is a script and not three shell lines: killing the JVM with pkill
# leaves run/world/session.lock behind, and the NEXT boot then dies on the lock
# before it ever loads a mod -- which looks exactly like "no errors found". A
# check that cannot reach the thing it is checking will only ever report success.
# See docs/LESSONS.md #5 and #7.
set -e
cd "$(dirname "$0")/.."
LOG=${LOG:-/tmp/interregnum-server.log}
TIMEOUT=${TIMEOUT:-420}

pkill -x java 2>/dev/null || true
sleep 1
rm -f run/world/session.lock
mkdir -p run
echo "eula=true" > run/eula.txt

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
    echo "stop" > "$FIFO"          # clean shutdown: releases the world lock
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
echo
echo "OK: server booted with the mod loaded, clean shutdown"
