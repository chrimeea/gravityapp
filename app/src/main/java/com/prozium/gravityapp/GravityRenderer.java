package com.prozium.gravityapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.prozium.gravityapp.level.GravityCamera;
import com.prozium.gravityapp.level.GravityGrid;
import com.prozium.gravityapp.level.GravityMediator;
import com.prozium.gravityapp.level.GravityModels;
import com.prozium.gravityapp.level.GravityStage;
import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.util.GravityVector3D;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by cristian on 26.02.2016.
 */
public class GravityRenderer implements GLSurfaceView.Renderer {

    static final String vertexShaderCode = "uniform mat4 MVPMatrix, modelMatrix;"
            + "attribute vec3 position, normal, prevPosition, prevNormal;"
            + "attribute vec2 coordinates;"
            + "varying vec3 n, p;"
            + "varying vec2 imageCoordinates;"
            + "uniform float teenFactor;"
            + "void main() {"
            + "vec3 myPosition = mix(prevPosition, position, teenFactor);"
            + "vec3 myNormal = mix(prevNormal, normal, teenFactor);"
            + "gl_Position = MVPMatrix * modelMatrix * vec4(myPosition, 1.0);"
            + "p = vec3(modelMatrix * vec4(myPosition, 1.0));"
            + "n = mat3(modelMatrix) * myNormal;"
            + "imageCoordinates = coordinates;"
            + "}";
    static final String fragmentShaderCode = "precision mediump float;"
            + "uniform float ambientStrength, specularStrength, scale, alpha;"
            + "uniform vec3 ambientColor, ambientPosition, cameraPosition;"
            + "uniform sampler2D image;"
            + "varying vec3 n, p;"
            + "varying vec2 imageCoordinates;"
            + "void main() {"
            + "vec3 norm = normalize(n);"
            + "vec3 lightDirection = normalize(ambientPosition - p);"
            + "float diffuse = max(dot(norm, lightDirection), 0.0);"
            + "float specular = specularStrength * pow(max(dot(normalize(cameraPosition - p), reflect(-lightDirection, norm)), 0.0), 256.0);"
            + "gl_FragColor = vec4((ambientStrength + diffuse + specular) * ambientColor, alpha) * texture2D(image, imageCoordinates * scale);"
            + "}";
    static final float MAX_DISTANCE = 10000f;
    final float[] mMVPMatrix = new float[16];
    final float[] mProjectionMatrix = new float[16];
    final float[] mViewMatrix = new float[16];
    final float[] mModelMatrix = new float[16];
    final float[] ambientLight = new float[] {0.6f, 0.6f, 0.6f};
    int mMVPMatrixHandle1, mModelMatrixHandle, cameraHandle, lightHandle, scaleHandle;
    int positionHandle, coordinatesHandle, normalHandle, ambientHandle, modelId, alphaHandle;
    int prevPositionHandle, prevNormalHandle, teenFactorHandle;
    int[] backgroundTexture = new int[1];
    float width, height, ratio, ambientStrength = 0.2f, specularStrength = 1f, detonateAlpha;
    final Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_4444);
    final Canvas canvas = new Canvas(bitmap);
    final Paint textPaint = new Paint();
    final List<Message> messages = new ArrayList<>();
    public GravityModels models;
    public GravityMediator mediator;
    public GravityStage stage;
    public GravityCamera camera;
    final Resources resources;

    GravityRenderer(final Resources resources) {
        this.resources = resources;
        models = new GravityModels(resources);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        final int mProgram1 = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram1, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode));
        GLES20.glAttachShader(mProgram1, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode));
        GLES20.glLinkProgram(mProgram1);
        GLES20.glUseProgram(mProgram1);
        GLES20.glUniform3fv(GLES20.glGetUniformLocation(mProgram1, "ambientColor"), 1, ambientLight, 0);
        mMVPMatrixHandle1 = GLES20.glGetUniformLocation(mProgram1, "MVPMatrix");
        mModelMatrixHandle = GLES20.glGetUniformLocation(mProgram1, "modelMatrix");
        cameraHandle = GLES20.glGetUniformLocation(mProgram1, "cameraPosition");
        lightHandle = GLES20.glGetUniformLocation(mProgram1, "ambientPosition");
        scaleHandle = GLES20.glGetUniformLocation(mProgram1, "scale");
        alphaHandle = GLES20.glGetUniformLocation(mProgram1, "alpha");
        ambientHandle = GLES20.glGetUniformLocation(mProgram1, "ambientStrength");
        GLES20.glUniform1f(ambientHandle, ambientStrength);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram1, "specularStrength"), specularStrength);
        teenFactorHandle = GLES20.glGetUniformLocation(mProgram1, "teenFactor");
        GLES20.glUniform1f(teenFactorHandle, 0f);
        positionHandle = GLES20.glGetAttribLocation(mProgram1, "position");
        prevPositionHandle = GLES20.glGetAttribLocation(mProgram1, "prevPosition");
        coordinatesHandle = GLES20.glGetAttribLocation(mProgram1, "coordinates");
        normalHandle = GLES20.glGetAttribLocation(mProgram1, "normal");
        prevNormalHandle = GLES20.glGetAttribLocation(mProgram1, "prevNormal");
        modelId = loadBuffer(models.vertexBuffer.duplicate());
        loadTextures();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(30);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        mediator.renderReady();
    }

    @Override
    public void onDrawFrame(final GL10 unused) {
        //final long t1 = System.currentTimeMillis();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTexture[0]);
        final GravityVector3D temp1 = new GravityVector3D(camera.camera).add(camera.cameraTrack);
        Matrix.setLookAtM(mViewMatrix, 0,
                camera.camera.v[0], camera.camera.v[1], camera.camera.v[2],
                temp1.v[0], temp1.v[1], temp1.v[2],
                0f, 1f, 0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        if (camera.roll != 0.0) {
            Matrix.translateM(mMVPMatrix, 0, camera.camera.v[0], camera.camera.v[1], camera.camera.v[2]);
            Matrix.rotateM(mMVPMatrix, 0, camera.cameraTrack == camera.cameraGo
                    ? camera.roll
                    : -camera.roll, camera.cameraTrack.v[0], camera.cameraTrack.v[1], camera.cameraTrack.v[2]);
            Matrix.translateM(mMVPMatrix, 0, -camera.camera.v[0], -camera.camera.v[1], -camera.camera.v[2]);
        }
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle1, 1, false, mMVPMatrix, 0);
        GLES20.glUniform3f(cameraHandle, camera.camera.v[0], camera.camera.v[1], camera.camera.v[2]);
        GLES20.glUniform3f(lightHandle, camera.camera.v[0], camera.camera.v[1], camera.camera.v[2]);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0f, -20f, 0f);
        Matrix.scaleM(mModelMatrix, 0, 10000f, 20f, 10000f);
        GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
        GLES20.glUniform1f(scaleHandle, 1000f);
        switchArrayBuffer(modelId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, models.modelOffset(0) + 30, 6);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0f, 40f, 0f);
        Matrix.scaleM(mModelMatrix, 0, 10000f, 20f, 10000f);
        GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
        GLES20.glUniform1f(scaleHandle, 100f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, models.modelOffset(0) + 24, 6);
        drawAllGrids();
        drawDetonation();
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        drawMessages();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //GLES20.glFinish();
        //Log.e("*", String.valueOf(System.currentTimeMillis() - t1));
    }

    void drawDetonation() {
        if (stage.detonateScale > 0f) {
            switchArrayBuffer(modelId);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glUniform1f(scaleHandle, 1f);
            GLES20.glUniform1f(alphaHandle, detonateAlpha);
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, stage.detonateCenter.v[0], stage.detonateCenter.v[1], stage.detonateCenter.v[2]);
            Matrix.scaleM(mModelMatrix, 0, stage.detonateScale, stage.detonateScale, stage.detonateScale);
            GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, models.modelOffset(4), models.modelSize(4));
            detonateAlpha -= detonateAlpha / (stage.detonateScale / 0.3f);
            stage.detonateScale -= 0.3f;
            GLES20.glDisable(GLES20.GL_BLEND);
        } else {
            detonateAlpha = 0.5f;
        }
    }

    void drawMessages() {
        float maxLife = 0f;
        Matrix.setLookAtM(mViewMatrix, 0,
                0f, 2f, 0f,
                0f, 2f, 1f,
                0f, 1f, 0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle1, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1f(scaleHandle, 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        switchArrayBuffer(modelId);
        int t = messages.size();
        for (int i = 0; i < t; i++) {
            final Message m = messages.get(i);
            if (m.life - maxLife > 3f) {
                maxLife = Math.max(maxLife, m.life);
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 0f, m.position(), 6f);
                GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
                GLES20.glUniform1f(alphaHandle, m.alpha());
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.texture[0]);
                GLES20.glUniform1f(ambientHandle, 1f);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, models.modelOffset(0), 6);
                m.life--;
                if (m.life < 0f) {
                    messages.remove(i);
                    i--;
                    t--;
                    GLES20.glDeleteTextures(1, m.texture, 0);
                }
            }
        }
        GLES20.glUniform1f(ambientHandle, ambientStrength);
        GLES20.glUniform1f(alphaHandle, 1f);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    void drawAllGrids() {
        final GravityVector3D temp2 = new GravityVector3D();
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glUniform1f(scaleHandle, 1f);
        drawActive();
        drawGrid(camera.cameraLocation);
        temp2.resetTo(camera.camera);
        temp2.v[0] += GravityGrid.GRID_SIZE;
        if (camera.cameraTrack.v[0] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[2] += GravityGrid.GRID_SIZE;
        if (camera.cameraTrack.v[0] + camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[0] -= GravityGrid.GRID_SIZE;
        if (camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[0] -= GravityGrid.GRID_SIZE;
        if (-camera.cameraTrack.v[0] + camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[2] -= GravityGrid.GRID_SIZE;
        if (-camera.cameraTrack.v[0] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[2] -= GravityGrid.GRID_SIZE;
        if (-camera.cameraTrack.v[0] - camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[0] += GravityGrid.GRID_SIZE;
        if (-camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        temp2.v[0] += GravityGrid.GRID_SIZE;
        if (camera.cameraTrack.v[0] - camera.cameraTrack.v[2] > 0f) {
            drawGrid(camera.grids.getGrid(temp2));
        }
        //GLES20.glDisable(GLES20.GL_CULL_FACE);
    }

    public void freeBuffer(final int bufferId) {
        GLES20.glDeleteBuffers(1, new int[] {bufferId}, 0);
    }

    public int loadBuffer(final FloatBuffer buffer) {
        final int[] bufferId = new int[1];
        GLES20.glGenBuffers(1, bufferId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * GravityModels.FLOAT_SIZE, buffer, GLES20.GL_STATIC_DRAW);
        return bufferId[0];
    }

    public void copySubBuffer(final int bufferId, final int offset, final int size, final FloatBuffer buffer) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, offset * GravityModels.FLOAT_SIZE, size * GravityModels.FLOAT_SIZE, buffer);
    }

    void drawGrid(final GravityGrid grid) {
        if (grid != null && grid.vertexBufferSize > 0) {
            switchArrayBuffer(grid.vertexBufferId);
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, grid.center.v[0], grid.center.v[1], grid.center.v[2]);
            GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, grid.vertexBufferSize);
        }
    }

    void drawActive() {
        for (GravityBlock block : stage.objects) {
            if (!block.isRemoved()) {
                GLES20.glUniform1f(teenFactorHandle, block.teenFactor);
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(
                        mModelMatrix,
                        0,
                        block.grid.center.v[0] + block.position.v[0],
                        block.grid.center.v[1] + block.position.v[1],
                        block.grid.center.v[2] + block.position.v[2]);
                Matrix.scaleM(mModelMatrix, 0, block.shape.side, block.shape.side, block.shape.side);
                Matrix.rotateM(mModelMatrix, 0, block.angle.v[0], 1f, 0f, 0f);
                Matrix.rotateM(mModelMatrix, 0, block.angle.v[1], 0f, 1f, 0f);
                Matrix.rotateM(mModelMatrix, 0, block.angle.v[2], 0f, 0f, 1f);
                GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES,
                        models.modelOffset(block.modelId),
                        models.modelSize(block.modelId));
            }
        }
        GLES20.glUniform1f(teenFactorHandle, 0f);
    }

    void switchArrayBuffer(final int bufferId) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle,
                GravityModels.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                GravityModels.STRIDE * GravityModels.FLOAT_SIZE,
                0);
        GLES20.glEnableVertexAttribArray(prevPositionHandle);
        GLES20.glVertexAttribPointer(prevPositionHandle,
                GravityModels.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                GravityModels.STRIDE * GravityModels.FLOAT_SIZE,
                GravityModels.COORDS_PER_VERTEX * GravityModels.FLOAT_SIZE);
        GLES20.glEnableVertexAttribArray(coordinatesHandle);
        GLES20.glVertexAttribPointer(coordinatesHandle,
                GravityModels.TEXTURE_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                GravityModels.STRIDE * GravityModels.FLOAT_SIZE,
                2 * GravityModels.COORDS_PER_VERTEX * GravityModels.FLOAT_SIZE);
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle,
                GravityModels.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                GravityModels.STRIDE * GravityModels.FLOAT_SIZE,
                (2 * GravityModels.COORDS_PER_VERTEX + GravityModels.TEXTURE_PER_VERTEX) * GravityModels.FLOAT_SIZE);
        GLES20.glEnableVertexAttribArray(prevNormalHandle);
        GLES20.glVertexAttribPointer(prevNormalHandle,
                GravityModels.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                GravityModels.STRIDE * GravityModels.FLOAT_SIZE,
                (3 * GravityModels.COORDS_PER_VERTEX + GravityModels.TEXTURE_PER_VERTEX) * GravityModels.FLOAT_SIZE);
    }

    @Override
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);
        if (width < height) {
            ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, MAX_DISTANCE);
        } else {
            ratio = (float) height / width;
            Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, MAX_DISTANCE);
        }
    }

    int loadShader(final int type, final String shaderCode) {
        final int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Could not compile program: "
                    + GLES20.glGetShaderInfoLog(shader) + " | " + type);
        }
        return shader;
    }

    void loadTextures() {
        GLES20.glGenTextures(1, backgroundTexture, 0);
        loadTextureFromFile(backgroundTexture[0], R.drawable.background);
    }

    void loadTextureFromFile(final int textureId, final int resourceId) {
        assert (textureId != 0);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, options);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        bitmap.recycle();
    }

    public void drawText(final String text) {
        final Message m = new Message(text);
        GLES20.glGenTextures(1, m.texture, 0);
        bitmap.eraseColor(Color.argb(0, 0, 0, 0));
        /*
        final Drawable background = context.getResources().getDrawable(R.drawable.background);
        background.setBounds(0, 0, 70, 70);
        background.draw(canvas);
        */
        textPaint.setColor(Color.WHITE);
        canvas.drawText(text, 80f - 6f * text.length(), 50f, textPaint);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.texture[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        messages.add(m);
    }

    void checkGlError(final String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GravityRenderer", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    class Message {

        static final float MAX_LIFE = 50f;
        String message;
        int[] texture = new int[1];
        float life = MAX_LIFE;

        Message(final String text) {
            message = text;
        }

        float alpha() {
            return 1f - (MAX_LIFE - life) * 0.02f;
        }

        float position() {
            return 1.8f + (MAX_LIFE - life) * 0.03f;
        }
    }
}
