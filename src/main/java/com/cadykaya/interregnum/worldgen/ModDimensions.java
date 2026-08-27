package com.cadykaya.interregnum.worldgen;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.Optional;

import com.cadykaya.interregnum.Interregnum;

/**
 * The god-worlds, as data.
 *
 * `WORLD.md` locks the grammar: each god holds a **system** of connected dimensions
 * (surface / under-layer / far-layer), and travel between systems is only by ferry.
 * This file currently holds the first surface layer of the first system, and it is
 * honest about being that -- see "What is deliberately not here" at the bottom.
 *
 * <h2>Why a dimension has to earn itself</h2>
 *
 * `AESTHETIC.md` supplies the executioner, quoted in `WORLDGEN.md`:
 *
 * > **Could I replace it with a different random weird thing without changing anything?**
 *
 * "The same game with purple stone" fails that: swap purple for green and nothing moves.
 * A dimension earns itself when its **rules** differ -- when something is possible or
 * impossible there that is not elsewhere, and the player has to adapt.
 *
 * <h2>The Quiet One's law is not "quiet". It is that nothing here answers you.</h2>
 *
 * Silence-as-decoration would be a muted soundtrack, and would fail the test above. The
 * law that earns the world is already written on every Warden docket that mentions it:
 * `SUBJECT: UNRESPONSIVE`. So every affordance in Minecraft that consists of *asking the
 * world for something* is dead here, and each one is an attribute rather than code:
 *
 * <ul>
 *   <li><b>A bed does nothing at all.</b> Not "you may not sleep here" -- <em>nothing</em>.
 *       {@link BedRule} with {@code canSleep = NEVER}, {@code canSetSpawn = NEVER},
 *       {@code explodes = false}, and <b>no error message</b>. The Nether refuses you
 *       loudly and the End refuses you loudly; both explode. This place declines to
 *       react, which is worse, and the whole difference is one empty {@code Optional}.</li>
 *   <li><b>A respawn anchor does nothing.</b> Same refusal, same silence.</li>
 *   <li><b>No raid can ever start.</b> Nothing is summoned here, by anybody.</li>
 *   <li><b>The world makes no sound of its own.</b> No ambient mood cues, no background
 *       music -- both attributes are set to their EMPTY value rather than left unset,
 *       because unset means "inherit", and inheriting the overworld's cave moans is the
 *       one thing this dimension must not do.</li>
 * </ul>
 *
 * A player crossing here has to adapt: there is no way to shorten a night, no way to
 * make this place a home, and no way to be answered. That is a rule, not a palette.
 *
 * <h2>The crossing already taught this</h2>
 *
 * The ferry refuses cargo that can make a sound for the Quiet One's crossing, and has
 * done since before this dimension existed. A player has already read that checklist by
 * the time they arrive. This file is the other half of that sentence finally being true.
 *
 * <h2>What is deliberately not here</h2>
 *
 * <b>Terrain is a placeholder and is not designed.</b> Vanilla's overworld noise settings
 * with a single fixed biome: it generates ground you can stand on and nothing more. That
 * is still true of every level here, including the new one.
 *
 * <b>One system now has two of its three layers.</b> The Anchorite's under-layer exists
 * and is joined to its surface by that god's own portal -- see
 * {@link com.cadykaya.interregnum.core.portal.Descent}. The other three gods have their
 * surface only, and no god has a far-layer. `WORLD.md` locks all four portals; three of
 * them are unbuilt, and the mechanisms they need (a plant with a lifespan, an hour you
 * have to make, a silence you can break by coughing) have nothing in common with this one
 * beyond the rule that the school is the key.
 *
 * Saying so in the file is cheaper than a later reader inferring that a placeholder was a
 * decision.
 */
public final class ModDimensions {
    private ModDimensions() {}

