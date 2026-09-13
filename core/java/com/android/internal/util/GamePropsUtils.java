/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2021-2022 crDroid Android Project
 *               2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GamePropsUtils {

    private static final String TAG = "GamePropsUtils";
    private static final boolean DEBUG = false;

    // Profile: Redmi K30 Ultra (M2006J10C / cezanne)
    // Unlocks 60 FPS Extreme & HDR graphics in BGMI / PUBG Mobile without 90 FPS stutter
    private static final Map<String, Object> propsToChangeK30Ultra;

    // Profile: Google Pixel XL (marlin)
    // Grants unlimited original quality Google Photos backup
    private static final Map<String, Object> propsToChangePixelXL;
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final String[] packagesToChangeK30Ultra = {
            // Battlegrounds Mobile India (BGMI) & PUBG Mobile (Global / Regional) - 60 FPS Extreme & HDR
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.pubg.krmobile",
            "com.vng.pubgmobile",
            "com.rekoo.pubgm",
            "com.tencent.tmgp.pubgmhd",         // Game for Peace
            "com.pubg.newstate",

            // Call of Duty: Mobile (All regional releases) - 60 FPS Max & High Quality
            "com.activision.callofduty.shooter",
            "com.activision.callofduty.warzone",
            "com.tencent.tmgp.kr.codm",
            "com.garena.game.codm",
            "com.vng.codmvn",

            // League of Legends: Wild Rift & Teamfight Tactics - 60 FPS
            "com.riotgames.league.wildrift",
            "com.riotgames.league.wildrifttw",
            "com.riotgames.league.wildriftvn",
            "com.riotgames.league.teamfighttactics",
            "com.riotgames.league.teamfighttacticstw",
            "com.riotgames.league.teamfighttacticsvn",

            // Competitive Tactical Shooters - 60 FPS
            "com.netease.bloodstrike",
            "com.proximabeta.mf.uamo",          // Arena Breakout
            "com.criticalforceentertainment.criticalops",
            "com.axlebolt.standoff2",
            "com.xd.t3.global",                 // T3 Arena
            "com.ea.gp.apexlegendsmobilefps",

            // Supercell Games - 60 FPS
            "com.supercell.brawlstars",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",

            // Mobile Legends: Bang Bang & HoYoverse - 60 FPS
            "com.mobile.legends",
            "com.miHoYo.GenshinImpact",
            "com.miHoYo.Yuanshen",
            "com.miHoYo.enterprise.NGHServer",
            "com.HoYoverse.hkrpgoversea",
            "com.miHoYo.hkrpg",

            // Fortnite & Epic Games - 60 FPS
            "com.epicgames.fortnite",
            "com.epicgames.portal",

            // MOBAs & Arena Shooters
            "com.levelinfinite.sgameGlobal",    // Honor of Kings
            "com.tencent.tmgp.sgame",           // Arena of Valor
            "com.garena.game.kgvn",
            "com.garena.game.kgtw",
            "com.garena.game.kgid",
            "com.garena.game.kgth",
            "com.netease.lztgglobal",           // Cyber Hunter
            "com.madfingergames.legends",       // Shadowgun Legends

            // Sports, Racing & Action
            "com.ea.gp.fifamobile",             // EA Sports FC / FIFA Mobile
            "com.gameloft.android.ANMP.GloftA9HM", // Asphalt 9
            "com.carxtech.sr",                  // CarX Street
            "com.ea.games.r3_row",              // Real Racing 3
            "com.ea.game.nfs14_row",            // Need for Speed No Limits
            "com.catdaddy.nba2km",              // NBA 2K Mobile
            "com.pearlabyss.blackdesertm",      // Black Desert Mobile
            "com.pearlabyss.blackdesertm.gl",
            "com.netease.dbdmobile",            // Dead by Daylight Mobile
            "com.nekki.shadowfight3",           // Shadow Fight 3
            "com.nekki.shadowfightarena"        // Shadow Fight 4
    };

    static {
        propsToChangeK30Ultra = new HashMap<>();
        propsToChangeK30Ultra.put("BRAND", "Redmi");
        propsToChangeK30Ultra.put("MANUFACTURER", "Xiaomi");
        propsToChangeK30Ultra.put("DEVICE", "cezanne");
        propsToChangeK30Ultra.put("PRODUCT", "cezanne");
        propsToChangeK30Ultra.put("MODEL", "M2006J10C");

        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("ID", "QP1A.191005.007.A3");
        propsToChangePixelXL.put("TYPE", "user");
        propsToChangePixelXL.put("TAGS", "release-keys");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    private static boolean isGamePropsEnabled(Context context) {
        if (context != null) {
            try {
                if (Settings.System.getInt(context.getContentResolver(), "game_props_enabled", 1) == 0) {
                    return false;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            if (!SystemProperties.getBoolean("persist.sys.game_props_enabled", true)) {
                return false;
            }
        } catch (Throwable ignored) {
        }

        return true;
    }

    public static void setProps(Context context) {
        if (context == null) {
            return;
        }
        final String packageName = context.getPackageName();
        if (packageName == null) {
            return;
        }

        // Mask custom ROM signatures (LineageOS properties, debuggable, modversion) from non-system apps
        if (!packageName.startsWith("android") && !packageName.startsWith("org.lineageos.")) {
            maskCustomRomProps();
        }

        // Unlimited original quality Google Photos backup (Pixel XL spoofing)
        if (packageName.startsWith("com.google.android.apps.photos")) {
            if (DEBUG) Log.d(TAG, "Defining Pixel XL props for Google Photos: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChangePixelXL.entrySet()) {
                setPropValue(prop.getKey(), prop.getValue());
            }
            return;
        }

        // CRITICAL SAFETY CHECK FOR PLAY INTEGRITY & SYSTEM:
        // NEVER spoof Google Play Services or System packages
        if (packageName.startsWith("com.google.") ||
            packageName.startsWith("com.android.vending") ||
            packageName.equals("android")) {
            return;
        }

        // EXPLICIT SAFETY CHECK FOR FREE FIRE:
        // NEVER spoof Free Fire to avoid ban
        if (packageName.equals("com.dts.freefireth") ||
            packageName.equals("com.dts.freefiremax") ||
            packageName.startsWith("com.dts.freefire")) {
            return;
        }

        // Check if user turned on the Game Spoofing toggle
        if (!isGamePropsEnabled(context)) {
            return;
        }

        Map<String, Object> propsToApply = null;
        if (Arrays.asList(packagesToChangeK30Ultra).contains(packageName)) {
            propsToApply = propsToChangeK30Ultra;
        }

        if (propsToApply == null) {
            return;
        }

        if (DEBUG) Log.d(TAG, "Defining gaming props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToApply.entrySet()) {
            setPropValue(prop.getKey(), prop.getValue());
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
        if (value instanceof String) {
            String strVal = (String) value;
            switch (key) {
                case "BRAND":
                    SystemProperties.setLocalOverride("ro.product.brand", strVal);
                    SystemProperties.setLocalOverride("ro.product.system.brand", strVal);
                    SystemProperties.setLocalOverride("ro.product.vendor.brand", strVal);
                    SystemProperties.setLocalOverride("ro.product.odm.brand", strVal);
                    break;
                case "MANUFACTURER":
                    SystemProperties.setLocalOverride("ro.product.manufacturer", strVal);
                    SystemProperties.setLocalOverride("ro.product.system.manufacturer", strVal);
                    SystemProperties.setLocalOverride("ro.product.vendor.manufacturer", strVal);
                    SystemProperties.setLocalOverride("ro.product.odm.manufacturer", strVal);
                    break;
                case "DEVICE":
                    SystemProperties.setLocalOverride("ro.product.device", strVal);
                    SystemProperties.setLocalOverride("ro.product.system.device", strVal);
                    SystemProperties.setLocalOverride("ro.product.vendor.device", strVal);
                    SystemProperties.setLocalOverride("ro.product.odm.device", strVal);
                    break;
                case "PRODUCT":
                    SystemProperties.setLocalOverride("ro.product.name", strVal);
                    SystemProperties.setLocalOverride("ro.product.system.name", strVal);
                    SystemProperties.setLocalOverride("ro.product.vendor.name", strVal);
                    SystemProperties.setLocalOverride("ro.product.odm.name", strVal);
                    break;
                case "MODEL":
                    SystemProperties.setLocalOverride("ro.product.model", strVal);
                    SystemProperties.setLocalOverride("ro.product.system.model", strVal);
                    SystemProperties.setLocalOverride("ro.product.vendor.model", strVal);
                    SystemProperties.setLocalOverride("ro.product.odm.model", strVal);
                    break;
                case "FINGERPRINT":
                    SystemProperties.setLocalOverride("ro.build.fingerprint", strVal);
                    SystemProperties.setLocalOverride("ro.system.build.fingerprint", strVal);
                    SystemProperties.setLocalOverride("ro.vendor.build.fingerprint", strVal);
                    SystemProperties.setLocalOverride("ro.product.build.fingerprint", strVal);
                    SystemProperties.setLocalOverride("ro.odm.build.fingerprint", strVal);
                    break;
                case "ID":
                    SystemProperties.setLocalOverride("ro.build.id", strVal);
                    break;
                case "TYPE":
                    SystemProperties.setLocalOverride("ro.build.type", strVal);
                    break;
                case "TAGS":
                    SystemProperties.setLocalOverride("ro.build.tags", strVal);
                    break;
            }
        }
    }

    private static void maskCustomRomProps() {
        SystemProperties.setLocalOverride("ro.lineage.version", "");
        SystemProperties.setLocalOverride("ro.lineage.releasetype", "");
        SystemProperties.setLocalOverride("ro.lineage.build.version", "");
        SystemProperties.setLocalOverride("ro.lineage.display.version", "");
        SystemProperties.setLocalOverride("ro.vortex.version", "");
        SystemProperties.setLocalOverride("ro.vortexos.version", "");
        SystemProperties.setLocalOverride("ro.modversion", "");
        SystemProperties.setLocalOverride("ro.lineagelegal.url", "");
        SystemProperties.setLocalOverride("ro.lineage.build.version.plat.sdk", "");
        SystemProperties.setLocalOverride("ro.lineage.build.version.plat.rev", "");
        SystemProperties.setLocalOverride("ro.debuggable", "0");
        SystemProperties.setLocalOverride("ro.build.type", "user");
        SystemProperties.setLocalOverride("ro.build.tags", "release-keys");
    }

    public static boolean isGamePackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String pkg : packagesToChangeK30Ultra) {
            if (pkg.equals(packageName)) {
                return true;
            }
        }
        if (packageName.startsWith("com.pubg.")
                || packageName.startsWith("com.tencent.ig")
                || packageName.startsWith("com.tencent.tmgp")
                || packageName.startsWith("com.vng.pubg")
                || packageName.startsWith("com.rekoo.pubg")
                || packageName.startsWith("com.epicgames.")
                || packageName.startsWith("com.riotgames.")
                || packageName.startsWith("com.dts.freefire")
                || packageName.startsWith("com.activision.callofduty")
                || packageName.startsWith("com.miHoYo.")
                || packageName.startsWith("com.HoYoverse.")
                || packageName.startsWith("com.krafton.")
                || packageName.toLowerCase().contains("pubg")
                || packageName.toLowerCase().contains("codm")) {
            return true;
        }
        return false;
    }

    public static boolean isTelegramPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        String lower = packageName.toLowerCase();
        return lower.contains("telegram")
                || lower.contains("challegram")
                || lower.contains("plusmess")
                || lower.equals("org.telegram.plus")
                || lower.contains("nekox")
                || lower.contains("nekogram")
                || lower.contains("exteragram")
                || lower.contains("ayugram")
                || lower.contains("forkgram")
                || lower.contains("nagram")
                || lower.contains("cherrygram")
                || lower.contains("catogram")
                || lower.contains("swiftgram")
                || lower.contains("mdgram")
                || lower.contains("octogram");
    }

    public static boolean isStorageExemptPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        if (isGamePackage(packageName) || isTelegramPackage(packageName)) {
            return true;
        }
        if (packageName.equals("com.android.camera") || packageName.equals("com.miui.extraphoto")) {
            return true;
        }
        return false;
    }

    public static boolean isGame(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        if (isGamePackage(appInfo.packageName)) {
            return true;
        }
        if (appInfo.category == ApplicationInfo.CATEGORY_GAME
                || (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true;
        }
        return false;
    }

    private static final String[] SENSITIVE_PACKAGES = {
        "bin.mt.termex",
        "org.lsposed.manager",
        "org.lsposed.lspatch",
        "de.robv.android.xposed.installer",
        "io.github.vvb2060.magisk",
        "com.topjohnwu.magisk",
        "me.weishu.exp",
        "me.simpleHook",
        "top.hookvip.pro",
        "Hook.JiuWu.Xp",
        "com.bug.hookvip",
        "lin.xposed",
        "com.tsng.hidemyapplist",
        "com.tsng.pzyhrx.hma",
        "com.topmiaohan.hidebllist",
        "zako.zako.zako",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.rifsxd.ksunext",
        "io.github.a13e300.ksuwebui",
        "com.sukisu.ultra",
        "com.resukisu.resukisu",
        "lucky.patcher",
        "com.chelpus.lackypatch",
        "com.android.vending.billing.InAppBillingService.LUCK",
        "ru.maximoff.apktool",
        "moe.shizuku.privileged.api",
        "com.omarea.vtools",
        "com.didjdk.adbhelper",
        "com.xayah.databackup.foss",
        "es.chiteroman.bootloaderspoofer"
    };

    public static boolean isSensitivePackageForCaller(String targetPkgName, String callingPkgName, int callingUid) {
        if (targetPkgName == null) {
            return false;
        }
        if (callingPkgName != null && isGamePackage(callingPkgName)) {
            for (String pkg : SENSITIVE_PACKAGES) {
                if (pkg.equals(targetPkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSensitivePackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String pkg : SENSITIVE_PACKAGES) {
            if (pkg.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
