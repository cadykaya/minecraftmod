package com.cadykaya.interregnum;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.registry.ModCreativeTabs;
import com.cadykaya.interregnum.registry.ModItems;

/**
 * Entrypoint. Registration wiring only -- no game logic lives here, per
 * docs/ARCHITECTURE.md. Nothing in this class or anything it reaches may touch a
 * client-only type; tools/client_leak_check.py enforces that.
 */
@Mod(Interregnum.MOD_ID)
public class Interregnum {
    public static final String MOD_ID = "interregnum";

    public Interregnum(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
    }
}
