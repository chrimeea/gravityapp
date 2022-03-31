package com.prozium.gravityapp.level.block;

import com.prozium.gravityapp.util.GravityVector3D;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravityBlockCollision {

    public GravityBlock block1, block2;

    public GravityBlockCollision(final GravityBlock a, final GravityBlock b) {
        block1 = a;
        block2 = b;
    }

    public float collide() {
        final GravityVector3D normal = new GravityVector3D(block2.position).substract(block1.position).normalize();
        final GravityVector3D temp = new GravityVector3D(normal).multiply(-1f);
        final float fa = block1.forces.dotProduct(normal);
        final float fb = block2.forces.dotProduct(temp);
        if ((fa < 0f && fb < 0f) || (fa >= 0f && fb < 0f && fa < -fb) || (fa < 0f && fb >= 0f && fb < -fa)) {
            return 0f;
        }
        if (fa < fb || (fa == fb && block1.stack != null)) {
            block2.collide(block1, temp);
            block1.isHit();
        } else {
            block1.collide(block2, normal);
            block2.isHit();
        }
        return fa + fb;
    }
}
