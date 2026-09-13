/*
 * Copyright (C) 2019-2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;

public class SwipeToScreenshotListener implements PointerEventListener {
    private static final String TAG = "SwipeToScreenshot";
    private static final int THREE_GESTURE_STATE_NONE = 0;
    private static final int THREE_GESTURE_STATE_DETECTING = 1;
    private static final int THREE_GESTURE_STATE_DETECTED_FALSE = 2;
    private static final int THREE_GESTURE_STATE_DETECTED_TRUE = 3;

    private static final int MAX_TRACKED_POINTERS = 3;
    private static final int UNTRACKED_POINTER = -1;

    private int mDeviceThresholdY = 160;
    private final int[] mPointerIds = new int[MAX_TRACKED_POINTERS];
    private final float[] mInitMotionY = new float[MAX_TRACKED_POINTERS];
    private final float[] mInitMotionX = new float[MAX_TRACKED_POINTERS];

    private final Callbacks mCallbacks;
    private final Context mContext;
    private int mThreeGestureState = THREE_GESTURE_STATE_NONE;

    public interface Callbacks {
        void onSwipeThreeFinger();
    }

    public SwipeToScreenshotListener(Context context, Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        mDeviceThresholdY = (int) (60.0f * displayMetrics.density);
        if (mDeviceThresholdY < 140) {
            mDeviceThresholdY = 140;
        }
        reset();
    }

    private void reset() {
        for (int i = 0; i < MAX_TRACKED_POINTERS; i++) {
            mPointerIds[i] = UNTRACKED_POINTER;
            mInitMotionY[i] = -1;
            mInitMotionX[i] = -1;
        }
        mThreeGestureState = THREE_GESTURE_STATE_NONE;
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
            return;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            reset();
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            if (event.getPointerCount() == 3) {
                startTracking(event);
            } else if (event.getPointerCount() > 3) {
                mThreeGestureState = THREE_GESTURE_STATE_DETECTED_FALSE;
            }
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            if (mThreeGestureState != THREE_GESTURE_STATE_DETECTED_TRUE) {
                reset();
            }
            return;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (mThreeGestureState == THREE_GESTURE_STATE_DETECTING) {
                if (event.getPointerCount() != 3) {
                    mThreeGestureState = THREE_GESTURE_STATE_DETECTED_FALSE;
                    return;
                }

                boolean allMovedDown = true;
                for (int i = 0; i < MAX_TRACKED_POINTERS; i++) {
                    int pointerIndex = event.findPointerIndex(mPointerIds[i]);
                    if (pointerIndex < 0) {
                        allMovedDown = false;
                        break;
                    }
                    float deltaY = event.getY(pointerIndex) - mInitMotionY[i];
                    float deltaX = Math.abs(event.getX(pointerIndex) - mInitMotionX[i]);

                    if (deltaY < mDeviceThresholdY) {
                        allMovedDown = false;
                    }
                    if (deltaX > deltaY || deltaY < -20) {
                        mThreeGestureState = THREE_GESTURE_STATE_DETECTED_FALSE;
                        return;
                    }
                }

                if (allMovedDown) {
                    mThreeGestureState = THREE_GESTURE_STATE_DETECTED_TRUE;
                    if (mCallbacks != null) {
                        mCallbacks.onSwipeThreeFinger();
                    }
                }
            }
        }
    }

    private void startTracking(MotionEvent event) {
        mThreeGestureState = THREE_GESTURE_STATE_DETECTING;
        for (int i = 0; i < MAX_TRACKED_POINTERS; i++) {
            mPointerIds[i] = event.getPointerId(i);
            mInitMotionY[i] = event.getY(i);
            mInitMotionX[i] = event.getX(i);
        }
    }
}
