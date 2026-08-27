package com.cadykaya.interregnum.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.entity.ShrineKeeperEntity;

/** Drawing the keeper. Same shape as the Warden's renderer; different everything else. */
public class ShrineKeeperRenderer
        extends MobRenderer<ShrineKeeperEntity, LivingEntityRenderState, ShrineKeeperModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "shrine_keeper"), "main");

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Interregnum.MOD_ID, "textures/entity/shrine_keeper.png");

    public ShrineKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new ShrineKeeperModel(context.bakeLayer(LAYER)), 0.4F);
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
