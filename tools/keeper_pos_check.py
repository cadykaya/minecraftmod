"""Is the shrine-keeper standing on the shrine, facing the offering box?

Split out of tools/worldgen_check.sh rather than inlined, for a boring reason worth
recording: the check is a bash script that already uses a heredoc to pass commands to
the server, and a Python heredoc inside it closed the outer one early and silently
corrupted the file. Nested heredocs sharing a terminator is a trap with no error
message worth reading.

Reads the raw `/data get` replies on argv so the parsing lives with the assertions.

    python3 tools/keeper_pos_check.py "<Pos reply>" "<Rotation reply>" <boxX> <boxZ> <floorY>
"""
import math
import re
import sys


def main():
    if len(sys.argv) != 6:
        print("  usage: keeper_pos_check.py <pos> <rot> <boxX> <boxZ> <floorY>")
        return 1
    pos_reply, rot_reply = sys.argv[1], sys.argv[2]
    box_x, box_z, floor_y = (float(v) for v in sys.argv[3:6])

    pos = re.findall(r"(-?[0-9.]+)d", pos_reply)
    if len(pos) < 3:
        print(f"  no keeper position in reply: {pos_reply!r}")
        return 1
    x, y, z = (float(v) for v in pos[:3])

    # Standing ON the paving. Asserted as a position rather than as mere existence:
    # an entity that spawned at the wrong Y is still an entity, so "there is a
    # keeper" would pass happily while the keeper was buried in the floor.
    if abs(y - floor_y) > 0.01:
        print(f"  y={y}: not standing on the paving (expected {floor_y})")
        return 1
    if max(abs(x - box_x), abs(z - box_z)) > 2.5:
        print(f"  ({x}, {z}): not beside the offering box at ({box_x}, {box_z})")
        return 1

    # ...and FACING it. A keeper with their back to the thing they exist for is an
    # error that only ever surfaces in a screenshot, so it gets an assertion. The
    # first version had the yaw negated and pointed them away from the box.
    rot = re.findall(r"(-?[0-9.]+)f", rot_reply)
    if not rot:
        print(f"  no keeper rotation in reply: {rot_reply!r}")
        return 1
    yaw = float(rot[0])
    # Minecraft yaw: 0 looks along +z, 90 along -x.
    look = (-math.sin(math.radians(yaw)), math.cos(math.radians(yaw)))
    to_box = (box_x - x, box_z - z)
    dist = math.hypot(*to_box) or 1.0
    dot = (look[0] * to_box[0] + look[1] * to_box[1]) / dist
    if dot < 0.9:
        print(f"  yaw {yaw} looks along {look}; the box is at offset {to_box} -- not facing it")
        return 1

    print(f"  keeper attends the box from ({x}, {z}), looking at it (yaw {yaw})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
