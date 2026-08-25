"""Is the shrine-keeper tethered to the shrine it was placed at?

Split out of tools/worldgen_check.sh rather than inlined, for a boring reason worth
recording: the check is a bash script that already uses a heredoc to pass commands to
the server, and a Python heredoc inside it closed the outer one early and silently
corrupted the file. Nested heredocs sharing a terminator is a trap with no error
message worth reading.

WHAT THIS DELIBERATELY DOES NOT ASSERT: the keeper's facing. A mob's yaw is set once
at placement and then overwritten by whatever it looks at next, so by the time any
command can observe it the evidence is gone -- an assertion about a live mob's yaw is
an assertion about *when you looked*. The arithmetic is tested instead, in
core/spatial/Facing, where the answer does not move.

The same is true of the exact position, which is why the tolerance here is the mob's
TETHER radius rather than a tight box: a keeper is allowed to shift about their post,
and CI proved they do. What is invariant is that they cannot leave it.

    python3 tools/keeper_pos_check.py "<Pos>" "<home_pos>" "<home_radius>" <cx> <cz> <floorY> <tether>
"""
import re
import sys


def main():
    if len(sys.argv) != 8:
        print("  usage: keeper_pos_check.py <pos> <home_pos> <home_radius> "
              "<cx> <cz> <floorY> <tether>")
        return 1
    pos_reply, home_reply, radius_reply = sys.argv[1], sys.argv[2], sys.argv[3]
    cx, cz, floor_y, tether = (float(v) for v in sys.argv[4:8])

    pos = re.findall(r"(-?[0-9.]+)d", pos_reply)
    if len(pos) < 3:
        print(f"  no keeper position in reply: {pos_reply!r}")
        return 1
    x, y, z = (float(v) for v in pos[:3])

    # On the ground. An entity that spawned at the wrong Y is still an entity, so
    # "there is a keeper" would pass happily while the keeper was inside the floor.
    if abs(y - floor_y) > 1.01:
        print(f"  y={y}: not standing on the shrine (expected about {floor_y})")
        return 1

    # Within the tether. This is the invariant the mob's home radius guarantees --
    # not a hope about how far it happened to have walked when we looked.
    if max(abs(x - cx), abs(z - cz)) > tether + 1.5:
        print(f"  ({x}, {z}): outside the tether around ({cx}, {cz})")
        return 1

    # And the tether is actually SET. Without this the position check above is just
    # "it has not wandered off yet", which is what passed locally and failed in CI.
    home = re.findall(r"(-?\d+)", home_reply)
    if len(home) < 3:
        print(f"  the keeper has no home position: {home_reply!r}")
        return 1
    hx, hy, hz = (int(v) for v in home[:3])
    if hx != int(cx) or hz != int(cz):
        print(f"  home is ({hx}, {hy}, {hz}), not the shrine centre ({int(cx)}, {int(cz)})")
        return 1

    radius = re.findall(r"(-?\d+)", radius_reply)
    if not radius or int(radius[0]) != int(tether):
        print(f"  home radius is {radius_reply!r}, expected {int(tether)}")
        return 1

    print(f"  keeper is at ({x}, {z}), tethered to ({hx}, {hz}) within {int(tether)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
