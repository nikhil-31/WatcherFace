package com.example.nik.watcherface;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.view.SurfaceHolder;

/**
 * Created by nik on 11/16/2016.
 */

public class AnalogWatchFaceService extends CanvasWatchFaceService {

    // Create the new engine that draws the watchFace
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    // Implement the service callback methods
    private class Engine extends CanvasWatchFaceService.Engine{

        // Create your watchface
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);


        }

        // Device features like burn-in protection and low-bit ambient mode
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }


        // When the time changes
        @Override
        public void onTimeTick() {
            super.onTimeTick();
        }

        // When the mode is changed
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }


        // draw your watch face
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
        }

        // The watch face becomes visible or invisible
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }
    }


}
