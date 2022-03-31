package com.prozium.gravityapp.util;

/**
 * Created by cristian on 15.03.2016.
 */
public class GravityVector3D {

    public static final float ERROR = 10000f;
    public float[] v;

    public GravityVector3D() {
        v = new float[3];
    }

    public GravityVector3D(final GravityVector3D v2) {
        v = new float[3];
        resetTo(v2);
    }

    public GravityVector3D(final float[] v2) {
        v = v2;
    }

    public void reset() {
        v[0] = v[1] = v[2] = 0f;
    }

    public GravityVector3D resetTo(final GravityVector3D v2) {
        v[0] = v2.v[0];
        v[1] = v2.v[1];
        v[2] = v2.v[2];
        return this;
    }

    public GravityVector3D add(final GravityVector3D v2) {
        v[0] += v2.v[0];
        v[1] += v2.v[1];
        v[2] += v2.v[2];
        return this;
    }

    public GravityVector3D substract(final GravityVector3D v2) {
        v[0] -= v2.v[0];
        v[1] -= v2.v[1];
        v[2] -= v2.v[2];
        return this;
    }

    public GravityVector3D multiply(final float n) {
        v[0] *= n;
        v[1] *= n;
        v[2] *= n;
        return this;
    }

    public GravityVector3D multiply(final GravityVector3D v2) {
        v[0] *= v2.v[0];
        v[1] *= v2.v[1];
        v[2] *= v2.v[2];
        return this;
    }

    public float dotProduct(final GravityVector3D v2) {
        return v[0] * v2.v[0] + v[1] * v2.v[1] + v[2] * v2.v[2];
    }

    public GravityVector3D crossProductNewVector(final GravityVector3D v2) {
        final GravityVector3D v3 = new GravityVector3D();
        v3.v[0] = v[1] * v2.v[2] - v[2] * v2.v[1];
        v3.v[1] = v[2] * v2.v[0] - v[0] * v2.v[2];
        v3.v[2] = v[0] * v2.v[1] - v[1] * v2.v[0];
        return v3;
    }

    public float length() {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    public float distance(final GravityVector3D v2) {
        return (float) Math.sqrt(Math.pow(v2.v[0] - v[0], 2.0) + Math.pow(v2.v[1] - v[1], 2.0) + Math.pow(v2.v[2] - v[2], 2.0));
    }

    public GravityVector3D normalize() {
        return normalize(1f);
    }

    public GravityVector3D normalize(final float n) {
        final float l = length();
        return l > 0f ? multiply(n / l) : this;
    }

    public GravityVector3D limit(final float l) {
        final float t = length();
        if (t > l) {
            multiply(l / t);
        }
        return this;
    }

    public static boolean isAlmostZero(final float v) {
        assert(v >= 0f);
        return v * ERROR < 1f;
    }

    public boolean isAlmostZero() {
        return GravityVector3D.isAlmostZero(length());
    }

    public float angle(final GravityVector3D v2) {
        final float l = length() * v2.length();
        return l == 0f ? 0f : (float) Math.acos(dotProduct(v2) / l);
    }

    public GravityVector3D rotate(final GravityVector3D angle) {
        final GravityVector3D temp = new GravityVector3D();
        temp.v[0] = 1f;
        rotateByAxis(temp, angle.v[0]);
        temp.v[0] = 0f;
        temp.v[1] = 1f;
        rotateByAxis(temp, angle.v[1]);
        temp.v[1] = 0f;
        temp.v[2] = 1f;
        rotateByAxis(temp, angle.v[2]);
        return this;
    }

    GravityVector3D rotateByAxis(final GravityVector3D axis, float angle) {
        if (angle != 0f) {
            final GravityVector3D temp = new GravityVector3D(this);
            angle *= Math.PI / 180f;
            v[0] = axis.v[0] * (axis.v[0] * temp.v[0] + axis.v[1] * temp.v[1] + axis.v[2] * temp.v[2]) * (1f - (float) Math.cos(angle))
                    + temp.v[0] * (float) Math.cos(angle)
                    + (-axis.v[2] * temp.v[1] + axis.v[1] * temp.v[2]) * (float) Math.sin(angle);
            v[1] = axis.v[1] * (axis.v[0] * temp.v[0] + axis.v[1] * temp.v[1] + axis.v[2] * temp.v[2]) * (1f - (float) Math.cos(angle))
                    + temp.v[1] * (float) Math.cos(angle)
                    + (axis.v[2] * temp.v[0] - axis.v[0] * temp.v[2]) * (float) Math.sin(angle);
            v[2] = axis.v[2] * (axis.v[0] * temp.v[0] + axis.v[1] * temp.v[1] + axis.v[2] * temp.v[2]) * (1f - (float) Math.cos(angle))
                    + temp.v[2] * (float) Math.cos(angle)
                    + (-axis.v[1] * temp.v[0] + axis.v[0] * temp.v[1]) * (float) Math.sin(angle);
        }
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(v[0]).append(v[1]).append(v[2]).append(" ");
        return s.toString();
    }
}
