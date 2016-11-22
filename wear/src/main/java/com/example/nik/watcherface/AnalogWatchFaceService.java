package com.example.nik.watcherface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.sql.Time;
import java.util.Calendar;
import java.util.TimeZone;

import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

/**
 * Created by nik on 11/16/2016.
 */

public class AnalogWatchFaceService extends CanvasWatchFaceService {

    // Create the new engine that draws the watchFace
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    // Implement the service callback methods
    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;

        // Stroke widths for the watch hands
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 4;

        private float mCenterX;
        private float mCenterY;

        Calendar mCalendar;

        // device features
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;

        private boolean mRegisteredTimeZoneReciever = false;

        // Background for the watch
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        // Paint objects for the watch face
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mTickAndCirclePaint;

        // Watch hands colors
        private int mWatchHandColor;
        private int mWatchHandShadowColor;
        private int mWatchHandHighlightColor;

        // Watch Hand lengths
        private float mHourHandLength;
        private float mMinuteHandLength;
        private float mSecondHandLength;


        // handler to update the time once a second in interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        // receiver to update the time zone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        // Create your watchface
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // Configure the system UI
            // Configures peeking cards to be a single line tall, background of peekCard is shown briefly
            // and only for interruptive notifications, and system time to not be sown
            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());


            // Load the background image
            // The Background bitmap is loaded only once when the system initializes the watchface
            Resources resources = AnalogWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bluebg, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            // Default Watch hands colors
            mWatchHandColor = Color.WHITE;
            mWatchHandShadowColor = Color.BLACK;
            mWatchHandHighlightColor = Color.RED;

            // Create graphic styles for the Watch hands
            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStyle(Paint.Style.STROKE);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            // allocate a calendar to calculate local time using the UTC time and time zone
            mCalendar = Calendar.getInstance();

        }

        // Device features like burn-in protection and low-bit ambient mode.
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

        }

        // When the time changes. It is called every minute by the system and it is sufficient to update in ambient mode
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        // When the mode is changed
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;

            updateWatchHandStyle();
            invalidate();

            // Check and trigger whether or not timer should be running (only runs in ambient mode)
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();
            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mTickAndCirclePaint.setColor(mWatchHandColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }

        }

        // Scale the background to fit the device any time the view changes
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Find the center. Ignore the window insets so that the round watches
            // with a "chin". the watch face is centered on the entire screen, not just
            // the usable portion

            mCenterX = width / 2f;
            mCenterY = height / 2f;

            // Calculate the lengths of different hands based on watch screen size
            mHourHandLength = (float) (mCenterX * 0.5);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mSecondHandLength = (float) (mCenterX * 0.875);

            if (mBackgroundScaledBitmap == null || mBackgroundScaledBitmap.getWidth() != width || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, width, height, true);
            }

        }

        // Draw your watch face
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            // Update the time
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Drawing the background
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawColor(Color.BLACK);
            } else {

                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            /* Code to draw the ticks on the watchface, usually it should be created with the background,
            *  but in cases where we have to allow users to select their own photos, this dynamically
            *  created them to top of the photo*/

            float innerTickRadius = mCenterX -10;
            float outerTickRadius = mCenterX;
            for(int tickIndex = 0; tickIndex <12; tickIndex++){
                float tickRot = (float) (tickIndex*Math.PI*2/12);
                float innerX = (float) Math.sin(tickRot)*innerTickRadius;
                float innerY = (float) -Math.cos(tickRot)*innerTickRadius;
                float outerX = (float) (Math.sin(tickRot)*outerTickRadius);
                float outerY = (float) (-Math.cos(tickRot)*outerTickRadius);

                canvas.drawLine(mCenterX + innerX, mCenterY+ innerY,
                        mCenterX+outerX,mCenterY+outerY,mTickAndCirclePaint);

            }

            // Compute rotations and lengths for the clock hands
            final float seconds = (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            // Save the canvas state before we can begin to rotate it
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint
            );

            // Seconds should only be shown in the interactive mode
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint
                );

                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        CENTER_GAP_AND_CIRCLE_RADIUS,
                        mTickAndCirclePaint
                );
            }

            //Restore the canvas original orientation
            canvas.restore();
        }


        // The watch face becomes visible or invisible
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReciever();
            }

            // If updating the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();

        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReciever) {
                return;
            }
            mRegisteredTimeZoneReciever = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReciever() {
            if (!mRegisteredTimeZoneReciever) {
                return;
            }

            mRegisteredTimeZoneReciever = false;
            AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

    }


}
