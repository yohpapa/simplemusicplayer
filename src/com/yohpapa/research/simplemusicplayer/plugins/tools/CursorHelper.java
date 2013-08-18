package com.yohpapa.research.simplemusicplayer.plugins.tools;

import android.database.Cursor;
import android.text.TextUtils;

/**
 * Created by YohPapa on 2013/08/11.
 */
public class CursorHelper {

    public static String getString(Cursor cursor, String key) {
        if(cursor == null || cursor.isClosed() || TextUtils.isEmpty(key))
            return null;

        return cursor.getString(cursor.getColumnIndex(key));
    }

    public static long getLong(Cursor cursor, String key) {
        if(cursor == null || cursor.isClosed() || TextUtils.isEmpty(key))
            return -1L;

        return cursor.getLong(cursor.getColumnIndex(key));
    }

    public static int getInt(Cursor cursor, String key) {
        if(cursor == null || cursor.isClosed() || TextUtils.isEmpty(key))
            return -1;

        return cursor.getInt(cursor.getColumnIndex(key));
    }
}
