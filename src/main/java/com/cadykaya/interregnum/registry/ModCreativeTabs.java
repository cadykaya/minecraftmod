package com.cadykaya.interregnum.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cadykaya.interregnum.Interregnum;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Interregnum.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.interregnum.main"))
                    .icon(() -> ModItems.CLAST.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.SHRINE_STONE.get());
                        output.accept(ModBlocks.SHRINE_STONE_CARVED.get());
                        output.accept(ModItems.GOD_HEART.get());
                        output.accept(ModItems.CLAST.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