    /**
     * The Quiet One's surface.
     *
     * Named for the docket header rather than for the god, because the mod's naming
     * doctrine is that nobody agrees what anything is called and the Wardenate's word is
     * the one written down. Villagers will not say it at all.
     */
    public static final ResourceKey<DimensionType> UNRESPONSIVE_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "unresponsive"));

    public static final ResourceKey<LevelStem> UNRESPONSIVE_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "unresponsive"));

    /** The level id a command targets: `execute in interregnum:unresponsive run ...`. */
    public static final ResourceKey<net.minecraft.world.level.Level> UNRESPONSIVE =
            ResourceKey.create(Registries.DIMENSION, UNRESPONSIVE_STEM.identifier());

    /**
     * The Anchorite's surface.
     *
     * Named, like the Quiet One's, for the docket header rather than for the god.
     */
    public static final ResourceKey<DimensionType> MASS_AUTHORITY_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "mass_authority"));

    public static final ResourceKey<LevelStem> MASS_AUTHORITY_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "mass_authority"));

    public static final ResourceKey<net.minecraft.world.level.Level> MASS_AUTHORITY =
            ResourceKey.create(Registries.DIMENSION, MASS_AUTHORITY_STEM.identifier());

    /**
     * Under the Anchorite's surface: <b>the place where down does not hold.</b>
     *
     * `WORLD.md` locks the grammar — *surface · under-layer · far-layer, joined by that
     * world's own portal logic* — and this is the first under-layer any god has. It is
     * reached only by {@link com.cadykaya.interregnum.core.portal.Descent}: there is no
     * ferry law naming it, because the ferry crosses BETWEEN systems and this is inside
     * one.
     *
     * Named the way the other four are, for the docket rather than for the god. The
     * Wardenate files a place under the authority that governs it and appends where in
     * it, which is how a bureau ends up with the only map anybody has.
     */
    public static final ResourceKey<DimensionType> MASS_AUTHORITY_LOWER_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "mass_authority_lower"));

    public static final ResourceKey<LevelStem> MASS_AUTHORITY_LOWER_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "mass_authority_lower"));

    public static final ResourceKey<net.minecraft.world.level.Level> MASS_AUTHORITY_LOWER =
            ResourceKey.create(Registries.DIMENSION, MASS_AUTHORITY_LOWER_STEM.identifier());

    /** The Verdant's surface. Docket header again, not the god's name. */
    public static final ResourceKey<DimensionType> GREEN_AUTHORITY_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "green_authority"));

    public static final ResourceKey<LevelStem> GREEN_AUTHORITY_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "green_authority"));

    public static final ResourceKey<net.minecraft.world.level.Level> GREEN_AUTHORITY =
            ResourceKey.create(Registries.DIMENSION, GREEN_AUTHORITY_STEM.identifier());

    /** The Hearth-Turner's surface. */
    public static final ResourceKey<DimensionType> TEMPORAL_AUTHORITY_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "temporal_authority"));

    public static final ResourceKey<LevelStem> TEMPORAL_AUTHORITY_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "temporal_authority"));

    public static final ResourceKey<net.minecraft.world.level.Level> TEMPORAL_AUTHORITY =
            ResourceKey.create(Registries.DIMENSION, TEMPORAL_AUTHORITY_STEM.identifier());

    /**
     * Deliberately unlike the overworld's -64..320.
     *
     * This is load-bearing for verification as well as for feel. A dimension that
     * merely *exists* is indistinguishable from a second copy of the overworld, and a
     * check that only proves "a level with this id loaded" would pass against a
     * mis-wired stem pointing at `minecraft:overworld`. A floor at y=0 is a fact a
     * command can test: `setblock` below it fails here and succeeds at home, so the
     * assertion proves our dimension_type is the one actually in force.
     */
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;

    public static void bootstrapTypes(BootstrapContext<DimensionType> ctx) {
        var blocks = ctx.lookup(Registries.BLOCK);
        ctx.register(UNRESPONSIVE_TYPE, new DimensionType(
                false,                              // hasFixedTime -- the sky still turns.
                                                    // Stopping it is the Hearth-Turner's
                                                    // law, and borrowing another god's
                                                    // signature to decorate this one is
                                                    // exactly the mush the four-law split
                                                    // exists to prevent.
                true,                               // hasSkyLight
                false,                              // hasCeiling
                false,                              // hasEnderDragonFight
                1.0,                                // coordinateScale
                MIN_Y,
                HEIGHT,
                HEIGHT,                             // logicalHeight
                // A HolderSet, not the TagKey: the record wants the resolved set, and
                // the lookup is the only thing that can resolve one at bootstrap time.
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,                               // ambientLight
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                unresponsive(),
                HolderSet.empty(),                  // timelines: no weather, ever
                Optional.empty()));                 // defaultClock

        // The Anchorite's world. Its law is CODE, not attributes -- see
        // com.cadykaya.interregnum.system.anchorite.Anchorite for why, and for what
        // 26.2 does and does not let a dimension declare about weight.
        //
        // What the attributes CAN carry is the consequence of that law. Nothing here
        // holds still, so nothing here can be a home either: the bed and the anchor
        // fail exactly as they do for the Quiet One, but for the opposite reason.
        // There it is that nobody answers. Here it is that nothing stays put.
        ctx.register(MASS_AUTHORITY_TYPE, new DimensionType(
                false, true, false, false, 1.0,
                MIN_Y, HEIGHT, HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                unmoored(),
                HolderSet.empty(),
                Optional.empty()));

        // Under it. The same god, one layer down, and the attributes say the same thing
        // harder: no sky at all, and a faint light of its own because there is no sun to
        // borrow. A bed still detonates -- it is the Anchorite's, and a place does not
        // stop being a god's for being underneath.
        //
        // `hasSkyLight` false is the one attribute here doing structural work rather
        // than atmospheric: it is what makes this READ as an under-layer to everything
        // in the game that asks, including the light engine, without pretending the
        // terrain has a ceiling it does not have. See the note on terrain below.
        ctx.register(MASS_AUTHORITY_LOWER_TYPE, new DimensionType(
                false,
                false,                              // hasSkyLight -- there is no sky
                false,                              // hasCeiling -- and no roof either;
                                                    // the terrain is vanilla noise and
                                                    // declaring a ceiling it does not
                                                    // generate would be a lie in data
                false, 1.0,
                MIN_Y, HEIGHT, HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.1F,                               // ambientLight: the only one of the
                                                    // five with any. Pitch dark is not a
                                                    // law, it is an absence of one
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                unmoored(),
                HolderSet.empty(),
                Optional.empty()));

        // The Verdant's world. Its law is also code -- growth has no attribute either
        // -- and lives in com.cadykaya.interregnum.system.verdant.Verdant.
        ctx.register(GREEN_AUTHORITY_TYPE, new DimensionType(
                false, true, false, false, 1.0,
                MIN_Y, HEIGHT, HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                covered(),
                HolderSet.empty(),
                Optional.empty()));

        // The Hearth-Turner's world. Its law is the ageing table -- see
        // com.cadykaya.interregnum.system.hearth.Hearth.
        //
        // The ONE dimension here with a fixed sky, and it is the one god entitled to it.
        // Stopping the day is a signature the other three had to be denied precisely so
        // it would mean something when this one used it: a world whose law is keeping
        // every past does not get to have an afternoon that becomes evening.
        ctx.register(TEMPORAL_AUTHORITY_TYPE, new DimensionType(
                true,                               // hasFixedTime
                true, false, false, 1.0,
                MIN_Y, HEIGHT, HEIGHT,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.0F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                kept(),
                HolderSet.empty(),
                Optional.empty()));
    }

    /**
     * A bed you can sleep in, that will hold your spawn, and it changes nothing.
     *
     * The fourth answer, and the only permissive one — which is the joke. Sleeping
     * passes the night, and the night here does not pass: `hasFixedTime` is on. So the
     * bed works perfectly, does exactly what a bed does, and achieves nothing, and the
     * player has to work out why. The Hearth-Turner does not refuse you anything. It
     * simply does not let go of the time you were trying to skip.
     */
    private static EnvironmentAttributeMap kept() {
        return EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.BED_RULE,
                        new BedRule(BedRule.Rule.ALWAYS, BedRule.Rule.ALWAYS, false,
                                Optional.empty()))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, true)
                .build();
    }

    /**
     * You may sleep here. You may not stay.
     *
     * The third answer to a bed, and all three are different on purpose. The Quiet One
     * declines to react; the Anchorite detonates; the Verdant lets you lie down and
     * refuses to hold your spawn. That is `WORLD.md`'s **"the one who covered"** exactly
     * -- during an older crisis it took over the overworld's duties and never quite
     * handed them back, and the estrangement is professional rather than personal. It
     * will cover you for a night. It will not take responsibility for you.
     */
    private static EnvironmentAttributeMap covered() {
        return EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.BED_RULE,
                        new BedRule(BedRule.Rule.WHEN_DARK, BedRule.Rule.NEVER, false,
                                Optional.empty()))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .build();
    }

    /**
     * A bed you cannot sleep in, and it DOES explode.
     *
     * Deliberately not the Quiet One's silence. Setting a bed down in a world where
     * unanchored things rise is a mistake the world will answer immediately and
     * loudly, and the Anchorite is not the god who declines to react. Two dimensions
     * refusing the same object for visibly different reasons is most of what makes
     * them feel like different people.
     */
    private static EnvironmentAttributeMap unmoored() {
        return EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.BED_RULE, BedRule.EXPLODES)
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .build();
    }

    /** The law, in vanilla's own vocabulary. See the class javadoc for why each one. */
    private static EnvironmentAttributeMap unresponsive() {
        return EnvironmentAttributeMap.builder()
                // Nothing. Not a refusal, not an explosion, not even a message.
                .set(EnvironmentAttributes.BED_RULE,
                        new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, false, Optional.empty()))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                // Set to EMPTY, not omitted: omitted means inherit, and what would be
                // inherited is the overworld's cave moaning.
                .set(EnvironmentAttributes.AMBIENT_SOUNDS,
                        net.minecraft.world.attribute.AmbientSounds.EMPTY)
                .set(EnvironmentAttributes.BACKGROUND_MUSIC,
                        net.minecraft.world.attribute.BackgroundMusic.EMPTY)
                .build();
    }

    public static void bootstrapStems(BootstrapContext<LevelStem> ctx) {
        var types = ctx.lookup(Registries.DIMENSION_TYPE);
        var noise = ctx.lookup(Registries.NOISE_SETTINGS);
        var biomes = ctx.lookup(Registries.BIOME);

        // Vanilla noise still -- the SHAPE of these worlds is not designed and the class
        // javadoc says so.
        //
        // These used `the_void`, and the reason is worth keeping now that it is gone:
        // every ordinary vanilla biome ships ambience, music and mob spawns that would
        // have been doing scenic work nobody had designed, so the placeholder was chosen
        // for what it did NOT carry. ModBiomes carries none of it either -- the same
        // restraint, with a colour attached instead of nothing. What is designed now is the biome: one per god, each carrying
        // that god's colour off the shared palette and nobody's mob spawns. See
        // ModBiomes, and note that three of these used `the_void` and the Verdant's used
        // `plains`, so until now the four worlds were visually identical grey stone and
        // one meadow with vanilla's whole spawn list in it.
        ctx.register(UNRESPONSIVE_STEM, new LevelStem(
                types.getOrThrow(UNRESPONSIVE_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(ModBiomes.UNANSWERED)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        ctx.register(MASS_AUTHORITY_STEM, new LevelStem(
                types.getOrThrow(MASS_AUTHORITY_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(ModBiomes.OLD_HEAVY)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        // The under-layer, on the same placeholder terms as its surface and on the same
        // biome. A biome of its own would be a colour decision about a place whose SHAPE
        // is not designed yet, and the two want deciding together -- see the class
        // javadoc, and HANDOFF's terrain item.
        ctx.register(MASS_AUTHORITY_LOWER_STEM, new LevelStem(
                types.getOrThrow(MASS_AUTHORITY_LOWER_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(ModBiomes.OLD_HEAVY)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        // The only one of the four that generates anything at all, and it has to be: the
        // Verdant's law is growth, and there is no point accelerating bare stone.
        ctx.register(GREEN_AUTHORITY_STEM, new LevelStem(
                types.getOrThrow(GREEN_AUTHORITY_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(ModBiomes.LONG_GREEN)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        ctx.register(TEMPORAL_AUTHORITY_STEM, new LevelStem(
                types.getOrThrow(TEMPORAL_AUTHORITY_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(ModBiomes.THE_TURNING)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));
    }

}
