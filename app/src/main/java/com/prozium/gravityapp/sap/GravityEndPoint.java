package com.prozium.gravityapp.sap;

import com.prozium.gravityapp.level.block.GravityBlock;

/**
 * Created by cristian on 19.04.2016.
 */
public abstract class GravityEndPoint {

    public GravityBlock block;
    public float position;
    public int order;

    GravityEndPoint(final GravityBlock block, final int order) {
        this.block = block;
        this.order = order;
    }

    abstract void updatePosition(final int axis);
}
