package com.cadykaya.interregnum.system.warden;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.block.WardenStatueBlock;
import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.system.RegardSavedData;
import com.cadykaya.interregnum.system.regard.RegardNotices;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.slf4j.Logger;

/**
 * What it costs to go dark.
 *
 * A woken statue is a Warden's post. Tearing it down is the lever the design wants
 * players to find -- pull the statues out of your valley and enforcement stops
 * reaching it -- and a lever with no price is not a decision, it is just the correct
 * move. So the Wardenate notices.
 *
 * <h2>The rules this obeys, both from WORLD.md and both load-bearing</h2>
 *
 * **Only when a player did it.** A creeper taking out a statue is not defiance, and
 * charging somebody for it is the unfairness that teaches people never to build near
 * one. Same rule the shrine-keeper's death already follows.
 *
 * **An UNWOKEN statue costs nothing.** Before the death these are garden ornaments
 * and the Wardenate has no opinion about your landscaping. It is the woken one --
 * the one currently serving as a post -- that is an act.
 *
 * The cost is deliberately smaller than a keeper's murder. This is property, and the
 * Wardens are procedural about property: it is a citation, not a grievance. Enough
 * that a colonnade cleared in an afternoon is felt; not so much that one accident
 * with a stray pickaxe rewrites your standing.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class StatueBreakEvents {
    private static final Logger LOG = LogUtils.getLogger();

    private StatueBreakEvents() {}

    /** Per woken statue, to the Wardenate. */
    public static final int STATUE_COST = -8;


    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof WardenStatueBlock)
                || !state.getValue(WardenStatueBlock.WOKEN)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = level.getServer();
        RegardSavedData regard = RegardSavedData.get(server);
        RegardNotices.around(server, player.getUUID(), () -> {
            regard.of(server, player.getUUID()).adjust(Institution.WARDENATE, STATUE_COST);
            regard.touch();
        });
        LOG.info("A woken statue at {} was taken down by a player; the Wardenate noticed.",
                event.getPos());
    }
}
