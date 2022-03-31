package com.prozium.gravityapp.level.block;

import android.util.Log;

import com.prozium.gravityapp.level.GravityCamera;
import com.prozium.gravityapp.level.GravityGrid;
import com.prozium.gravityapp.level.GravityStage;
import com.prozium.gravityapp.sap.GravitySAPBox;
import com.prozium.gravityapp.util.GravityVector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 15.03.2016.
 */
public class GravityBlock {

    static final float MAX_FORCE = 0.1f;
    public static final float SCALE = 0.05f;
    static final float RESTITUTION = 1.0f;
    static final float ATTENUATION = 0.7f;
    static final float GRAVITY = 0.001f;
    static final GravityVector3D BACKGROUND = new GravityVector3D(new float[] {0f, -GRAVITY, 0f});
    static final GravityVector3D BACKGROUND_NORMALIZED = new GravityVector3D(BACKGROUND).normalize();
    static int idgen = 0;
    public final GravityVector3D forces = new GravityVector3D();
    public final GravityVector3D angularSpeed = new GravityVector3D();
    public final GravityVector3D position = new GravityVector3D();
    public final GravityVector3D angle = new GravityVector3D();
    public final GravityVector3D pendingForces = new GravityVector3D();
    public final GravityVector3D pendingAngularSpeed = new GravityVector3D();
    public final GravityVector3D instantForces = new GravityVector3D();
    final float mass;
    final int id;
    public int vertexOffset, modelId;
    public boolean isFixed, isTransient, isAlive = true;
    public List<GravityBlock> stack;
    public GravityGrid grid;
    public GravityBlockShape shape;
    public final List<GravitySAPBox> boxes = new ArrayList<>(4);
    public float teenFactor;
    int frames = 1, currentFrame;

    public GravityBlock(final GravityGrid grid, final float mass, final int modelId) {
        id = idgen++;
        this.grid = grid;
        this.mass = mass;
        this.modelId = modelId;
        shape = new GravityBlockShape(position, angle, SCALE * mass);
    }

    public void applyPending() {
        forces.resetTo(pendingForces);
        if (stack == null) {
            forces.substract(new GravityVector3D(GravityBlock.BACKGROUND).multiply(mass));
        }
        angularSpeed.resetTo(pendingAngularSpeed);
        pendingForces.reset();
        pendingAngularSpeed.reset();
        position.add(instantForces);
        instantForces.reset();
    }

    public void animate() {
        assert currentFrame < frames;
        if (frames > 1) {
            teenFactor += 0.1f;
            if (teenFactor >= 1f) {
                teenFactor = 0f;
                currentFrame++;
                modelId++;
                if (currentFrame == frames) {
                    currentFrame = 0;
                    modelId -= frames;
                }
            }
        }
    }

    public long pairKey(final GravityBlock b) {
        return Math.min(id, b.id) * 15485863 + Math.max(id, b.id);
    }

    //TODO: not needed if the fixed object is one very large object
    public GravityBlock fixedBlock(final GravityBlock block) {
        final GravityBlock b = new GravityBlock(block.grid, Math.max(block.mass, 2f), -1);
        b.isFixed = true;
        b.stack = new ArrayList<>();
        b.position.v[0] = block.position.v[0];
        b.position.v[1] = -shape.side;
        b.position.v[2] = block.position.v[2];
        return b;
    }

    public boolean isRemoved() {
        return !grid.ready;
    }

    GravityVector3D inertiaTensor() {
        final float i = 1f / (10f * (float) Math.pow(mass, 2.0) * SCALE);
        return new GravityVector3D(new float[] {i, i, i});
    }

    static GravityVector3D maxCombineToSelf(final GravityVector3D v1, final GravityVector3D v2, final GravityVector3D v3) {
        final float lv1 = v1.length();
        final float lv2 = v2.length();
        if (lv2 > 0f) {
            if (GravityVector3D.isAlmostZero(lv1)) {
                v1.resetTo(v3);
            }
            return v1.normalize().add(new GravityVector3D(v2).normalize()).normalize(Math.max(lv1, lv2));
        } else {
            return v1;
        }
    }

    void untangle(final GravityBlock b) {
        maxCombineToSelf(instantForces,
                new GravityVector3D(position).substract(b.position).normalize(shape.penetration(b.shape) + 10f / GravityVector3D.ERROR),
                instantForces);
    }

    public void isHit() {}

    public void collide(final GravityBlock b, final GravityVector3D normal) {
        assert (stack == null);
        final GravityVector3D temp = new GravityVector3D();
        untangle(b);
        final GravityVector3D cb = GravityBlockShape.intersection(shape, b.shape);
        final GravityVector3D ca = new GravityVector3D(cb).substract(position);
        cb.substract(b.position);
        final GravityVector3D angularVelChangea = normal.crossProductNewVector(ca).multiply(inertiaTensor());
        final GravityVector3D angularVelChangeb = normal.crossProductNewVector(cb).multiply(b.inertiaTensor());
        normal.multiply(
                (RESTITUTION + 1f)
                        * temp.resetTo(forces).substract(b.forces).length()
                        / (1f / mass
                        + angularVelChangea.crossProductNewVector(ca).dotProduct(normal)
                        + 1f / b.mass
                        + angularVelChangeb.crossProductNewVector(cb).dotProduct(normal)));
        maxCombineToSelf(pendingForces, temp.resetTo(forces).substract(new GravityVector3D(normal).multiply(1f / mass)).multiply(ATTENUATION), instantForces);
        maxCombineToSelf(pendingAngularSpeed, temp.resetTo(angularSpeed).substract(angularVelChangea), pendingAngularSpeed);
        maxCombineToSelf(b.pendingForces, temp.resetTo(b.forces).add(new GravityVector3D(normal).multiply(1f / b.mass)).multiply(ATTENUATION), b.instantForces);
        maxCombineToSelf(b.pendingAngularSpeed, temp.resetTo(b.angularSpeed).add(angularVelChangeb), b.pendingAngularSpeed);
        if (b.stack != null) {
            if (pendingForces.isAlmostZero()
                    && pendingAngularSpeed.isAlmostZero()
                    && temp.resetTo(b.pendingForces).normalize().substract(BACKGROUND_NORMALIZED).isAlmostZero()
                    && b.pendingAngularSpeed.isAlmostZero()) {
                b.pendingForces.reset();
                stack = b.stack;
                stack.add(this);
                grid.mediator.blockBecomesInactive(this);
            } else {
                final List<GravityBlock> s = b.stack;
                int t = s.size();
                grid.mediator.buildingCollapse(b);
                for (int i = 0; i < t; i++) {
                    final GravityBlock block = s.get(i);
                    if (block.position.v[1] >= b.position.v[1]) {
                        s.remove(i);
                        i--;
                        t--;
                        grid.mediator.blockBecomesActive(block);
                    }
                }
            }
        }
    }

    public void onRemove(final GravityStage stage) {}

    public void step(final GravityCamera camera) {
        forces.add(new GravityVector3D(GravityBlock.BACKGROUND).multiply(mass)).limit(MAX_FORCE);
        //TODO: temporary fix for blocks below ground
        if (forces.v[1] + position.v[1] < 0f) {
            forces.v[1] = 0f;
        }
        position.add(forces);
        angle.add(angularSpeed);
    }
}
