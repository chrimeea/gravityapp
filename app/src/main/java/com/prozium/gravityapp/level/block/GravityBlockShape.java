package com.prozium.gravityapp.level.block;

import com.prozium.gravityapp.sap.GravityMultiSAP;
import com.prozium.gravityapp.util.GravityVector3D;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravityBlockShape {

    public float side;
    public GravityVector3D position, angle;

    public GravityBlockShape(final GravityVector3D position, final GravityVector3D angle, final float side) {
        assert(side < GravityMultiSAP.SAP_GRID_SIZE);
        this.position = position;
        this.angle = angle;
        this.side = side;
    }

    //TODO: more accurate intersection based on shape, use gjk simplex
    float penetration(final GravityBlockShape b) {
        return side + b.side - position.distance(b.position);
    }

    //TODO: more accurate intersection based on shape, use gjk simplex
    public boolean exactIntersects(final GravityBlockShape b) {
        return penetration(b) > 0f;
    }

    //TODO: more accurate intersection based on shape, use gjk simplex
    static GravityVector3D intersection(final GravityBlockShape a, final GravityBlockShape b) {
        return new GravityVector3D(a.position)
                .multiply(a.side / (a.side + b.side))
                .add(new GravityVector3D(b.position).multiply(b.side / (a.side + b.side)));
    }

    //TODO: include rotation
    public float getMinEndpointPosition(final int axis) {
        return position.v[axis] - side;
    }

    //TODO: include rotation
    public float getMaxEndpointPosition(final int axis) {
        return position.v[axis] + side;
    }

    public boolean axisIntersects(final GravityBlockShape b, final int axis) {
        return getMaxEndpointPosition(axis) >= b.getMinEndpointPosition(axis) && b.getMaxEndpointPosition(axis) >= getMinEndpointPosition(axis);
    }

    public boolean axisIntersects(final GravityBlockShape b) {
        return axisIntersects(b, 0) && axisIntersects(b, 1) && axisIntersects(b, 2);
    }
}
