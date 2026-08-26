package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Somebody said something in chat.
 *
 * Four lines of decision and a raycast; everything else is {@link Speech}, where a command
 * can reach it on a server with no players. The [VERIFY] note at the bottom says which
 * half of this file is checked and which cannot be.
 *
 * <h2>The event is never cancelled</h2>
 *
 * The message goes to chat exactly as typed, spell or not. That is the entire locked
 * reason casting is speech: *"you say the word, out loud, in chat, and everyone in earshot
 * sees you say it."* Swallowing the word to keep the channel tidy would leave a keybind
 * that happens to be typed, with no witness, no citation and no reason to prefer a cellar.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class SpeechEvents {
    private SpeechEvents() {}

    /**
     * How far a caster's gaze reaches for a target, in blocks.
     *
     * Vanilla's block-interaction reach is about five and this is deliberately longer: you
     * are not touching the thing, you are naming it. Kept short enough that what you meant
     * is never in doubt -- a spell landing on whatever happened to be under the crosshair
     * two hundred blocks away is a spell nobody can aim.
     */
    public static final double GAZE = 24.0;

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Speech.speak(level, player.blockPosition(), gazeTarget(player), player.getUUID(),
                event.getRawText(), GrimoireSavedData.get(level.getServer()).of(player.getUUID()));
    }

    /**
     * The block this player is looking at, or their own position if they are looking at
     * nothing within {@link #GAZE}.
     *
     * Falling back to the caster rather than refusing is what makes casting into open sky
     * behave like casting at your own feet: something happens, where you are, and you can
     * see that it did. A refusal would leave a player who mis-aimed unable to tell whether
     * the spell failed or the word was wrong.
     */
    private static BlockPos gazeTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(GAZE));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? player.blockPosition() : hit.getBlockPos();
    }
}
