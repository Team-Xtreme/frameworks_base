/*
* Copyright (C) 2017 The Pixel Experience Project
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
package com.android.internal.util.custom;

import android.content.Context;

import android.os.SystemProperties;
import android.content.ContentResolver;
import android.provider.Settings;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.ServiceManager;

import com.android.internal.statusbar.IStatusBarService;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;

public class NavbarUtils {

    public static boolean hasNavbarByDefault(Context context) {
        boolean needsNav = context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            needsNav = false;
        } else if ("0".equals(navBarOverride)) {
            needsNav = true;
        }
        return needsNav;
    }

    public static boolean isNavigationBarEnabled(Context context){
        boolean mHasNavigationBar = false;
        boolean mNavBarOverride = false;

        // Allow a system property to override this. Used by the emulator.
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            mNavBarOverride = true;
        } else if ("0".equals(navBarOverride)) {
            mNavBarOverride = false;
        }
        mHasNavigationBar = !mNavBarOverride && Settings.Secure.getIntForUser(
                context.getContentResolver(), Settings.Secure.NAVIGATION_BAR_ENABLED,
                context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar) ? 1 : 0,
                UserHandle.USER_CURRENT) == 1;

        return mHasNavigationBar;
    }

    public static void setNavigationBarEnabled(Context context, Boolean enabled){
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    public static void reloadNavigationBar(Context context){
        setNavigationBarEnabled(context, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setNavigationBarEnabled(context, true);
            }
        }, 1000);
    }

    public static boolean isNavigationBarLocked(Context context){
        return Settings.Secure.getIntForUser(
                context.getContentResolver(), Settings.Secure.NAVIGATION_BAR_LOCKED, 0,
                UserHandle.USER_CURRENT) == 1;
    }

    public static void lockNavigationBar(Context context){
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_LOCKED, 1, UserHandle.USER_CURRENT);
        FireActions.toggleNavigationBarDirectly(true);
    }

    public static void restoreNavigationBar(Context context, Boolean toggle){
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_LOCKED, 0, UserHandle.USER_CURRENT);
        if (toggle){
            FireActions.toggleNavigationBarDirectly(isNavigationBarEnabled(context));
        }
    }

    private static final class FireActions {
        private static IStatusBarService mStatusBarService = null;
        private static IStatusBarService getStatusBarService() {
            synchronized (FireActions.class) {
                if (mStatusBarService == null) {
                    mStatusBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService("statusbar"));
                }
                return mStatusBarService;
            }
        }

        public static void toggleNavigationBarDirectly(boolean toggle) {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleNavigationBar(toggle);
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }
    }

    public static boolean shouldShowNavbarWhenFingerprintSensorBusy(Context context, String clientPackageName){
        boolean isInLockTaskMode = false;
        try {
            isInLockTaskMode = ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
        }
        if (NavbarUtils.shouldShowNavbarInLockTaskMode(context) && isInLockTaskMode){
            return false;
        }
        KeyguardManager km = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        boolean onKeyguard = km.isKeyguardLocked() || km.inKeyguardRestrictedInputMode();
        boolean fromSystemUI = clientPackageName.equals("com.android.systemui") || clientPackageName.equals("android");
        return context.getResources().getBoolean(com.android.internal.R.bool.config_showNavbarWhenFingerprintSensorBusy) && !onKeyguard && !fromSystemUI;
    }

    public static boolean shouldShowNavbarInLockTaskMode(Context context){
        return context.getResources().getBoolean(com.android.internal.R.bool.config_showNavbarInLockTaskMode);
    }

    public static boolean shouldShowNavbarInKeyguard(Context context){
        return context.getResources().getBoolean(com.android.internal.R.bool.config_showNavbarInKeyguard) && !isNavigationBarEnabled(context);
    }
}