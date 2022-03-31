package com.prozium.gravityapp.level;

import android.content.res.Resources;

import com.prozium.gravityapp.R;
import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.util.GravityVector3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by cristian on 15.05.2016.
 */
public class GravityModels {

    public static final int COORDS_PER_VERTEX = 3;
    public static final int TEXTURE_PER_VERTEX = 2;
    public static final int FLOAT_SIZE = 4;
    public static final int STRIDE = COORDS_PER_VERTEX * 4 + TEXTURE_PER_VERTEX;
    public static final FloatBuffer emptyBuffer = ByteBuffer.allocateDirect(104832 * FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(217266 * FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
    final int modelOffset[];

    public GravityModels(final Resources res) {
        try {
            modelOffset = new int[6];
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.cube), -1);
            modelOffset[1] = vertexBuffer.position();
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.tree1), -1);
            modelOffset[2] = vertexBuffer.position();
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.person1), -1);
            vertexBuffer.position(modelOffset[2]);
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.person2), modelOffset[2]);
            modelOffset[3] = vertexBuffer.position();
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.person1), modelOffset[2]);
            modelOffset[4] = vertexBuffer.position();
            loadModelFromObj(vertexBuffer, res.openRawResource(R.raw.sphere), -1);
            modelOffset[5] = vertexBuffer.position();
            vertexBuffer.position(0);
            //Log.e("********************", ""+(modelOffset[3] - modelOffset[2]));
            //Log.e("********************", ""+modelOffset[5]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int rawModelOffset(final int modelId) {
        return modelOffset[modelId];
    }

    public int modelOffset(final int modelId) {
        return rawModelOffset(modelId) / STRIDE;
    }

    public int rawModelSize(final int modelId) {
        return modelOffset[modelId + 1] - modelOffset[modelId];
    }

    public int modelSize(final int modelId) {
        return rawModelSize(modelId) / STRIDE;
    }

    public void copyModel(final FloatBuffer buffer, final int modelId) {
        final FloatBuffer temp = vertexBuffer.duplicate();
        temp.position(rawModelOffset(modelId));
        temp.limit(temp.position() + rawModelSize(modelId));
        buffer.put(temp);
    }

    void loadModelFromObj(final FloatBuffer vertexBuffer, final InputStream is, int prevIndex) throws IOException {
        int i, j;
        float[] temp = new float[0];
        final ArrayList<float[]> v = new ArrayList<>();
        final ArrayList<float[]> vt = new ArrayList<>();
        final ArrayList<float[]> vn = new ArrayList<>();
        final StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
        st.commentChar('#');
        st.whitespaceChars('/', '/');
        st.eolIsSignificant(true);
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            if (st.ttype == StreamTokenizer.TT_WORD) {
                if (st.sval.equals("v")) {
                    temp = new float[COORDS_PER_VERTEX];
                    for (i = 0; i < temp.length; i++) {
                        st.nextToken();
                        temp[i] = (float) st.nval;
                    }
                    v.add(temp);
                } else if (st.sval.equals("vt")) {
                    temp = new float[2];
                    for (i = 0; i < temp.length; i++) {
                        st.nextToken();
                        temp[i] = (float) st.nval;
                    }
                    vt.add(temp);
                } else if (st.sval.equals("vn")) {
                    temp = new float[COORDS_PER_VERTEX];
                    for (i = 0; i < temp.length; i++) {
                        st.nextToken();
                        temp[i] = (float) st.nval;
                    }
                    vn.add(temp);
                } else if (st.sval.equals("f")) {
                    for (i = 0; i < COORDS_PER_VERTEX; i++) {
                        st.nextToken();
                        if (prevIndex != -1) {
                            temp = new float[COORDS_PER_VERTEX];
                            for (j = 0; j < COORDS_PER_VERTEX; j++) {
                                temp[j] = vertexBuffer.get(prevIndex + j);
                            }
                            vertexBuffer.put(v.get((int) st.nval - 1));
                            vertexBuffer.put(temp);
                        } else {
                            temp = v.get((int) st.nval - 1);
                            vertexBuffer.put(temp);
                            vertexBuffer.put(temp);
                        }
                        st.nextToken();
                        vertexBuffer.put(vt.get((int) st.nval - 1));
                        st.nextToken();
                        if (prevIndex != -1) {
                            prevIndex += 2 * COORDS_PER_VERTEX + TEXTURE_PER_VERTEX;
                            for (j = 0; j < COORDS_PER_VERTEX; j++) {
                                temp[j] = vertexBuffer.get(prevIndex + j);
                            }
                            vertexBuffer.put(vn.get((int) st.nval - 1));
                            vertexBuffer.put(temp);
                            prevIndex += 2 * COORDS_PER_VERTEX;
                        } else {
                            temp = vn.get((int) st.nval - 1);
                            vertexBuffer.put(temp);
                            vertexBuffer.put(temp);
                        }
                    }
                }
            }
            while (st.nextToken() != StreamTokenizer.TT_EOL);
        }
        is.close();
    }

    void transform(final GravityBlock block, final FloatBuffer buffer, int offset) {
        final GravityVector3D temp = new GravityVector3D();
        final int t = modelSize(block.modelId);
        int i, j;
        for (i = 0; i < t; i++) {
            for (j = 0; j < COORDS_PER_VERTEX; j++) {
                temp.v[j] = buffer.get(offset + j);
            }
            temp.rotate(block.angle);
            for (j = 0; j < COORDS_PER_VERTEX; j++) {
                buffer.put(offset + j, (temp.v[j] * block.shape.side + block.position.v[j]));
                buffer.put(offset + j + 3, buffer.get(offset + j));
                temp.v[j] = buffer.get(offset + j + 8);
            }
            temp.rotate(block.angle);
            for (j = 0; j < COORDS_PER_VERTEX; j++) {
                buffer.put(offset + j + 8, temp.v[j]);
                buffer.put(offset + j + 11, buffer.get(offset + j + 8));
            }
            offset += STRIDE;
        }
    }
}
