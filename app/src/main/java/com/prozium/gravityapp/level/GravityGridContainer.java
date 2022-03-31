package com.prozium.gravityapp.level;

import com.prozium.gravityapp.util.GravityVector3D;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by cristian on 09.04.2017.
 */

public class GravityGridContainer {

    public final Map<Long, GravityGrid> grid = new ConcurrentHashMap<Long, GravityGrid>();
    final GravityMediator mediator;
    public final Random r = new Random(System.currentTimeMillis());

    public GravityGridContainer(final GravityMediator mediator) {
        this.mediator = mediator;
    }

    public GravityGrid generateGridArea(final GravityVector3D camera) {
        final GravityVector3D temp = new GravityVector3D(camera);
        final GravityGrid g = setGrid(temp);
        temp.v[0] += GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[2] += GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[0] -= GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[0] -= GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[2] -= GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[2] -= GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[0] += GravityGrid.GRID_SIZE;
        setGrid(temp);
        temp.v[0] += GravityGrid.GRID_SIZE;
        setGrid(temp);
        return g;
    }

    public GravityGrid getGrid(final GravityVector3D position) {
        return grid.get(GravityGrid.generateKey(position));
    }

    public GravityGrid setGrid(final GravityVector3D position) {
        final long key = GravityGrid.generateKey(position);
        GravityGrid g = grid.get(key);
        if (g == null || !g.ready) {
            if (g == null) {
                g = new GravityGrid(mediator);
                g.seed = r.nextLong();
                g.objects = new ConcurrentLinkedQueue<>();
                g.generateCenter(position);
                g.ready = true;
                grid.put(key, g);
            } else {
                g.objects = new ConcurrentLinkedQueue<>();
                g.ready = true;
            }
            final GravityGrid l = g;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    l.generateGrid();
                }
            }).start();
        }
        return g;
    }
}
