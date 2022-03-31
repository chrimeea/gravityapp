package com.prozium.gravityapp.level.block;

import android.util.Log;

import com.prozium.gravityapp.level.GravityCamera;
import com.prozium.gravityapp.level.GravityGrid;
import com.prozium.gravityapp.level.GravityStage;
import com.prozium.gravityapp.util.GravityVector3D;

/**
 * Created by cristian on 06.04.2017.
 */

public class GravityBlockPerson extends GravityBlock {

    boolean doll = false;

    public GravityBlockPerson(final GravityGrid grid, final float mass, final int modelId) {
        super(grid, mass, modelId);
        isTransient = true;
        teenFactor = grid.mediator.r.nextFloat();
    }

    public void step(final GravityCamera camera) {
        if (!doll && !forces.isAlmostZero()) {
            doll = true;
        } else if (doll && forces.length() < GRAVITY) {
            grid.mediator.blockRemoved(this);
            grid.mediator.personKilled();
            return;
        }
        if (doll) {
            super.step(camera);
        } else {
            final GravityVector3D temp = new GravityVector3D(position).add(grid.center).substract(camera.camera);
            temp.v[1] = 0f;
            position.add(temp.normalize(0.01f + grid.mediator.r.nextFloat() * 0.01f));
        }
    }

    public void applyPending() {
        if (doll) {
            super.applyPending();
        }
    }

    public void isHit() {
        if (!doll) {
            instantForces.reset();
            pendingForces.reset();
            pendingAngularSpeed.reset();
        }
    }

    public void collide(final GravityBlock b, final GravityVector3D normal) {
        if (doll) {
            super.collide(b, normal);
        }
    }

    public static GravityBlockPerson makePerson(final GravityBlock block) {
        final GravityBlockPerson person = new GravityBlockPerson(block.grid, 1f, 2);
        person.position.v[0] = block.position.v[0];
        person.position.v[1] = person.shape.side + 10f / GravityVector3D.ERROR;
        person.position.v[2] = block.position.v[2];
        return person;
    }
}
