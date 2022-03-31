package com.prozium.gravityapp.sap;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravitySAPBox {

    public final GravityEndPoint[] minEndPoint = new GravityMinEndPoint[3], maxEndPoint = new GravityMaxEndPoint[3];
    public GravitySAP sap;

    public GravitySAPBox(final GravitySAP sap) {
        this.sap = sap;
    }
}
