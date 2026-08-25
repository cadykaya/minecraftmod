package com.cadykaya.interregnum.system.dialogue;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * What a player is, for the purpose of what they may say.
 *
 * Dialogue options can be gated on tags (`class/theoclast`, and later things like
 * `cited/3`), and this is the one place that answers what a given player carries.
 *
 * **It returns nothing today, and that is correct rather than unfinished.** The only
 * tag any written scene uses is `class/theoclast`, and the Theoclast class does not
 * exist yet -- no clast can be attuned, so no player can truthfully hold it. Hiding
 * those options is the honest answer. When attunement lands, it lands here, and every
 * scene already written starts offering its gated lines with no edit to the scenes.
 *
 * Kept as a named seam rather than an inline `Set.of()` at the call site so that
 * "where do tags come from" has an answer a reader can find.
 */
public final class PlayerTags {
    private PlayerTags() {}

    public static Set<String> of(ServerPlayer player) {
        return Set.of();
    }
}
