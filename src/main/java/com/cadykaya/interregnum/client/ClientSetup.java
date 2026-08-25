package com.cadykaya.interregnum.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.registry.ModEntities;

/**
 * Everything the client, and only the client, has to be told.
 *
 * Both halves below are mandatory and neither fails at startup if forgotten: a
 * renderer with no registered layer definition throws when the first entity of that
 * type comes into view, and an entity type with no renderer at all does the same.
 * Both are therefore invisible until a player is standing in front of one, which is
 * the worst possible time to find out. tools/registry_check.py asserts the pairing
 * statically instead.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WardenRenderer.LAYER, WardenGeometry::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WARDEN.get(), WardenRenderer::new);
    }
}
