/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.FileUtils;
import android.os.UserHandle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.IOException;

public final class ThermalUtils {

    private static final String TAG = "ThermalUtils";
    private static final String THERMAL_CONTROL = "thermal_control";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_BATTERY = 1;
    protected static final int STATE_BALANCED = 2;
    protected static final int STATE_GAMING = 3;

    private static final String THERMAL_STATE_DEFAULT = "0";
    private static final String THERMAL_STATE_BATTERY = "8";
    private static final String THERMAL_STATE_BALANCED = "12";
    private static final String THERMAL_STATE_GAMING = "10";

    private static final String THERMAL_BALANCED = "thermal.camera=";
    private static final String THERMAL_BATTERY = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.benchmark=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private SharedPreferences mSharedPrefs;

    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, ThermalService.class),
                UserHandle.CURRENT);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = THERMAL_BATTERY + ":" + THERMAL_BALANCED + ":" + THERMAL_GAMING;
            writeValue(value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_BATTERY:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_BALANCED:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_GAMING:
                modes[2] = modes[2] + packageName + ",";
                break;
        }

        finalString = modes[0] + ":" + modes[1] + ":" + modes[2];

        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_BATTERY;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_BALANCED;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_GAMING;
        }
        return state;
    }

    protected void setDefaultThermalProfile() {
        try {
            FileUtils.stringToFile(THERMAL_SCONFIG, THERMAL_STATE_DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to " + THERMAL_SCONFIG, e);
        }
    }

    protected void setThermalProfile(String packageName) {
        String value = getValue();
        String modes[];
        String state = THERMAL_STATE_DEFAULT;

        if (value != null) {
            modes = value.split(":");

            if (modes[0].contains(packageName + ",")) {
                state = THERMAL_STATE_BATTERY;
            } else if (modes[1].contains(packageName + ",")) {
                state = THERMAL_STATE_BALANCED;
            } else if (modes[2].contains(packageName + ",")) {
                state = THERMAL_STATE_GAMING;
            }
        }

        try {
            FileUtils.stringToFile(THERMAL_SCONFIG, state);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to " + THERMAL_SCONFIG, e);
        }
    }
}
