package com.prozium.gravityapp.sap;

import com.prozium.gravityapp.level.block.GravityBlock;

/**
 * Created by cristian on 09.05.2016.
 */
public class GravityMaxEndPoint extends GravityEndPoint {

    GravityMaxEndPoint(final GravityBlock block, final int order) {
        super(block, order);
    }

    void updatePosition(final int axis) {
        position = block.shape.getMaxEndpointPosition(axis);
    }
}
