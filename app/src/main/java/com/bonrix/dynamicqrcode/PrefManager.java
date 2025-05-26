package com.bonrix.dynamicqrcode;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefManager {
    public static SharedPreferences mPrefs;
    public static String PREF_PACKET_SIZE = "packetsize";
    public static String PREF_IS_INTERVAL = "isinterval";
    public static String PREF_FIRST_PACKET_INTERVAL = "first_packettimeinterval";
    public static String PREF_PACKET_INTERVAL = "packettimeinterval";


    public static void savePref(Context context, String key, String value) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = mPrefs.edit();
        e.putString(key, value);
        e.commit();
    }

    public static void saveBoolPref(Context context, String key, Boolean value) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = mPrefs.edit();
        e.putBoolean(key, value);
        e.commit();
    }

    public static String getPref(Context context, String key) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String value = mPrefs.getString(
                key, "");
        return value;
    }

    public static Boolean getBoolPref(Context context, String key) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Boolean value = mPrefs.getBoolean(
                key, false);
        return value;
    }
    public static Integer getIntPref(Context context, String key) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        int value = mPrefs.getInt(
                key, 0);
        return value;
    }
    public static void saveIntPref(Context context, String key, int value) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = mPrefs.edit();
        e.putInt(key, value);
        e.commit();
    }
}
