package com.prozium.gravityapp.sap;

import com.prozium.gravityapp.level.block.GravityBlock;

/**
 * Created by cristian on 09.05.2016.
 */
public class GravityMinEndPoint extends GravityEndPoint {

    GravityMinEndPoint(final GravityBlock block, final int order) {
        super(block, order);
    }

    void updatePosition(final int axis) {
        position =  block.shape.getMinEndpointPosition(axis);
    }
}
