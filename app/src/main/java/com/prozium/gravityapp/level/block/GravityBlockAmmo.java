package com.prozium.gravityapp.level.block;

import com.prozium.gravityapp.level.GravityCamera;
import com.prozium.gravityapp.level.GravityGrid;
import com.prozium.gravityapp.level.GravityStage;
import com.prozium.gravityapp.util.GravityVector3D;

import java.util.Random;

/**
 * Created by cristian on 05.04.2017.
 */

public class GravityBlockAmmo extends GravityBlock {

    public float range;

    public GravityBlockAmmo(final GravityGrid grid, final float mass, final int modelId, final float range) {
        super(grid, mass, modelId);
        this.range = range;
        isTransient = true;
    }

    public void collide(final GravityBlock b, final GravityVector3D normal) {
        assert (stack == null);
        if (range > 0f) {
            //TODO: use sap to find objects in range
            final GravityVector3D temp = new GravityVector3D();
            for (GravityBlock block: grid.objects) {
                temp.resetTo(block.position).substract(position);
                if (temp.length() < range) {
                    block.forces.add(temp.normalize(0.1f));
                    if (block.stack != null) {
                        if (block.stack.isEmpty()) {
                            block.forces.reset();
                        } else {
                            if (block.stack.size() > 3) {
                                grid.scoreCubes(block.stack.size());
                            }
                            for (GravityBlock l : block.stack) {
                                grid.mediator.blockBecomesActive(l);
                            }
                        }
                    }
                }
            }
        } else if (!(b instanceof  GravityBlockAmmo)) {
            super.collide(b, normal);
        }
        isAlive = false;
    }

    public void onRemove(final GravityStage stage) {
        if (range > 0f) {
            stage.detonateCenter = new GravityVector3D(position).add(grid.center);
            stage.detonateScale = range;
            stage.soundPool.play(stage.bombId, 1f, 1f, 20, 0, 1f);
        } else {
            stage.ammo[0]++;
        }
    }

    public static GravityBlockAmmo makeAmmo(final GravityCamera camera, final Random r, final float range) {
        final GravityBlockAmmo b = new GravityBlockAmmo(camera.cameraLocation, 0.5f, 0, range);
        b.position.resetTo(camera.cameraFront).normalize(3.0f).add(camera.camera).substract(b.grid.center);
        b.forces.resetTo(camera.cameraFront).add(
                new GravityVector3D(new float[]{(r.nextFloat() - 0.5f) * 2f, 0f, 0f})).normalize(0.05f);
        return b;
    }
}
