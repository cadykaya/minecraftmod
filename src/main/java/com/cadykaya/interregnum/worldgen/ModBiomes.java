package com.cadykaya.interregnum.worldgen;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * One biome per god-world, named the way people name them rather than the way the
 * paperwork does.
 *
 * <h2>The names are the split the mod already has</h2>
 *
 * `WORLD.md`'s register lists two names for each god's world: the SUBJECT line on the
 * dead god's letters — `GREEN AUTHORITY`, `MASS AUTHORITY`, `TEMPORAL AUTHORITY`,
 * `UNRESPONSIVE` — and what people actually call the place: *the Long Green*, *Old
 * Heavy*, *the Turning*. The dimension ids are already the first set. These are the
 * second, so the id a player sees on the debug screen is the colloquial one and the id
 * in the ferry's paperwork is the civil-service one, which is the joke the whole
 * bureaucracy runs on.
 *
 * <b>And the Quiet One's world cannot take that name, because it does not have one.</b>
 * `WORLD.md`: three gods have a common name and the fourth's column reads *"they will
 * not say it"*. So its biome is named for the silence rather than for whoever is being
 * silent — {@code unanswered} — which is the same move the fourth letter makes when it
 * opens `To —`.
 *
 * <h2>Every colour here comes out of `assets/palette.json`</h2>
 *
 * Not "inspired by": the exact hex, off a ramp, and `tools/biome_check.py` fails the
 * build if any colour in the generated JSON is not one. A sky is as much a piece of art
 * direction as a texture is, and the palette system already exists to stop art direction
 * being decided one file at a time by whoever was in that file — see `ARTSTYLE.md`. It
 * was covering textures only, which is an arbitrary place for it to stop.
 *
 * <h2>26.2 moved most of this out of the biome</h2>
 *
 * {@link BiomeSpecialEffects} used to carry fog, sky, water-fog and the ambient sound
 * loops. In 26.2 it carries <b>water and vegetation colours only</b>, and everything
 * else moved into the same {@code EnvironmentAttributeMap} the dimension types here
 * already use for their laws — so a biome sets its sky with
 * {@code setAttribute(EnvironmentAttributes.SKY_COLOR, …)}. Written down in
 * `PLATFORM.md`, because a mod ported from a tutorial written against 1.21 would compile
 * against the shorter record and silently lose its sky.
 */
public final class ModBiomes {
    private ModBiomes() {}

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME,
                Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, path));
    }

    /** The Quiet One's. Named for the silence, because the god has no name to use. */
    public static final ResourceKey<Biome> UNANSWERED = key("unanswered");
    /** The Anchorite's — "Old Heavy" in the register. */
    public static final ResourceKey<Biome> OLD_HEAVY = key("old_heavy");
    /** The Verdant's — "the Long Green". */
    public static final ResourceKey<Biome> LONG_GREEN = key("long_green");
    /** The Hearth-Turner's — "the Turning". */
    public static final ResourceKey<Biome> THE_TURNING = key("the_turning");

    // ---- the palette, by name, so a reader can check these against assets/palette.json
    private static final int STONE_1 = 0x2B2C2E;
    private static final int STONE_2 = 0x4E5053;
    private static final int SKY_1 = 0x283649;
    private static final int METAL_1 = 0x262D33;
    private static final int METAL_2 = 0x44525C;
    private static final int METAL_3 = 0x6A7F8F;
    private static final int FOLIAGE_1 = 0x273A17;
    private static final int FOLIAGE_2 = 0x426227;
    private static final int FOLIAGE_3 = 0x64933A;
    private static final int FOLIAGE_4 = 0x87C74E;
    private static final int BRASS_1 = 0x644C1F;
    private static final int BRASS_2 = 0x9A7630;
    private static final int WOOD_1 = 0x4D341A;

    public static void bootstrap(BootstrapContext<Biome> ctx) {
        var features = ctx.lookup(Registries.PLACED_FEATURE);
        var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);

        // NOTHING SPAWNS IN ANY OF THEM, and that is a decision rather than an omission.
        // These are places you are sent to learn a school and deliver a letter, and a
        // skeleton in the middle of that is scenery nobody wrote. Three of the four were
        // already empty because they used `the_void`; the Verdant's used `plains` and
        // quietly inherited its whole spawn list.
        MobSpawnSettings nobody = MobSpawnSettings.EMPTY;

        // ---- the Quiet One: colourless, and the water is the colour of the rock -------
        // Nothing here is a different substance from anything else. The world does not
        // distinguish, because distinguishing would be answering.
        ctx.register(UNANSWERED, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, SKY_1)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, STONE_1)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, STONE_1)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(STONE_2)
                        .build())
                .mobSpawnSettings(nobody)
                .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
                .build());

        // ---- the Anchorite: cold, refined, holds its shape ---------------------------
        // The `metal` family, which the palette defines as "refined, cold, manufactured;
        // holds its shape" -- which is the Anchorite's whole law said as a colour.
        ctx.register(OLD_HEAVY, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.4F)
                .downfall(0.0F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, METAL_1)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, METAL_2)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, METAL_1)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(METAL_3)
                        .build())
                .mobSpawnSettings(nobody)
                .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
                .build());

        // ---- the Verdant: too green, and the sky is green too ------------------------
        // The only one of the four that is not restful, on purpose. `WORLD.md` locks
        // "accelerating growth is a HAZARD", so this is not a pleasant meadow: it is the
        // brightest step of the foliage ramp on the grass AND on the leaves AND in the
        // air, so there is nowhere in frame that is not the same green. A place that has
        // one colour left is a place something has gone wrong in.
        //
        // The only one with features, and it has to be: a world whose law is growth
        // needs things that can grow, and there is no point accelerating bare stone.
        var green = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultSoftDisks(green);
        BiomeDefaultFeatures.addPlainVegetation(green);
        BiomeDefaultFeatures.addDefaultFlowers(green);
        BiomeDefaultFeatures.addPlainGrass(green);
        BiomeDefaultFeatures.addDefaultMushrooms(green);
        ctx.register(LONG_GREEN, new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.8F)
                .downfall(0.9F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, FOLIAGE_3)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, FOLIAGE_2)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, FOLIAGE_1)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(FOLIAGE_2)
                        .grassColorOverride(FOLIAGE_4)
                        .foliageColorOverride(FOLIAGE_4)
                        .build())
                .mobSpawnSettings(nobody)
                .generationSettings(green.build())
                .build());

        // ---- the Hearth-Turner: light that never moves --------------------------------
        // `brass` is the palette's "mechanisms a player can operate", and this is the god
        // of keeping every version -- an archive, lit like one, in a permanent late
        // afternoon that is never allowed to become evening. Water the colour of old
        // varnish, because nothing here is fresh and nothing here is finished.
        ctx.register(THE_TURNING, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.7F)
                .downfall(0.0F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, BRASS_1)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, BRASS_2)
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, WOOD_1)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(WOOD_1)
                        .build())
                .mobSpawnSettings(nobody)
                .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
                .build());
    }
}
