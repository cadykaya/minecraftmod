"""Minimal Minecraft RCON client. Runs commands in a live server and prints replies.

Why this exists instead of piping to the server's stdin: stdin does not reach the
game under Gradle's runServer. The smoke test believed it did for several commits
-- `stop` appeared to work because killing the process triggers the JVM shutdown
hook, which saves chunks and therefore *looks* like a clean stop. It was never a
clean stop, and no command ever ran. See docs/LESSONS.md #10.

    python3 tools/rcon.py --port 25575 --password pw "say hello" "list"
"""
import argparse
import socket
import struct
import sys
import time

LOGIN, COMMAND, RESPONSE = 3, 2, 0


class RconError(Exception):
    pass


class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, password="", timeout=10.0):
        self.addr, self.password, self.timeout = (host, port), password, timeout
        self.sock = None
        self._id = 0

    def __enter__(self):
        self.sock = socket.create_connection(self.addr, timeout=self.timeout)
        self.sock.settimeout(self.timeout)
        rid = self._send(LOGIN, self.password)
        got, _ = self._recv()
        # A failed login answers with request id -1. Checking the ID rather than the
        # body matters: the body is empty on success AND on failure.
        if got != rid:
            raise RconError("RCON authentication failed")
        return self

    def __exit__(self, *exc):
        if self.sock:
            self.sock.close()

    def _send(self, kind, body):
        self._id += 1
        payload = struct.pack("<ii", self._id, kind) + body.encode("utf8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        return self._id

    def _read_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise RconError("connection closed by server")
            buf += chunk
        return buf

    def _recv(self):
        (length,) = struct.unpack("<i", self._read_exact(4))
        data = self._read_exact(length)
        rid, _kind = struct.unpack("<ii", data[:8])
        return rid, data[8:-2].decode("utf8", errors="replace")

    def run(self, cmd):
        self._send(COMMAND, cmd)
        _, body = self._recv()
        return body


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", default="interregnum")
    ap.add_argument("--timeout", type=float, default=10.0)
    ap.add_argument("--file", help="read commands from this file, one per line")
    ap.add_argument("commands", nargs="*")
    a = ap.parse_args()
    cmds = list(a.commands)
    if a.file:
        # A file, not argv, because passing multi-line commands through a shell
        # invites IFS bugs: under /bin/sh, IFS=$'\n' is not a newline but the
        # literal characters, and "minecraft" duly split into "mi" and "ecraft".
        with open(a.file) as fh:
            cmds += [l.strip() for l in fh if l.strip()]
    if not cmds:
        print("no commands given", file=sys.stderr)
        return 2
    try:
        with Rcon(a.host, a.port, a.password, a.timeout) as r:
            for cmd in cmds:
                # `wait <seconds>` is not a Minecraft command. Commands go over one
                # connection back to back, so a whole batch can run inside a fraction
                # of a second of server time -- which is fine for asking questions and
                # wrong for anything that has to finish first. Loading a chunk is
                # asynchronous: `forceload add` returns immediately and the chunk
                # arrives later, so a batch that forceloads and then immediately
                # generates into the result is racing the server. See docs/LESSONS.md.
                if cmd.startswith("wait "):
                    print(f"$ {cmd}")
                    time.sleep(float(cmd.split(None, 1)[1]))
                    continue
                reply = r.run(cmd).strip()
                print(f"$ {cmd}")
                if reply:
                    print(f"  {reply}")
    except (OSError, RconError) as e:
        print(f"RCON failed: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
