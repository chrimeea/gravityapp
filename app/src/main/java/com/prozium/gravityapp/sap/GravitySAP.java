package com.prozium.gravityapp.sap;

import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.level.block.GravityBlockCollision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravitySAP {
    public final List<GravityEndPoint>[] axis = new List[] {
            new ArrayList<GravityEndPoint>(),
            new ArrayList<GravityEndPoint>(),
            new ArrayList<GravityEndPoint>()};
    public final long id;

    public GravitySAP(final long id) {
        this.id = id;
    }

    public boolean isEmpty() {
        return axis[0].isEmpty();
    }

    void insertionSortUpdate(final int axisIndex, final GravityEndPoint bi, final Map<Long, GravityBlockCollision> collisions) {
        bi.updatePosition(axisIndex);
        int i = bi.order, j = i + 1;
        long k;
        GravityEndPoint bj;
        if (j < axis[axisIndex].size() && bi.position > axis[axisIndex].get(j).position) {
            bj = axis[axisIndex].get(j);
            while (bi.position > bj.position) {
                if (bi.block != bj.block) {
                    if (bi instanceof GravityMaxEndPoint && bj instanceof GravityMinEndPoint) {
                        if (bi.block.isFixed || bj.block.isFixed || bi.block.shape.axisIntersects(bj.block.shape)) {
                            k = bi.block.pairKey(bj.block);
                            if (!collisions.containsKey(k)) {
                                collisions.put(k, new GravityBlockCollision(
                                        bi.block.isFixed ? bi.block.fixedBlock(bj.block) : bi.block,
                                        bj.block.isFixed ? bj.block.fixedBlock(bi.block) : bj.block));
                            }
                        }
                    } else if (bi instanceof GravityMinEndPoint && bj instanceof GravityMaxEndPoint) {
                        collisions.remove(bi.block.pairKey(bj.block));
                    }
                }
                axis[axisIndex].set(j - 1, bj);
                bj.order = j - 1;
                j++;
                if (j >= axis[axisIndex].size()) {
                    break;
                } else {
                    bj = axis[axisIndex].get(j);
                }
            }
            axis[axisIndex].set(j - 1, bi);
            bi.order = j - 1;
        } else if (i > 0) {
            j = i - 1;
            bj = axis[axisIndex].get(j);
            while (bi.position < bj.position) {
                if (bi instanceof GravityMinEndPoint && bj instanceof GravityMaxEndPoint) {
                    if (bi.block.isFixed || bj.block.isFixed || bi.block.shape.axisIntersects(bj.block.shape)) {
                        k = bi.block.pairKey(bj.block);
                        if (!collisions.containsKey(k)) {
                            collisions.put(k, new GravityBlockCollision(
                                    bi.block.isFixed ? bi.block.fixedBlock(bj.block) : bi.block,
                                    bj.block.isFixed ? bj.block.fixedBlock(bi.block) : bj.block));
                        }
                    }
                } else if (bi instanceof GravityMaxEndPoint && bj instanceof GravityMinEndPoint) {
                    collisions.remove(bi.block.pairKey(bj.block));
                }
                axis[axisIndex].set(j + 1, bj);
                bj.order = j + 1;
                j--;
                if (j < 0) {
                    break;
                } else {
                    bj = axis[axisIndex].get(j);
                }
            }
            axis[axisIndex].set(j + 1, bi);
            bi.order = j + 1;
        }
    }

    void insertionSortAddNoCollision(final int axisIndex, final GravityEndPoint bi) {
        bi.updatePosition(axisIndex);
        final int t = axis[axisIndex].size();
        for (int i = bi.order; i < t; i++) {
            if (bi.position < axis[axisIndex].get(i).position) {
                axis[axisIndex].add(null);
                for (int j = t - 1; j >= i; j--) {
                    final GravityEndPoint e = axis[axisIndex].get(j);
                    axis[axisIndex].set(j + 1, e);
                    e.order = j + 1;
                }
                axis[axisIndex].set(i, bi);
                bi.order = i;
                return;
            }
        }
        axis[axisIndex].add(bi);
        bi.order = t;
    }

    void addBlockToSAPNoCollision(final GravityBlock b) {
        final GravitySAPBox box = new GravitySAPBox(this);
        for (int i = 0; i < 3; i++) {
            box.minEndPoint[i] = new GravityMinEndPoint(b, 1);
            insertionSortAddNoCollision(i, box.minEndPoint[i]);
            box.maxEndPoint[i] = new GravityMaxEndPoint(b, box.minEndPoint[i].order + 1);
            insertionSortAddNoCollision(i, box.maxEndPoint[i]);
        }
        b.boxes.add(box);
    }

    void addBlockToSAP(final GravityBlock b, final Map<Long, GravityBlockCollision> collisions) {
        final GravitySAPBox box = new GravitySAPBox(this);
        box.minEndPoint[0] = new GravityMinEndPoint(b, 1);
        insertionSortAddNoCollision(0, box.minEndPoint[0]);
        box.maxEndPoint[0] = new GravityMaxEndPoint(b, box.minEndPoint[0].order + 1);
        insertionSortAddNoCollision(0, box.maxEndPoint[0]);
        box.minEndPoint[1] = new GravityMinEndPoint(b, 1);
        insertionSortAddNoCollision(1, box.minEndPoint[1]);
        box.maxEndPoint[1] = new GravityMaxEndPoint(b, box.minEndPoint[1].order + 1);
        insertionSortAddNoCollision(1, box.maxEndPoint[1]);
        box.minEndPoint[2] = new GravityMinEndPoint(b, axis[2].size());
        axis[2].add(box.minEndPoint[2]);
        insertionSortUpdate(2, box.minEndPoint[2], collisions);
        box.maxEndPoint[2] = new GravityMaxEndPoint(b, axis[2].size());
        axis[2].add(box.maxEndPoint[2]);
        insertionSortUpdate(2, box.maxEndPoint[2], collisions);
        b.boxes.add(box);
    }
}
