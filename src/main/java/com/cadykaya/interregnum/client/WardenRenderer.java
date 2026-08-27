package com.cadykaya.interregnum.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.entity.WardenEntity;

/**
 * Drawing a Warden.
 *
 * A plain {@link LivingEntityRenderState}: everything the model needs is already on
 * it (look angles, walk phase), so there is no custom state to extract and nothing
 * to keep in sync. The moment a Warden carries something the renderer must know
 * about -- a raised ledger, a citation in progress -- this grows a render state of
 * its own, and not before.
 */
public class WardenRenderer extends MobRenderer<WardenEntity, LivingEntityRenderState, WardenModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "warden"), "main");

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "textures/entity/warden.png");

    public WardenRenderer(EntityRendererProvider.Context context) {
        // 0.6 shadow: slightly under the robe's footprint, so the figure reads as
        // standing on the ground rather than hovering over its own shadow.
        super(context, new WardenModel(context.bakeLayer(LAYER)), 0.6F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
