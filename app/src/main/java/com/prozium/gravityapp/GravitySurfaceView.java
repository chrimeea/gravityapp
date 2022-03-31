package com.prozium.gravityapp;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.prozium.gravityapp.level.GravityMediator;

/**
 * Created by cristian on 26.02.2016.
 */
public class GravitySurfaceView extends GLSurfaceView {

    public final GravityRenderer renderer;
    public GravityMediator mediator;
    float x, y;

    GravitySurfaceView(final GravityActivity context) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new GravityRenderer(getResources());
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        this.mediator = new GravityMediator(this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            x = e.getX();
            y = e.getY();
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            if (Math.abs(x - e.getX()) > 20.0 && Math.abs(y - e.getY()) < 2.0 * Math.abs(x - e.getX())) {
                mediator.userChangeDirection(e.getX() > x);
            }
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
            mediator.userEndsMove();
            if (Math.abs(y - e.getY()) > 10.0
                    && Math.abs(y - e.getY()) > 2.0 * Math.abs(x - e.getX())
                    && e.getY() > y) {
                mediator.userSwipeDown();
            }
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mediator.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mediator.onResume();
    }
}
