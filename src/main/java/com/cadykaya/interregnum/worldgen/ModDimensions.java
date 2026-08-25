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
 * with a single fixed biome: it generates ground you can stand on and nothing more. The
 * under-layer and far-layer do not exist, the portal logic that would join them does not
 * exist, and no letter can be delivered here yet. Saying so in the file is cheaper than
 * a later reader inferring that a placeholder was a decision.
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

    /** The Verdant's surface. Docket header again, not the god's name. */
    public static final ResourceKey<DimensionType> GREEN_AUTHORITY_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "green_authority"));

    public static final ResourceKey<LevelStem> GREEN_AUTHORITY_STEM =
            ResourceKey.create(Registries.LEVEL_STEM,
                    Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "green_authority"));

    public static final ResourceKey<net.minecraft.world.level.Level> GREEN_AUTHORITY =
            ResourceKey.create(Registries.DIMENSION, GREEN_AUTHORITY_STEM.identifier());

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

        // Placeholder terrain, stated as such in the class javadoc: vanilla noise, one
        // biome, ground you can stand on. The law above is the part that is designed.
        ctx.register(UNRESPONSIVE_STEM, new LevelStem(
                types.getOrThrow(UNRESPONSIVE_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(quietBiome())),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        ctx.register(MASS_AUTHORITY_STEM, new LevelStem(
                types.getOrThrow(MASS_AUTHORITY_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(quietBiome())),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));

        // The one place a vanilla biome is chosen for what it DOES carry rather than
        // for what it does not: the Verdant's law is growth, and a world where things
        // grow needs things that can. Still a placeholder in the same sense as the
        // others -- the terrain is not designed.
        ctx.register(GREEN_AUTHORITY_STEM, new LevelStem(
                types.getOrThrow(GREEN_AUTHORITY_TYPE),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS)),
                        noise.getOrThrow(NoiseGeneratorSettings.OVERWORLD))));
    }

    /**
     * The one biome, and it is chosen for what it does NOT carry.
     *
     * Every ordinary vanilla biome ships ambience, music and mob spawns that would be
     * doing scenic work this world has not designed. `the_void` carries none of it, so
     * nothing here is speaking on the Quiet One's behalf by accident.
     */
    private static ResourceKey<Biome> quietBiome() {
        return Biomes.THE_VOID;
    }
}
