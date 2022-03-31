package com.prozium.gravityapp.level;

import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.util.GravityVector3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by cristian on 10.05.2016.
 */
public class GravityGrid {

    public static final float GRID_SIZE = 200f;
    public Collection<GravityBlock> objects;
    public int totalCubes, cubes, vertexBufferId, vertexBufferSize;
    public boolean complete, ready;
    public GravityVector3D center;
    public final GravityMediator mediator;
    long seed;

    public GravityGrid(final GravityMediator mediator) {
        this.mediator = mediator;
    }

    FloatBuffer prepareVertexBuffer(final int vertexSize, final GravityModels models) {
        final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexSize * GravityModels.FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (GravityBlock block : objects) {
            block.vertexOffset = vertexBuffer.position();
            models.copyModel(vertexBuffer, block.modelId);
            models.transform(block, vertexBuffer, block.vertexOffset);
        }
        vertexBuffer.position(0);
        return vertexBuffer;
    }

    public void scoreCubes(final int n) {
        cubes += n;
    }

    public boolean canComplete() {
        return !complete && cubes >= totalCubes;
    }

    void generateCenter(final GravityVector3D position) {
        center = new GravityVector3D(new float[] {(float) Math.floor(position.v[0] / GRID_SIZE) * GRID_SIZE + GRID_SIZE / 2f, 0f, (float) Math.floor(position.v[2] / GRID_SIZE) * GRID_SIZE + GRID_SIZE / 2f});
    }

    public static long generateKey(final GravityVector3D position) {
        return (long) (Math.floor(position.v[0] / GRID_SIZE) * 15485863.0 + Math.floor(position.v[2] / GRID_SIZE));
    }

    int generateTrees(final Random rand, final float cityRadius, final float radius, final int total) {
        GravityBlock b;
        final float s = 8f * 2f * GravityBlock.SCALE + 10f / GravityVector3D.ERROR;
        float a, d;
        for (int i = 0; i < total; i++) {
            b = new GravityBlock(this, 8f, 1);
            a = (rand.nextFloat() * 2f * (float) Math.PI);
            d = rand.nextFloat() * (radius - cityRadius) + cityRadius;
            b.position.v[0] = (float) Math.sin(a) * d * s;
            b.position.v[1] = b.shape.side + 10f / GravityVector3D.ERROR;
            b.position.v[2] = (float) Math.cos(a) * d * s;
            b.angle.v[1] = rand.nextFloat() * 360f - 180f;
            b.stack = new ArrayList<>();
            b.stack.add(b);
            mediator.blockAdded(b);
        }
        return total;
    }

    int generateMap(final Random rand, final int width, final int height, final int buildingMinHeight, final int buildingMaxHeight) {
        final int[][] map = new int[width][height];
        final float s = 2f * 2f * GravityBlock.SCALE + 10f / GravityVector3D.ERROR;
        int buildings, stories, c = 0, n = 0;
        boolean diagonal;
        GravityBlock b = null, temp;
        List<GravityBlock> stack;
        final List<int[]> pending = new ArrayList<>();
        final GravityBlock[][][] wall = new GravityBlock[2][width][height];
        int[] index;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                buildings = 0;
                stories = 0;
                diagonal = (i == 0 || j == 0 || map[i - 1][j - 1] == 0);
                if (i > 0 && map[i - 1][j] != 0) {
                    buildings++;
                    stories = map[i - 1][j];
                }
                if (j > 0 && map[i][j - 1] != 0) {
                    buildings++;
                    stories = map[i][j - 1];
                }
                if (buildings == 2) {
                    map[i][j] = stories;
                } else if (buildings == 1 && j < height - 1 && diagonal) {
                    map[i][j] = rand.nextInt(5) > 3 ? 0 : stories;
                } else if (buildings == 0 && diagonal) {
                    map[i][j] = rand.nextInt(400) > 0 ? 0 : buildingMinHeight + rand.nextInt(buildingMaxHeight - buildingMinHeight + 1);
                }
                if (map[i][j] > 0) {
                    pending.add(new int[] {i, j});
                }
            }
        }
        int t = pending.size();
        for (int i = 0; i < t; i++) {
            index = pending.get(i);
            if (index[0] == 0
                    || index[0] == width - 1
                    || index[1] == 0
                    || index[1] == height - 1
                    || map[index[0] - 1][index[1]] == 0
                    || map[index[0] + 1][index[1]] == 0
                    || map[index[0]][index[1] - 1] == 0
                    || map[index[0]][index[1] + 1] == 0) {
                stack = new ArrayList<>();
                for (int k = 0; k < map[index[0]][index[1]]; k++) {
                    b = new GravityBlock(this, 2f, 0);
                    b.position.v[0] = index[0] * s - width * s / 2f;
                    b.position.v[1] = k * s + b.shape.side + 10f / GravityVector3D.ERROR;
                    b.position.v[2] = index[1] * s - height * s / 2f;
                    stack.add(b);
                    b.stack = stack;
                    mediator.blockAdded(b);
                    n++;
                }
                wall[c][index[0]][index[1]] = b;
                pending.remove(i);
                i--;
                t--;
            }
        }
        while (t > 0) {
            for (int i = 0; i < t; i++) {
                index = pending.get(i);
                if (wall[c][index[0] - 1][index[1]] != null) {
                    temp = wall[c][index[0] - 1][index[1]];
                } else if (wall[c][index[0] + 1][index[1]] != null) {
                    temp = wall[c][index[0] + 1][index[1]];
                } else if (wall[c][index[0]][index[1] - 1] != null) {
                    temp = wall[c][index[0]][index[1] - 1];
                } else if (wall[c][index[0]][index[1] + 1] != null) {
                    temp = wall[c][index[0]][index[1] + 1];
                } else {
                    temp = null;
                }
                if (temp != null) {
                    b = new GravityBlock(this, 2f, 0);
                    b.position.v[0] = index[0] * s - width * s / 2f;
                    b.position.v[1] = (map[index[0]][index[1]] - 1) * s + b.shape.side + 10f / GravityVector3D.ERROR;
                    b.position.v[2] = index[1] * s - height * s / 2f;
                    b.stack = temp.stack;
                    b.stack.add(b);
                    mediator.blockAdded(b);
                    n++;
                    wall[(c + 1) % 2][index[0]][index[1]] = b;
                    pending.remove(i);
                    i--;
                    t--;
                }
            }
            c = (c + 1) % 2;
        }
        return n;
    }

    public void generateGrid() {
        int n = 0;
        final Random rand = new Random(seed);
        final int[] o = new int[] {0, 0};
        if (rand.nextBoolean()) {
            o[1] = generateTrees(rand, 20f, 40f, 10);
            if (!complete) {
                o[0] = generateMap(rand, 50, 50, 4, 7);
            }
        } else {
            complete = true;
            o[1] = generateTrees(rand, 0f, 40f, 5);
        }
        mediator.onGridComplete(this, o);
    }
}
