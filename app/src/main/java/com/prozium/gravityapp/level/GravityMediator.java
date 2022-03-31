package com.prozium.gravityapp.level;

import android.util.Log;

import com.prozium.gravityapp.GravitySurfaceView;
import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.level.block.GravityBlockAmmo;
import com.prozium.gravityapp.level.block.GravityBlockPerson;
import com.prozium.gravityapp.util.GravityVector3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cristian on 10.04.2017.
 */

public class GravityMediator {

    final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(2);
    GravityCamera camera;
    GravityStage stage;
    public final Random r = new Random(System.currentTimeMillis());
    final GravitySurfaceView view;

    public GravityMediator(final GravitySurfaceView view) {
        this.view = view;
        view.renderer.mediator = this;
        stage = new GravityStage(this, view.getContext());
        camera = new GravityCamera(this);
    }

    public void renderReady() {
        camera.cameraLocation = camera.grids.generateGridArea(camera.camera);
        view.renderer.camera = camera;
        view.renderer.stage = stage;
    }

    public void userChangeDirection(final boolean turnRight) {
        camera.changeDirection(turnRight);
    }

    public void userEndsMove() {
        camera.freemove = true;
    }

    public void userSwipeDown() {
        camera.setAutopilot();
        stage.launch = true;
    }

    public void assetsLoaded() {
        setMessage("BEGIN");
        viewHasChanged();
        if (!stage.gameStarted && stage.loaded > GravityStage.ALL_LOADED) {
            stage.gameStarted = true;
            stage.soundPool.play(stage.planeId, 1f, 1f, 100, -1, 1f);
            timer.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (stage.isRunning) {
                        stage.loop();
                    }
                }
            }, 0, 33, TimeUnit.MILLISECONDS);
            timer.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (stage.isRunning) {
                        camera.moveCamera();
                        stage.mediator.viewHasChanged();
                    }
                }
            }, 0, 33, TimeUnit.MILLISECONDS);
        }
    }

    public void viewHasChanged() {
        view.requestRender();
    }

    public void setMessage(final String message) {
        stage.soundPool.play(stage.messageId, 1f, 1f, 5, 0, 1f);
        view.queueEvent(new Runnable() {
            @Override
            public void run() {
                view.renderer.drawText(message);
            }
        });
    }

    public void onGridComplete(final GravityGrid grid, final int[] o) {
        int n = 0;
        for (int i = 0; i < o.length; i++) {
            n += o[i] * view.renderer.models.rawModelSize(i);
        }
        final FloatBuffer buffer = grid.prepareVertexBuffer(n, view.renderer.models);
        view.queueEvent(new Runnable() {
            @Override
            public void run() {
                grid.vertexBufferId = view.renderer.loadBuffer(buffer);
                grid.vertexBufferSize = buffer.capacity() / GravityModels.STRIDE;
            }
        });
        if (!stage.gameStarted) {
            stage.loaded++;
            if (stage.loaded > GravityStage.ALL_LOADED) {
                assetsLoaded();
            }
        }
    }

    public void launchBomb() {
        final GravityBlockAmmo b = GravityBlockAmmo.makeAmmo(camera, r, 2f);
        blockAdded(b);
        blockBecomesActive(b);
        stage.soundPool.play(stage.launchId, 1f, 1f, 5, 0, 1f);
        stage.ammo[1]--;
    }

    public void launchNuke() {
        final GravityBlockAmmo b = GravityBlockAmmo.makeAmmo(camera, r, 4f);
        blockAdded(b);
        blockBecomesActive(b);
        stage.soundPool.play(stage.launchId, 1f, 1f, 5, 0, 1f);
        stage.ammo[2]--;
    }

    public void launchImpact() {
        final GravityBlockAmmo b = GravityBlockAmmo.makeAmmo(camera, r, 0f);
        blockAdded(b);
        blockBecomesActive(b);
        stage.soundPool.play(stage.launchId, 1f, 1f, 5, 0, 1f);
        stage.ammo[0]--;
    }

    public void personKilled() {
        setMessage("KILLED");
        stage.addScore(50);
    }

    public void locationComplete() {
        setMessage("COMPLETE");
        stage.addScore(100);
    }

    public void gridFreed(final GravityGrid g) {
        view.queueEvent(new Runnable() {
            @Override
            public void run() {
                view.renderer.freeBuffer(g.vertexBufferId);
            }
        });
    }

    public void blockBecomesActive(final GravityBlock block) {
        block.stack = null;
        if (stage.objects.add(block) && !block.isTransient) {
            view.queueEvent(new Runnable() {
                @Override
                public void run() {
                    view.renderer.copySubBuffer(
                            block.grid.vertexBufferId,
                            block.vertexOffset,
                            view.renderer.models.rawModelSize(block.modelId),
                            GravityModels.emptyBuffer.duplicate());
                }
            });
        }
    }

    public void blockBecomesInactive(final GravityBlock block) {
        if (stage.objects.remove(block) && !block.isTransient) {
            view.queueEvent(new Runnable() {
                @Override
                public void run() {
                    final int size = view.renderer.models.rawModelSize(block.modelId);
                    final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(size * GravityModels.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
                    view.renderer.models.copyModel(vertexBuffer, block.modelId);
                    view.renderer.models.transform(block, vertexBuffer, 0);
                    vertexBuffer.position(0);
                    view.renderer.copySubBuffer(block.grid.vertexBufferId, block.vertexOffset, size, vertexBuffer);
                }
            });
        }
    }

    public void blockUpdated(final GravityBlock block) {
        block.step(camera);
        block.animate();
        final GravityGrid grid = camera.grids.getGrid(new GravityVector3D(block.grid.center).add(block.position));
        if (grid != null) {
            block.position.add(block.grid.center).substract(grid.center);
            block.grid = grid;
        }
        stage.sap.updateBlock(block);
    }

    public void blockRemoved(final GravityBlock block) {
        assert (block.isTransient);
        if (block.stack != null) {
            block.stack.remove(block);
        }
        if (block.grid.objects.remove(block)) {
            blockBecomesInactive(block);
            stage.sap.removeBlock(block);
            block.onRemove(stage);
        }
    }

    public void blockAdded(final GravityBlock block) {
        stage.sap.addBlock(block);
        if (block.grid.objects.add(block)) {
            if (!block.isTransient) {
                block.grid.totalCubes++;
            }
        }
    }
    
    public void onCollision(final float hit) {
        if (hit * 50f > 0.1f) {
            stage.soundPool.play(stage.hitId, hit, hit, (int) (hit * 10), 0, 1f);
        }
    }

    public void onPause() {
        if (stage != null) {
            stage.isRunning = false;
            stage.soundPool.autoPause();
        }
    }

    public void onResume() {
        if (stage != null) {
            stage.isRunning = true;
            stage.soundPool.autoResume();
        }
    }

    public void buildingCollapse(final GravityBlock block) {
        if (block.stack.size() > 3) {
            block.grid.scoreCubes(block.stack.size());
            final GravityBlockPerson person = GravityBlockPerson.makePerson(block);
            blockAdded(person);
            blockBecomesActive(person);
        }
    }
}
