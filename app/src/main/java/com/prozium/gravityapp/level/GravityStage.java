package com.prozium.gravityapp.level;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.prozium.gravityapp.R;
import com.prozium.gravityapp.level.block.GravityBlock;
import com.prozium.gravityapp.sap.GravityMultiSAP;
import com.prozium.gravityapp.util.GravityVector3D;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cristian on 03.03.2016.
 */
public class GravityStage {

    final static int ALL_LOADED = 13;

    public final int[] ammo = new int[] {12, 0, 0};
    public boolean launch, gameStarted, isRunning = true;
    final GravityMediator mediator;
    public int hitId, launchId, planeId, messageId, bombId, loaded, score;
    public final SoundPool soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
    public final Collection<GravityBlock> objects = Collections.newSetFromMap(new ConcurrentHashMap<GravityBlock, Boolean>());
    public final GravityMultiSAP sap;
    public GravityVector3D detonateCenter;
    public float detonateScale;

    public GravityStage(final GravityMediator mediator, final Context context) {
        this.mediator = mediator;
        sap = new GravityMultiSAP(mediator);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

            @Override
            public void onLoadComplete(final SoundPool soundPool, final int sampleId, final int status) {
                loaded++;
                if (loaded > ALL_LOADED) {
                    mediator.assetsLoaded();
                }
            }
        });
        hitId = soundPool.load(context, R.raw.hit, 0);
        launchId = soundPool.load(context, R.raw.launch, 0);
        planeId = soundPool.load(context, R.raw.plane, 0);
        messageId = soundPool.load(context, R.raw.message, 0);
        bombId = soundPool.load(context, R.raw.bomb, 0);
    }

    public void addScore(final int n) {
        if (n > 0) {
            score += n;
            mediator.setMessage("+" + n);
        }
    }

    //TODO: fix block below ground, fix block in air, fix trees normals, fix active objects
    //TODO: optimize fixed blocks, convert obj to own format
    //TODO: rectangular shape collision (fixes collisions that do not exact intersect)
    //TODO: land plane/take car or walk, bomb obj, roads between towns
    //TODO: people, vehicles, armies battle each other, various types of buildings
    //TODO: rpg, improve weapons range, power, number, type and rate of fire, increase plane range before refuel
    //TODO: score ladder, accomplishments (no of towns destroyed, types of buildings), quests
    //TODO: use quaternions instead of three rotations, show plane, show score
    //TODO: put stacking blocks in gl on a background thread so it won't interfere with the fps
    //TODO: random grid centers, spherical ground
    //TODO: enable cull face, may need to draw trees separately
    //TODO: render blocks as spheres and apply blur shader to look like the buildings melt
    //TODO: skeletal animation
    //TODO: use gles30 (api level 18) and glBindVertexArray (VAO)
    void loop() {
        if (launch) {
            if (ammo[2] > 0) {
                mediator.launchNuke();
                launch = false;
            } else if (ammo[1] > 0) {
                mediator.launchBomb();
                launch = false;
            } else if (ammo[0] > 0) {
                if (mediator.r.nextInt(4) == 0) {
                    mediator.launchImpact();
                    if (ammo[0] == 0) {
                        launch = false;
                    }
                }
            } else {
                launch = false;
            }
        } else if (ammo[1] + ammo[2] == 0) {
            final int n = mediator.r.nextInt(1000);
            if (ammo[2] == 0 && n == 0) {
                ammo[1]++;
                mediator.setMessage("NUKE");
            } else if (ammo[1] == 0 && n < 3) {
                ammo[2]++;
                mediator.setMessage("BOMB");
            }
        }
        addScore(sap.loopCollisions());
        final Iterator<GravityBlock> i = objects.iterator();
        if (i.hasNext()) {
            while (i.hasNext()) {
                final GravityBlock b = i.next();
                assert (b.stack == null);
                if (b.isRemoved()) {
                    i.remove();
                } else {
                    mediator.blockUpdated(b);
                }
            }
        }
    }
}
