package com.cadykaya.interregnum.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * How a Warden moves.
 *
 * The geometry is generated -- see {@link WardenGeometry} -- because three things
 * need the same box list and they fail silently against each other. What is here is
 * the part that is design rather than data: the motion.
 *
 * Two rules, and both are the dread covenant expressed in radians.
 *
 * **The head leads and the body follows late.** A Warden turns its head to you
 * before its body knows, and the mantle swings with it. That lag is most of the
 * menace: it is the movement of something that has noticed you and has not yet
 * decided to do anything about it.
 *
 * **The arms barely swing.** A quarter of the amplitude a walking humanoid gets.
 * Wardens do not stride, they arrive; big arm swing reads as effort, and nothing
 * about this should look like effort.
 */
public class WardenModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart robeLower;

    public WardenModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.robeLower = root.getChild("robe_lower");
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Clamped hard on purpose. An unclamped head follows a player standing on
        // top of it straight through the shoulders, and a Warden whose head has
        // rotated inside its own body is funny -- which is the one thing they may
        // never be.
        head.xRot = Mth.clamp(head.xRot, -0.5F, 0.5F);
        head.yRot = Mth.clamp(head.yRot, -1.1F, 1.1F);

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);

        // A quarter of a humanoid's swing, and the arms move together rather than
        // in opposition: this is a figure being carried forward, not one walking.
        float swing = Mth.cos(pos * 0.6F) * 0.25F * speed;
        rightArm.xRot = swing;
        leftArm.xRot = swing;

        // The robe's lower step lags the walk by half a phase, so the hem trails.
        // Only the bottom box moves; the upper step stays with the body, which is
        // what makes it read as cloth over a frame rather than as a swinging bell.
        robeLower.zRot = Mth.cos(pos * 0.3F) * 0.04F * speed;
    }
}
