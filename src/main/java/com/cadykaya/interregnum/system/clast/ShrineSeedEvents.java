package com.cadykaya.interregnum.system.clast;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.clast.Clasts;
import com.cadykaya.interregnum.registry.ModAttachments;
import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;

/**
 * The shrines' share of the god, handed over as they are found.
 *
 * `WORLD.md`, locked: *"The overflow detonates outward, scattering splinters at shrines
 * and the crater."* The crater's share is dealt at the moment of death, in {@link
 * com.cadykaya.interregnum.system.Deicide}. The shrines' cannot be: the deicide can only
 * reach chunks that happen to be loaded when it happens, which is the same constraint the
 * statues have, solved the same way.
 *
 * <h2>And it is the better beat, as it is for the statues</h2>
 *
 * A player who walks to a shrine some days later and finds something lying on the step
 * has found it. A world that put everything in place in one instant, most of it where
 * nobody was, has only made a list.
 *
 * <h2>Marked whether or not anything was handed over</h2>
 *
 * See {@link Seeded}. The pool is finite, so a shrine chunk that loaded, unloaded and
 * loaded again must not take a second clast — and a shrine that loaded after the pool ran
 * dry is finished with rather than waiting, so it is marked too. Otherwise every empty
 * shrine in the world would rescan its own sections on every load, forever, for an answer
 * that cannot change.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class ShrineSeedEvents {
    private static final Logger LOG = LogUtils.getLogger();

    private ShrineSeedEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        // The god still lives: nothing has shattered, so there is nothing to find.
        if (ChapterSavedData.isDormant(level)) {
            return;
        }
        Seeded seeded = chunk.getData(ModAttachments.SEEDED);
        if (seeded.isSeeded()) {
            return;
        }
        // CHEAPEST TEST LAST, on purpose, and it is the opposite ordering to the entity
        // handlers. There the instanceof throws away almost every call; here the
        // attachment read is a map lookup that throws away every chunk in the world after
        // its first load, and the section scan behind it is the expensive part.
        BlockPos masonry = findShrine(chunk);
        if (masonry == null) {
            return;
        }
        int share = ClastsSavedData.get(level.getServer()).take(Clasts.AT_SHRINE);
        int dropped = Scatter.drop(level, masonry, share);
        if (seeded.mark()) {
            chunk.markUnsaved();
        }
        if (dropped > 0) {
            LOG.info("{} clast(s) came to rest at the shrine in {}.",
                    dropped, chunk.getPos());
        }
    }

    /**
     * The top of this chunk's shrine masonry, or null if there is none.
     *
     * Palette-gated per section exactly as `Leaks.holdsShrine` is, so a chunk of stone and
     * air costs one predicate pass -- but this one has to return WHERE, because a clast
     * dropped at the chunk's corner would be lying in a field rather than on the shrine.
     * Highest match wins: the shrine's own top course, so what lands is on it rather than
     * inside it.
     */
    private static BlockPos findShrine(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()
                    || !section.maybeHas(s -> s.is(ModBlocks.SHRINE_STONE.get())
                            || s.is(ModBlocks.SHRINE_STONE_CARVED.get()))) {
                continue;
            }
            int bottom = chunk.getMinY() + (i << 4);
            for (int y = 15; y >= 0; y--) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        var state = section.getBlockState(x, y, z);
                        if (state.is(ModBlocks.SHRINE_STONE.get())
                                || state.is(ModBlocks.SHRINE_STONE_CARVED.get())) {
                            return new BlockPos(
                                    chunk.getPos().getMinBlockX() + x,
                                    bottom + y,
                                    chunk.getPos().getMinBlockZ() + z);
                        }
                    }
                }
            }
        }
        return null;
    }
}
