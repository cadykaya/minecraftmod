package com.cadykaya.interregnum.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.system.attrition.Tended;
import com.cadykaya.interregnum.system.claim.PlacedBlocks;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Interregnum.MOD_ID);

    /** Attached to chunks, saved with them. See PlacedBlocks for the design. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlacedBlocks>> PLACED_BLOCKS =
            ATTACHMENTS.register("placed_blocks",
                    () -> AttachmentType.serializable(PlacedBlocks::new).build());

    /**
     * When anybody was last near this chunk. Attached to chunks, saved with them.
     * See {@link com.cadykaya.interregnum.system.attrition.Tended}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Tended>> TENDED =
            ATTACHMENTS.register("tended",
                    () -> AttachmentType.serializable(Tended::new).build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
