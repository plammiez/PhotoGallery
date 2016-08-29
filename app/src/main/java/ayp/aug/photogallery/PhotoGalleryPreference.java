package ayp.aug.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Waraporn on 8/19/2016.
 */
public class PhotoGalleryPreference {

    private static final String TAG = "PhotoGalleryPref";
    protected static final String PREF_SEARCH_KEY = "PhotoGalleryPref";
    protected static final String PREF_LAST_ID = "PREF_LAST_ID";
    protected static final String PREF_IS_ALARM_ON = "PREF_ALARM_ON";

    public static SharedPreferences getSP(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    public static String getStoredSearchKey(Context context) {
        return getSP(context).getString(PREF_SEARCH_KEY, null);
    }

    public static void setStoredSearchKey(Context context, String key) {
        getSP(context).edit()
                .putString(PREF_SEARCH_KEY, key)
                .apply();
    }

    public static String getStoredLastId(Context context) {
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return getSP(context).getString(PREF_LAST_ID, null);
    }

    public static void setStoredLastId(Context context, String key) {
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        getSP(context).edit()
                .putString(PREF_LAST_ID, key)
                .apply();
    }

    public static Boolean getStoredAlarmOn(Context context) {
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return getSP(context).getBoolean(PREF_IS_ALARM_ON, false);
    }

    public static void setStoredAlarmOn(Context context, Boolean isAlarmOn) {
        getSP(context).edit()
                .putBoolean(PREF_IS_ALARM_ON, isAlarmOn)
                .apply();
    }
}
