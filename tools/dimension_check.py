"""The god-worlds' laws are still the laws.

Three worlds so far, and the most valuable thing this file does is hold them APART.
All three have an opinion about a bed and all three opinions are different: the Quiet
One declines to react at all, the Anchorite detonates, the Verdant lets you sleep and
refuses to hold your spawn. Two booleans and an enum carry most of the difference
between three gods, they cost nothing to lose in a refactor, and no test looking at one
dimension alone would notice them converging.

`tools/crossing_check.sh` proves the Quiet One's world is a separate place with its
own floor. It cannot prove the thing that makes the place worth crossing to, because
a dedicated server exposes no command that reads a dimension's attributes back. So
the law is asserted here, where it lives: in the generated `dimension_type` JSON.

That is a weaker kind of proof and it is labelled as one. It says *the data we ship
declares this*, not *the game behaves like this*. What it does catch is the failure
that would actually happen: somebody edits `ModDimensions.java` -- to fix a colour, to
add an attribute, to silence a warning -- regenerates, and quietly hands a bed back
its explosion. Nothing would fail. Nobody would notice until a player slept.

The specific things asserted, and why each is the law rather than decoration:

  * `bed_rule` says never/never, does NOT explode, and carries NO error message.
    The Nether and the End both refuse a bed loudly. This place declines to react,
    and the whole difference is the absent `explodes` and the absent message. If
    either reappears, the Quiet One has started answering.
  * `respawn_anchor_works` is false -- the same refusal, the same silence.
  * `can_start_raid` is false -- nothing is summoned here, by anybody.
  * `audio/ambient_sounds` and `audio/background_music` are PRESENT and EMPTY.
    Present matters as much as empty: an omitted attribute means *inherit*, and what
    would be inherited is the overworld's cave moaning. An empty dict here is a
    decision; a missing key is an accident that sounds exactly like the overworld.
  * `min_y` is 0 -- the fact `crossing_check.sh` tests live, asserted here too so a
    change to it fails fast in the gate rather than after a twelve-minute CI run.

    python3 tools/dimension_check.py
"""
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN = os.path.join(REPO, "src/generated/resources/data/interregnum")

fails = []


def law(path, checks):
    full = os.path.join(GEN, "dimension_type", path + ".json")
    if not os.path.exists(full):
        fails.append(f"{path}: no generated dimension_type -- run `gradle runServerData`")
        return
    with open(full) as fh:
        dt = json.load(fh)
    attrs = dt.get("attributes", {})
    for describe, ok in checks(dt, attrs):
        if not ok:
            fails.append(f"{path}: {describe}")


def unresponsive(dt, a):
    bed = a.get("minecraft:gameplay/bed_rule")
    yield ("a bed is not refused at all -- `bed_rule` is absent, so this world "
           "inherits the overworld's and you can sleep in it", bed is not None)
    if bed is not None:
        yield ("a bed can be slept in", bed.get("can_sleep") == "never")
        yield ("a bed can set spawn -- this place can be made a home",
               bed.get("can_set_spawn") == "never")
        yield ("the bed EXPLODES. That is the Nether's answer and the End's answer: "
               "both refuse you loudly. The Quiet One does not react at all, and "
               "that difference is the entire character",
               bed.get("explodes", False) is False)
        yield ("the bed carries an error message. It is not supposed to say anything, "
               "including no",
               "error_message" not in bed)

    yield ("a respawn anchor works here",
           a.get("minecraft:gameplay/respawn_anchor_works") is False)
    yield ("a raid can start here -- something can be summoned",
           a.get("minecraft:gameplay/can_start_raid") is False)

    for key, what in (("minecraft:audio/ambient_sounds", "ambient mood sounds"),
                      ("minecraft:audio/background_music", "background music")):
        yield (f"{what} are not declared at all. An omitted attribute INHERITS, so "
               f"this world would sound like the overworld -- which is the one thing "
               f"it must not do. It has to be present and empty, not absent",
               key in a)
        if key in a:
            yield (f"{what} are declared and non-empty", a[key] == {})

    yield ("min_y is not 0 -- crossing_check.sh's live floor assertion is testing "
           "something other than what this file describes", dt.get("min_y") == 0)


def mass_authority(dt, a):
    """The Anchorite refuses a bed too, and it is important that it refuses it
    DIFFERENTLY. Two worlds rejecting the same object for visibly different reasons
    is most of what makes them feel like different people, and the difference here
    is one boolean: the Quiet One declines to react, the Anchorite detonates. If
    these two ever converge, the pantheon has lost a character."""
    bed = a.get("minecraft:gameplay/bed_rule")
    yield ("`bed_rule` is absent, so this world inherits the overworld's",
           bed is not None)
    if bed is not None:
        yield ("a bed can be slept in", bed.get("can_sleep") == "never")
        yield ("the bed does NOT explode. That is the Quiet One's silence, and it is "
               "the wrong god: this world is not unresponsive, it is unmoored, and it "
               "answers a bed immediately",
               bed.get("explodes", False) is True)
    yield ("a respawn anchor works here",
           a.get("minecraft:gameplay/respawn_anchor_works") is False)
    yield ("min_y is not 0 -- crossing coordinates in the live checks assume it is",
           dt.get("min_y") == 0)


law("unresponsive", unresponsive)
def green_authority(dt, a):
    """The Verdant's answer to a bed is the THIRD different one, and that is the point.
    The Quiet One declines to react; the Anchorite detonates; the Verdant lets you lie
    down and will not hold your spawn. WORLD.md: "the one who covered" -- during an older
    crisis it took over the overworld's duties and never quite handed them back, and the
    estrangement is professional rather than personal. It will cover you for a night. It
    will not take responsibility for you."""
    bed = a.get("minecraft:gameplay/bed_rule")
    yield ("`bed_rule` is absent, so this world inherits the overworld's",
           bed is not None)
    if bed is not None:
        yield ("you cannot sleep here. The Verdant covers; it does not refuse",
               bed.get("can_sleep") == "when_dark")
        yield ("a bed sets spawn here -- this world would take responsibility for you, "
               "which is the one thing the one who covered will not do",
               bed.get("can_set_spawn") == "never")
        yield ("the bed explodes. That is the Anchorite's answer, not this one",
               bed.get("explodes", False) is False)
    yield ("a respawn anchor works here",
           a.get("minecraft:gameplay/respawn_anchor_works") is False)


law("mass_authority", mass_authority)
law("green_authority", green_authority)

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} law violation(s)")
    sys.exit(1)

print("\nOK: nobody answers, nothing holds still, and nothing takes responsibility for you")
