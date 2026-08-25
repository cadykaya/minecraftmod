"""Verify the core self-test by breaking the engine on purpose.

VERIFICATION.md: "a check that has never failed is unverified." This runs each
mutation below against a scratch copy of core/, compiles it, and requires the
self-test to FAIL. A mutation that survives means the suite has a hole -- the
output names it and this script exits non-zero.

    python3 tools/mutate_check.py

Add a mutation whenever you add a guard. That is the whole discipline.
"""
import os, shutil, subprocess, sys, tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = os.path.join(REPO, "core")
MAIN = "core/src/main/java/com/cadykaya/interregnum/core"

# (description, file, find, replace)
MUTATIONS = [
    ("dialogue: INITIATOR ignores the initiator",
     f"{MAIN}/dialogue/Conversation.java",
     "case INITIATOR -> picks.getOrDefault(initiator, picks.values().iterator().next());",
     "case INITIATOR -> picks.values().iterator().next();"),
    ("dialogue: UNANIMOUS silently falls back to majority",
     f"{MAIN}/dialogue/Conversation.java",
     "yield distinct.size() == 1 ? distinct.iterator().next() : null;",
     "yield picks.values().iterator().next();"),
    ("dialogue: graph validator waves through failures",
     f"{MAIN}/dialogue/DialogueGraph.java",
     "if (!failures.isEmpty())", "if (false && !failures.isEmpty())"),
    ("dialogue: VOTE tie does not favour the initiator",
     f"{MAIN}/dialogue/Conversation.java",
     "yield (top.size() > 1 && initiatorPick != null && top.contains(initiatorPick))\n                        ? initiatorPick : top.get(0);",
     "yield top.get(0);"),
    ("dialogue: stances lose the order people spoke in",
     f"{MAIN}/dialogue/Conversation.java",
     "        return Collections.unmodifiableMap(new LinkedHashMap<>(picks));",
     "        return Map.copyOf(picks);"),
    ("dialogue: a departed player's pick stays on the table",
     f"{MAIN}/dialogue/Conversation.java",
     "        picks.remove(participant);\n        return participants.remove(participant);",
     "        return participants.remove(participant);"),
    ("dialogue: the initiator can be removed, orphaning INITIATOR nodes",
     f"{MAIN}/dialogue/Conversation.java",
     "        if (participant.equals(initiator))\n"
     "            throw new IllegalArgumentException(\"cannot remove the initiator; end the conversation\");",
     ""),
    ("effects: everyone is judged on the winning option, not their own stance",
     f"{MAIN}/regard/RegardEffects.java",
     "            DialogueOption option = resolution.stanceOf(participant);",
     "            DialogueOption option = resolution.chosen();"),
    ("effects: a failed unanimous vote still gets recorded",
     f"{MAIN}/regard/RegardEffects.java",
     "        if (resolution.kind() != Resolution.Kind.ADVANCED) {\n"
     "            return applied;                    // nothing was settled, so nothing was said\n"
     "        }",
     ""),
    ("effects: the requested delta is reported instead of the applied one",
     f"{MAIN}/regard/RegardEffects.java",
     "                int delta = state.adjust(entry.getKey(), entry.getValue());",
     "                state.adjust(entry.getKey(), entry.getValue());\n"
     "                int delta = entry.getValue();"),
    ("facing: yaw is not negated, so everything faces backwards",
     f"{MAIN}/spatial/Facing.java",
     "        return (float) -Math.toDegrees(Math.atan2(dx, dz));",
     "        return (float) Math.toDegrees(Math.atan2(dx, dz));"),
    ("facing: the axes are swapped",
     f"{MAIN}/spatial/Facing.java",
     "        return (float) -Math.toDegrees(Math.atan2(dx, dz));",
     "        return (float) -Math.toDegrees(Math.atan2(dz, dx));"),
    ("chapter: high-water mark may regress",
     f"{MAIN}/chapter/ChapterState.java",
     "if (derived.band > highWater.band) highWater = derived;", "highWater = derived;"),
    ("chapter: deserialize trusts a saved chapter without re-deriving",
     f"{MAIN}/chapter/ChapterState.java",
     "st.recompute();", "if (false) st.recompute();"),
    ("chapter: prerequisites become any-of instead of all-of",
     f"{MAIN}/chapter/Chapter.java",
     "case EXODUS -> Set.of(Milestone.DEICIDE, Milestone.WARDEN_CONTACT,\n                                  Milestone.FIRST_CROSSING);",
     "case EXODUS -> Set.of(Milestone.FIRST_CROSSING);"),
    ("regard: deicide leaves no ceiling on survivors",
     f"{MAIN}/regard/RegardState.java",
     "                lowerCeiling(i, -10);", ""),
    ("regard: the victim's floor can be raised again",
     f"{MAIN}/regard/RegardState.java",
     "                lowerCeiling(i, MIN);", "                adjust(i, -100);"),
    ("regard: ghost relationship leaks to non-killers",
     f"{MAIN}/regard/RegardState.java",
     "if (i == Institution.THE_GHOST && !isKiller) return 0;", ""),
    ("regard: a deicide drags the villages down with the gods",
     f"{MAIN}/regard/RegardState.java",
     "            if (i == Institution.WARDENATE || i == Institution.THE_GHOST\n"
     "                    || i == Institution.VILLAGES) continue;",
     "            if (i == Institution.WARDENATE || i == Institution.THE_GHOST) continue;"),
    ("regard: adjust reports the requested delta, not the applied one",
     f"{MAIN}/regard/RegardState.java",
     "return after - before;", "return delta;"),

    # The crossing guards. Each of these is a way the band notices turn back into
    # the karma bar they exist to avoid, and none of them would raise an exception
    # or look wrong in a log -- they would just quietly tell the player the wrong
    # thing, which is why they are here rather than trusted by reading.
    ("crossing: every movement is reported, not just the ones that cross",
     f"{MAIN}/regard/Standings.java",
     "if (was != null && was != now) {", "if (was != null) {"),
    ("crossing: a fall is reported as a rise",
     f"{MAIN}/regard/BandChange.java",
     "return to.ordinal() > from.ordinal();", "return true;"),
    ("crossing: an event that crossed nothing is allowed through",
     f"{MAIN}/regard/BandChange.java",
     "if (from == to) {", "if (false) {"),
    ("crossing: institutions are reported in whatever order a map iterates",
     f"{MAIN}/regard/Standings.java",
     "for (Institution i : Institution.values()) {\n            Standing was = before.get(i);",
     "for (Institution i : before.keySet().stream()\n                .sorted(java.util.Comparator.comparing(Enum::name)).toList()) {\n            Standing was = before.get(i);"),
]


