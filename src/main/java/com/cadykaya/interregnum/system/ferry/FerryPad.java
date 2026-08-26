package com.cadykaya.interregnum.system.ferry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;

/**
 * The far pad: where a crossing arrives, in every world that has one.
 *
 * <h2>Locked, and it was a command argument</h2>
 *
 * `WORLD.md`: *"a keel block captures the structure, validates it against the
 * destination's law, and re-places it at <b>the far pad</b>."* There was no pad. The
 * arrival position was a parameter an operator typed, which meant the ferry did not go
 * anywhere in particular — it went wherever you said, and a mail service whose
 * destination is an argument is not a mail service.
 *
 * <h2>The same dock, four times, in whatever was to hand</h2>
 *
 * Every pad is the identical seven-by-seven with the identical three-by-three landing and
 * the identical four corner posts. Only the material differs, and it differs because the
 * Post built each one out of whatever that world had.
 *
 * That is the joke the rest of the mod's bureaucracy runs on, said in blocks: an
 * institution does not redesign its dock for each god. It has a standard dock. The
 * standard dock is why you can step off a ferry in a world where nothing answers and
 * still know exactly where the landing square is — which is also, quietly, the only
 * navigational aid any of these worlds has.
 *
 * <h2>It is not claimed, and it is rebuilt when it has gone</h2>
 *
 * The claim ledger records <b>what a player placed</b>, and nobody placed this. So the
 * Verdant grows over its pad, the Turning ages it, attrition would take its distinctions
 * — all correct, all in character, and none of it survivable for a landing square that a
 * crossing has to be able to find. {@link #ensure} rebuilds a pad whose landing has gone,
 * which reads as the only thing left of the Post still doing its job.
 */
public final class FerryPad {
    private FerryPad() {}

    /** Half-width of the platform: 3 gives the seven-by-seven. */
    private static final int APRON = 3;
    /** Half-width of the landing square, where the keel comes down. */
    private static final int LANDING = 1;

    /** What each world's dock is made of: the apron, and the landing square. */
    private record Kit(Block apron, Block landing) {}

    /**
     * Four worlds, four kits, one shape.
     *
     * Each is that world's plainest worked stone, so the dock reads as local material
     * rather than as something shipped in — the Post used what was there. The
     * Hearth-Turner's arrives already cracked, because in that world everything has a
     * past and a new dock would be the one object in it that did not.
     */
    private static final Map<ResourceKey<Level>, Kit> KITS = Map.of(
            key("unresponsive"), new Kit(Blocks.STONE, Blocks.POLISHED_ANDESITE),
            key("mass_authority"), new Kit(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES),
            key("green_authority"), new Kit(Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS),
            key("temporal_authority"), new Kit(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS));

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("interregnum", path));
    }

    /**
     * Is there already a ferry on the landing?
     *
     * Without this the second crossing to a world lands ON the first and silently
     * replaces whatever of it shared a coordinate — a hull deleted by another hull, with
     * nothing anywhere reporting it. One dock per world is the design; a queue is not,
     * and inventing one here would be inventing a mechanic. So the Post does the thing a
     * dock actually does and refuses to bring a second vessel down onto an occupied
     * berth.
     *
     * The test is a keel, not "is anything there". The apron is walkable and a player
     * standing on the pad, or a block they put down beside it, must not close the world.
     */
    public static boolean occupied(ServerLevel level, BlockPos arrival) {
        return level.getBlockState(arrival)
                .is(com.cadykaya.interregnum.registry.ModBlocks.FERRY_KEEL.get());
    }

    /** Whether this world has a dock at all. */
    public static boolean serves(ServerLevel level) {
        return KITS.containsKey(level.dimension());
    }

    /**
     * Build the pad if it is not there, and say where a keel should come down.
     *
     * @return the block a keel arrives on top of the landing at, or null if this world
     *         has no dock — which is not an error, it is what the overworld is.
     *
     * The column is (0, 0) in every world, and the height is read off the world's own
     * surface heightmap rather than fixed. A hard-coded y would bury the dock in one
     * world and leave it in the air in another, and the four worlds do not agree about
     * where their ground is.
     */
    public static BlockPos ensure(ServerLevel level) {
        Kit kit = KITS.get(level.dimension());
        if (kit == null) {
            return null;
        }
        // LOOK FOR THE DOCK BEFORE COMPUTING WHERE ONE WOULD GO, and the order is the
        // whole of this method's history. The first version asked the surface heightmap
        // for a height and checked the block there -- which works exactly once, because
        // building the dock RAISES the surface, so the second crossing measured a
        // different height, found no landing, and built a second dock a block above the
        // first. A position derived from the world cannot be derived from a world the
        // thing has already changed.
        //
        // Scanning the column instead is immune to that by construction: it finds the
        // dock wherever it is, including after a player has built on the apron or the
        // Verdant has grown over it. 256 block reads, once per crossing.
        for (int y = level.getMaxY(); y >= level.getMinY(); y--) {
            BlockPos at = new BlockPos(0, y, 0);
            if (level.getBlockState(at).is(kit.landing())) {
                return at.above();
            }
        }

        // No dock yet. `WORLD_SURFACE` is right here and only here: nothing has been
        // built at this column, so the surface it reports is the terrain's.
        BlockPos centre = new BlockPos(0, level.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0), 0);

        for (int dx = -APRON; dx <= APRON; dx++) {
            for (int dz = -APRON; dz <= APRON; dz++) {
                BlockPos at = centre.offset(dx, 0, dz);
                boolean isLanding = Math.abs(dx) <= LANDING && Math.abs(dz) <= LANDING;
                level.setBlock(at, (isLanding ? kit.landing() : kit.apron())
                        .defaultBlockState(), 3);
                // Everything above the deck is cleared to the height a hull needs, so a
                // crossing does not arrive inside a hillside. Three blocks: the landing
                // square plus room to stand, which is what the dock is for.
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlock(at.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        // Four posts, so the dock is findable across flat ground and unmistakable from
        // any angle. They mark the apron's corners, not the landing's -- standing
        // between two of them puts you on the deck.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                level.setBlock(centre.offset(sx * APRON, 1, sz * APRON),
                        kit.landing().defaultBlockState(), 3);
            }
        }
        return centre.above();
    }
}
