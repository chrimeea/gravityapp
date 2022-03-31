package com.prozium.gravityapp.level;

import com.prozium.gravityapp.util.GravityVector3D;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by cristian on 09.04.2017.
 */

public class GravityCamera {

    public GravityGrid cameraLocation;
    public final GravityGridContainer grids;
    public final GravityVector3D camera = new GravityVector3D(new float[]{GravityGrid.GRID_SIZE / 2f, 1.9f, GravityGrid.GRID_SIZE / 2f - 100f});
    public final GravityVector3D cameraFront = new GravityVector3D(new float[]{0f, 0f, 1f});
    public final GravityVector3D cameraGo = new GravityVector3D(new float[]{0f, 0f, 1f});
    public GravityVector3D cameraTrack = cameraGo;
    public float yaw = (float) Math.PI / 2f, roll, targetYaw = yaw;
    public boolean autopilot, freemove = true;
    final GravityMediator mediator;

    public GravityCamera(final GravityMediator mediator) {
        this.mediator = mediator;
        grids = new GravityGridContainer(mediator);
    }

    float trackingAngle(final GravityVector3D position) {
        final GravityVector3D temp = new GravityVector3D(position).substract(camera);
        final float targetYaw = temp.angle(new GravityVector3D(new float[] {1f, 0f, 0f}));
        return temp.v[2] < 0f ? -targetYaw : targetYaw;
    }

    float rollToYaw() {
        return 0.002f * 0.1f * (roll / 0.1f) * ((Math.abs(roll) / 0.1f) + 1f) / 2f;
    }

    public void changeDirection(final boolean turnRight) {
        autopilot = false;
        freemove = false;
        cameraGo.resetTo(cameraFront).normalize();
        cameraTrack = cameraGo;
        if (turnRight) {
            if (roll > -45f) {
                roll -= 0.3f;
            }
        } else if (roll < 45f) {
            roll += 0.3f;
        }
    }

    public void moveCamera() {
        final float d = camera.distance(cameraLocation.center);
        if (autopilot) {
            cameraFront.v[0] = cameraLocation.center.v[0];
            cameraFront.v[1] = camera.v[1];
            cameraFront.v[2] = cameraLocation.center.v[2];
            cameraFront.substract(camera);
            camera.add(new GravityVector3D(cameraGo).multiply(0.5f));
            if (targetYaw == yaw && cameraGo.dotProduct(cameraFront) < 0f) {
                targetYaw = trackingAngle(cameraLocation.center);
            }
            if (freemove) {
                if ((yaw < targetYaw && yaw - rollToYaw() < targetYaw)
                        || (yaw > targetYaw && yaw - rollToYaw() > targetYaw)) {
                    if (yaw < targetYaw) {
                        roll -= 0.1f;
                    } else {
                        roll += 0.1f;
                    }
                } else if (roll != 0f) {
                    if (roll > 0f) {
                        roll -= 0.1f;
                    } else {
                        roll += 0.1f;
                    }
                }
            }
            if (cameraLocation.canComplete()) {
                cameraLocation.complete = true;
                mediator.locationComplete();
            } else {
                if (Math.abs(roll) > 0.06) {
                    yaw -= 0.002f * roll;
                    cameraGo.v[0] = (float) Math.cos(yaw);
                    cameraGo.v[2] = (float) Math.sin(yaw);
                    targetYaw = trackingAngle(cameraLocation.center);
                } else {
                    roll = 0f;
                    targetYaw = yaw;
                    cameraTrack = cameraFront;
                }
            }
        } else {
            cameraFront.resetTo(cameraGo);
            camera.add(new GravityVector3D(cameraGo).multiply(0.5f));
            if (freemove) {
                if  (Math.abs(roll) < 0.3f) {
                    roll = 0.0f;
                } else if (roll < 0f) {
                    roll += 0.3f;
                } else if (roll > 0f) {
                    roll -= 0.3f;
                }
            }
            if (roll != 0f) {
                yaw -= 0.002f * roll;
                cameraGo.v[0] = (float) Math.cos(yaw);
                cameraGo.v[2] = (float) Math.sin(yaw);
            }
        }
        final GravityGrid old = cameraLocation;
        cameraLocation = grids.generateGridArea(camera);
        if (cameraLocation != old) {
            for (final GravityGrid g: grids.grid.values()) {
                if (g.ready
                        && g.vertexBufferSize > 0
                        && g.center.distance(cameraLocation.center) > 2f * GravityGrid.GRID_SIZE) {
                    g.ready = false;
                    g.objects = null;
                    g.vertexBufferSize = g.totalCubes = g.cubes = 0;
                    mediator.gridFreed(g);
                }
            }
        }
    }

    public void setAutopilot() {
        if (!autopilot) {
            targetYaw = trackingAngle(cameraLocation.center);
            autopilot = true;
            mediator.setMessage("AUTOPILOT");
        }
    }
}