def run(workdir):
    """-> (compiled, selftest_exit). Measures the JVM's exit, never a pipe's."""
    build = os.path.join(workdir, "build")
    os.makedirs(build, exist_ok=True)
    srcs = []
    for r, _, fs in os.walk(os.path.join(workdir, "core", "src")):
        srcs += [os.path.join(r, f) for f in fs if f.endswith(".java")]
    cj = subprocess.run(["javac", "-d", build] + srcs, capture_output=True, text=True)
    if cj.returncode != 0:
        return False, None
    rj = subprocess.run(["java", "-cp", build,
                         "com.cadykaya.interregnum.core.SelfTest"],
                        capture_output=True, text=True)
    return True, rj.returncode


def main():
    ok, code = run_in_place()
    if not ok or code != 0:
        print("FAIL: the unmutated self-test does not pass; fix that first")
        return 1
    print(f"baseline: self-test passes\n")

    survivors = []
    for desc, rel, find, repl in MUTATIONS:
        with tempfile.TemporaryDirectory() as td:
            shutil.copytree(CORE, os.path.join(td, "core"))
            p = os.path.join(td, rel.replace("core/", "core/", 1))
            src = open(p).read()
            if find not in src:
                print(f"  [STALE ] {desc}\n           pattern no longer present in {rel}")
                survivors.append(desc + " (stale pattern)")
                continue
            open(p, "w").write(src.replace(find, repl, 1))
            compiled, exit_code = run(td)
            if not compiled:
                print(f"  [SKIP  ] {desc} (mutant did not compile)")
                continue
            if exit_code == 0:
                print(f"  [SURVIVED] {desc}")
                survivors.append(desc)
            else:
                print(f"  [caught] {desc}")

    print()
    if survivors:
        print(f"FAIL: {len(survivors)} mutation(s) not caught by the self-test:")
        for s in survivors:
            print("  -", s)
        return 1
    print(f"OK: all {len(MUTATIONS)} mutations caught")
    return 0


def run_in_place():
    with tempfile.TemporaryDirectory() as td:
        shutil.copytree(CORE, os.path.join(td, "core"))
        return run(td)


if __name__ == "__main__":
    sys.exit(main())
