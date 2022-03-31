package com.prozium.gravityapp.sap;

import android.util.Log;

import com.prozium.gravityapp.level.GravityMediator;
import com.prozium.gravityapp.level.GravityStage;
import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.level.block.GravityBlockCollision;
import com.prozium.gravityapp.level.GravityGrid;
import com.prozium.gravityapp.util.GravityVector3D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravityMultiSAP {

    public static final float SAP_GRID_SIZE = 50f;
    public static final GravityBlock ground = new GravityBlock(null, 2f, 0);
    final Map<Long, GravitySAP> blocks = new HashMap<>();
    public final Map<Long, GravityBlockCollision> collisions = new HashMap<Long, GravityBlockCollision>();
    final GravityMediator mediator;

    static {
        ground.isFixed = true;
        ground.position.v[1] = -ground.shape.side;
    }

    public GravityMultiSAP(final GravityMediator mediator) {
        this.mediator = mediator;
    }

    GravitySAP getSAP(final GravityVector3D center, final float x, final float y) {
        final long key = (long) (GravityGrid.generateKey(center) * Math.pow(GravityGrid.GRID_SIZE, 2.0) / Math.pow(SAP_GRID_SIZE, 2.0))
                + (long) (Math.floor((x + GravityGrid.GRID_SIZE / 2f) / SAP_GRID_SIZE) * Math.floor(GravityGrid.GRID_SIZE / SAP_GRID_SIZE) + Math.floor((y + GravityGrid.GRID_SIZE / 2f) / SAP_GRID_SIZE));
        if (!blocks.containsKey(key)) {
            final GravitySAP l = new GravitySAP(key);
            blocks.put(key, l);
            l.axis[1].add(new GravityMaxEndPoint(ground, 0));
            return l;
        } else {
            return blocks.get(key);
        }
    }

    Collection<GravitySAP> getSAPsForBlock(final GravityBlock b) {
        final Collection<GravitySAP> s = new ArrayList<>(4);
        final GravitySAP axis1 = getSAP(b.grid.center, b.shape.getMinEndpointPosition(0), b.shape.getMinEndpointPosition(2));
        final GravitySAP axis2 = getSAP(b.grid.center, b.shape.getMaxEndpointPosition(0), b.shape.getMinEndpointPosition(2));
        final GravitySAP axis3 = getSAP(b.grid.center, b.shape.getMinEndpointPosition(0), b.shape.getMaxEndpointPosition(2));
        s.add(axis1);
        if (axis1 != axis2) {
            s.add(axis2);
            if (axis1 != axis3) {
                s.add(axis3);
                s.add(getSAP(b.grid.center, b.shape.getMaxEndpointPosition(0), b.shape.getMaxEndpointPosition(2)));
            }
        } else if (axis1 != axis3) {
            s.add(axis3);
        }
        assert(s.contains(getSAP(b.grid.center, b.shape.getMinEndpointPosition(0), b.shape.getMinEndpointPosition(2))));
        assert(s.contains(getSAP(b.grid.center, b.shape.getMaxEndpointPosition(0), b.shape.getMinEndpointPosition(2))));
        assert(s.contains(getSAP(b.grid.center, b.shape.getMinEndpointPosition(0), b.shape.getMaxEndpointPosition(2))));
        assert(s.contains(getSAP(b.grid.center, b.shape.getMaxEndpointPosition(0), b.shape.getMaxEndpointPosition(2))));
        assert(s.contains(getSAP(b.grid.center, b.shape.getMinEndpointPosition(0), b.position.v[2])));
        assert(s.contains(getSAP(b.grid.center, b.shape.getMaxEndpointPosition(0), b.position.v[2])));
        assert(s.contains(getSAP(b.grid.center, b.position.v[0], b.shape.getMinEndpointPosition(2))));
        assert(s.contains(getSAP(b.grid.center, b.position.v[0], b.shape.getMaxEndpointPosition(2))));
        return s;
    }

    public void updateBlock(final GravityBlock b) {
        final Collection<GravitySAP> s = getSAPsForBlock(b);
        int t = b.boxes.size();
        for (int i = 0; i < t; i++) {
            final GravitySAPBox box = b.boxes.get(i);
            if (s.remove(box.sap)) {
                updateBox(box);
            } else {
                removeBox(box);
                b.boxes.remove(i);
                i--;
                t--;
            }
        }
        for (GravitySAP temp: s) {
            temp.addBlockToSAP(b, collisions);
        }
    }

    public void addBlock(final GravityBlock b) {
        for (GravitySAP temp: getSAPsForBlock(b)) {
            temp.addBlockToSAPNoCollision(b);
        }
    }

    public void removeBlock(final GravityBlock block) {
        for (GravitySAPBox temp: block.boxes) {
            removeBox(temp);
        }
        block.boxes.clear();
    }

    void updateBox(final GravitySAPBox box) {
        for (int j = 0; j < 3; j++) {
            box.sap.insertionSortUpdate(j, box.minEndPoint[j], collisions);
            box.sap.insertionSortUpdate(j, box.maxEndPoint[j], collisions);
        }
    }

    void removeBox(final GravitySAPBox box) {
        for (int j = 0; j < 3; j++) {
            removeEndPointFromAxis(box.maxEndPoint[j].order, box.sap.axis[j]);
            removeEndPointFromAxis(box.minEndPoint[j].order, box.sap.axis[j]);
        }
        if (box.sap.isEmpty()) {
            blocks.remove(box.sap.id);
        }
    }

    static void removeEndPointFromAxis(final int order, final List<GravityEndPoint> l) {
        final int t = l.size();
        for (int i = order + 1; i < t; i++) {
            final GravityEndPoint e = l.get(i);
            l.set(i - 1, e);
            e.order = i - 1;
        }
        l.remove(t - 1);
    }

    public int loopCollisions() {
        final Collection<GravityBlock> pending = new HashSet<GravityBlock>();
        final Iterator<GravityBlockCollision> j = collisions.values().iterator();
        int s = 0;
        while (j.hasNext()) {
            final GravityBlockCollision pair = j.next();
            if (pair.block1.isRemoved() || pair.block2.isRemoved()) {
                j.remove();
            } else if (pair.block1.shape.exactIntersects(pair.block2.shape)) {
                final int cb = pair.block1.grid.cubes;
                mediator.onCollision(pair.collide());
                if (pair.block1.grid.cubes - cb > 0) {
                    s += pair.block1.grid.cubes - cb;
                }
                if (!pair.block1.isAlive) {
                    mediator.blockRemoved(pair.block1);
                } else if (!pair.block1.isFixed) {
                    pending.add(pair.block1);
                }
                if (!pair.block2.isAlive) {
                    mediator.blockRemoved(pair.block2);
                } else if (!pair.block2.isFixed) {
                    pending.add(pair.block2);
                }
                if (!pair.block1.isAlive || !pair.block2.isAlive) {
                    j.remove();
                }
            }
        }
        for (GravityBlock b: pending) {
            if (!b.isRemoved()) {
                b.applyPending();
                updateBlock(b);
            }
        }
        return s;
    }
}
