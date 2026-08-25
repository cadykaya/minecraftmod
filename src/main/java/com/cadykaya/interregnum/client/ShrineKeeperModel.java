package com.cadykaya.interregnum.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * How the keeper moves.
 *
 * The opposite of the Warden's motion, on purpose. A Warden's head leads and its
 * body follows late, because it has noticed you and has not decided anything yet.
 * The keeper's head turns LESS -- they look up from the ledger, briefly, and go back
 * to it. Someone with work to do.
 *
 * The arms swing in opposition like a person walking, where the Warden's move
 * together like something being carried forward. That one difference does most of
 * the species work at any distance where the paint has stopped mattering.
 */
public class ShrineKeeperModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public ShrineKeeperModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Clamped tighter than the Warden's. They glance; they do not stare.
        head.yRot = Mth.clamp(state.yRot * Mth.DEG_TO_RAD, -0.8F, 0.8F);
        head.xRot = Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -0.4F, 0.4F);

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        // Opposed, and a person's amplitude rather than a monument's.
        rightArm.xRot = Mth.cos(pos * 0.7F) * 0.9F * speed;
        leftArm.xRot = Mth.cos(pos * 0.7F + (float) Math.PI) * 0.9F * speed;
    }
}
