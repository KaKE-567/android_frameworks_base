/*
 * Copyright (C) 2021 The LineageOS Project
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

package com.android.systemui.gaming;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.systemui.Dependency;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.statusbar.policy.ZenModeController;

import java.util.HashSet;
import java.util.Set;

public class GamingModeController {
    private static final String TAG = "GamingModeController";

    public static final String SETTING_GAMING_MODE_APPS = "gaming_mode_apps";
    public static final String SETTING_GAMING_MODE_DND = "gaming_mode_dnd";
    public static final String SETTING_GAMING_MODE_MISTOUCH = "gaming_mode_mistouch";
    public static final String SETTING_GAMING_MODE_ACTIVE = "gaming_mode_active";

    public static final String DEFAULT_GAMING_APPS =
            "com.pubg.imobile," +
            "com.pubg.krmobile," +
            "com.tencent.ig," +
            "com.vng.pubgmobile," +
            "com.dts.freefireth," +
            "com.dts.freefiremax," +
            "com.activision.callofduty.shooter," +
            "com.riotgames.league.wildrift," +
            "com.miHoYo.GenshinImpact," +
            "com.mobile.legends," +
            "com.epicgames.fortnite," +
            "com.ea.gp.apexlegendsmobilefps," +
            "com.gameloft.android.ANMP.GloftA9HM," +
            "com.carxtech.sr";

    private static GamingModeController sInstance;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Set<String> mGamingApps = new HashSet<>();

    private boolean mIsGamingActive = false;
    private String mActiveGamePackage = null;
    private int mPreviousZenMode = Settings.Global.ZEN_MODE_OFF;
    private boolean mDndTurnedOnByGame = false;

    // Mistouch swipe tracking
    private int mGameBackSwipeCount = 0;
    private long mLastGameBackSwipeTime = 0;

    private boolean mDndEnabled = true;
    private boolean mMistouchEnabled = true;

    public static synchronized GamingModeController init(Context context) {
        if (sInstance == null) {
            sInstance = new GamingModeController(context.getApplicationContext());
        }
        return sInstance;
    }

    public static GamingModeController getInstance() {
        return sInstance;
    }

    private GamingModeController(Context context) {
        mContext = context;
        loadSettings();
        registerSettingsObserver();
        registerActivityListener();
    }

    private synchronized void loadSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        int currentUser = ActivityManager.getCurrentUser();
        String apps = Settings.System.getStringForUser(resolver,
                SETTING_GAMING_MODE_APPS, currentUser);
        if (apps == null) {
            apps = Settings.System.getString(resolver, SETTING_GAMING_MODE_APPS);
        }
        if (apps == null) {
            apps = DEFAULT_GAMING_APPS;
            Settings.System.putStringForUser(resolver,
                    SETTING_GAMING_MODE_APPS, DEFAULT_GAMING_APPS, currentUser);
        }

        mGamingApps.clear();
        if (!TextUtils.isEmpty(apps)) {
            for (String pkg : apps.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    mGamingApps.add(trimmed);
                }
            }
        }

        mDndEnabled = Settings.System.getIntForUser(resolver,
                SETTING_GAMING_MODE_DND, 1, currentUser) == 1 &&
                Settings.System.getInt(resolver, SETTING_GAMING_MODE_DND, 1) == 1;
        mMistouchEnabled = Settings.System.getIntForUser(resolver,
                SETTING_GAMING_MODE_MISTOUCH, 1, currentUser) == 1 &&
                Settings.System.getInt(resolver, SETTING_GAMING_MODE_MISTOUCH, 1) == 1;

        Log.d(TAG, "Loaded gaming apps: " + mGamingApps.size() + ", dnd: " + mDndEnabled + ", mistouch: " + mMistouchEnabled);
    }

    private void registerSettingsObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        ContentObserver observer = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                loadSettings();
                checkForegroundTask();
            }
        };

        resolver.registerContentObserver(
                Settings.System.getUriFor(SETTING_GAMING_MODE_APPS),
                false, observer, UserHandle.USER_ALL);
        resolver.registerContentObserver(
                Settings.System.getUriFor(SETTING_GAMING_MODE_DND),
                false, observer, UserHandle.USER_ALL);
        resolver.registerContentObserver(
                Settings.System.getUriFor(SETTING_GAMING_MODE_MISTOUCH),
                false, observer, UserHandle.USER_ALL);
    }

    private void registerActivityListener() {
        ActivityManagerWrapper.getInstance().registerTaskStackListener(new TaskStackChangeListener() {
            @Override
            public void onTaskStackChanged() {
                mHandler.post(() -> checkForegroundTask());
            }

            @Override
            public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
                mHandler.post(() -> checkForegroundTask());
            }

            @Override
            public void onTaskCreated(int taskId, ComponentName componentName) {
                mHandler.post(() -> checkForegroundTask());
            }
        });

        mHandler.post(() -> checkForegroundTask());
    }

    private synchronized void checkForegroundTask() {
        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
        ComponentName topActivity = runningTask == null ? null : runningTask.topActivity;
        String currentPkg = topActivity == null ? null : topActivity.getPackageName();
        boolean isGame = currentPkg != null && isGamingApp(currentPkg);

        if (isGame) {
            if (!mIsGamingActive || !TextUtils.equals(mActiveGamePackage, currentPkg)) {
                mIsGamingActive = true;
                mActiveGamePackage = currentPkg;
                Settings.System.putInt(mContext.getContentResolver(), SETTING_GAMING_MODE_ACTIVE, 1);
                Log.d(TAG, "Entered game mode for: " + currentPkg);

                if (mDndEnabled) {
                    enableDnd();
                }
            }
        } else {
            if (mIsGamingActive) {
                mIsGamingActive = false;
                mActiveGamePackage = null;
                Settings.System.putInt(mContext.getContentResolver(), SETTING_GAMING_MODE_ACTIVE, 0);
                Log.d(TAG, "Exited game mode");

                if (mDndTurnedOnByGame) {
                    disableDnd();
                }
            }
        }
    }

    private void enableDnd() {
        try {
            ZenModeController zenController = Dependency.get(ZenModeController.class);
            if (zenController != null) {
                mPreviousZenMode = zenController.getZen();
                if (mPreviousZenMode == Settings.Global.ZEN_MODE_OFF) {
                    mDndTurnedOnByGame = true;
                    zenController.setZen(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
                    Log.d(TAG, "Auto DND enabled for game");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable DND", e);
        }
    }

    private void disableDnd() {
        try {
            ZenModeController zenController = Dependency.get(ZenModeController.class);
            if (zenController != null) {
                zenController.setZen(mPreviousZenMode, null, TAG);
                Log.d(TAG, "Auto DND restored to previous mode: " + mPreviousZenMode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore DND", e);
        } finally {
            mDndTurnedOnByGame = false;
        }
    }

    public synchronized boolean isGamingApp(String packageName) {
        return packageName != null && mGamingApps.contains(packageName);
    }

    public synchronized boolean isGamingActive() {
        return mIsGamingActive;
    }

    public boolean isMistouchEnabled() {
        return mMistouchEnabled;
    }

    public boolean isDndEnabled() {
        return mDndEnabled;
    }

    /**
     * Called when a back swipe/action occurs while in game mode.
     * Returns true if back action should be blocked/intercepted, false if allowed.
     */
    public synchronized boolean shouldBlockForGamingMistouch() {
        ContentResolver resolver = mContext.getContentResolver();
        int currentUser = ActivityManager.getCurrentUser();
        boolean mistouchUser = Settings.System.getIntForUser(resolver,
                SETTING_GAMING_MODE_MISTOUCH, 1, currentUser) == 1;
        boolean mistouchGlobal = Settings.System.getInt(resolver,
                SETTING_GAMING_MODE_MISTOUCH, 1) == 1;
        if (!mistouchUser || !mistouchGlobal || !mMistouchEnabled) {
            mGameBackSwipeCount = 0;
            return false;
        }

        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
        ComponentName topActivity = runningTask == null ? null : runningTask.topActivity;
        String currentPkg = topActivity == null ? null : topActivity.getPackageName();
        if (currentPkg == null || !isGamingApp(currentPkg)) {
            mGameBackSwipeCount = 0;
            mIsGamingActive = false;
            return false;
        }

        long now = SystemClock.uptimeMillis();
        if (now - mLastGameBackSwipeTime > 2500) {
            mGameBackSwipeCount = 0;
        }
        mGameBackSwipeCount++;
        mLastGameBackSwipeTime = now;

        if (mGameBackSwipeCount < 3) {
            final int remaining = 3 - mGameBackSwipeCount;
            mHandler.post(() -> {
                Toast.makeText(mContext,
                        "Swipe " + remaining + " more time" + (remaining > 1 ? "s" : "") + " to exit",
                        Toast.LENGTH_SHORT).show();
            });
            return true;
        }

        // 3rd swipe within 2.5 seconds: allow exit and reset count
        mGameBackSwipeCount = 0;
        return false;
    }
}
