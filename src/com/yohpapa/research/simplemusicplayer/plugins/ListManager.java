/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package com.yohpapa.research.simplemusicplayer.plugins;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.yohpapa.research.simplemusicplayer.plugins.tools.CursorHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ListManager extends CordovaPlugin {

    /**
     * @see CordovaPlugin
     * @param action
     * @param args
     * @param callbackContext
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if("get_album_info".equals(action)) {
            final Context context = cordova.getActivity().getApplicationContext();

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ContentResolver resolver = context.getContentResolver();
                    Cursor cursor = null;
                    try {
                        cursor = resolver.query(
                                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                    new String[] {
                                        MediaStore.Audio.Albums._ID,
                                        MediaStore.Audio.Albums.ALBUM,
                                        MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                                        MediaStore.Audio.Albums.ARTIST,
                                        MediaStore.Audio.Albums.ALBUM_ART,
                                    },
                                    null, null, MediaStore.Audio.Albums.ALBUM + " ASC");

                        if(cursor == null || !cursor.moveToFirst()) {
                            callbackContext.error("The cursor is invalid.");
                            return;
                        }

                        JSONArray results = new JSONArray();
                        do {
                            JSONObject obj = new JSONObject();
                            obj.put("id", CursorHelper.getLong(cursor, MediaStore.Audio.Albums._ID));
                            obj.put("name", CursorHelper.getString(cursor, MediaStore.Audio.Albums.ALBUM));
                            obj.put("numTracks", CursorHelper.getInt(cursor, MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                            obj.put("artist", CursorHelper.getString(cursor, MediaStore.Audio.Albums.ARTIST));
                            obj.put("artwork", CursorHelper.getString(cursor, MediaStore.Audio.Albums.ALBUM_ART));

                            results.put(obj);

                        } while(cursor.moveToNext());

                        callbackContext.success(results);

                    } catch(JSONException e) {
                        callbackContext.error(e.toString());
                    } finally {
                        if(cursor != null) {
                            cursor.close();
                        }
                    }
                }
            });
            return true;

        } else if("get_track_info".equals(action)) {
            final long albumId = args.getLong(0);
            final Context context = cordova.getActivity().getApplicationContext();

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ContentResolver resolver = context.getContentResolver();
                    Cursor albumCursor = null;
                    Cursor trackCursor = null;

                    try {
                        albumCursor = resolver.query(
                                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                        new String[] {
                                            MediaStore.Audio.Albums.ALBUM,
                                            MediaStore.Audio.Albums.ALBUM_ART,
                                        },
                                        MediaStore.Audio.Albums._ID + "=?", new String[] {String.valueOf(albumId)},
                                        null);

                        if(albumCursor == null || !albumCursor.moveToFirst() || albumCursor.getCount() != 1) {
                            callbackContext.error("The album's cursor is invalid.");
                            return;
                        }

                        trackCursor = resolver.query(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                new String[] {
                                        MediaStore.Audio.Media._ID,
                                        MediaStore.Audio.Media.TITLE,
                                        MediaStore.Audio.Media.ARTIST,
                                        MediaStore.Audio.Media.ALBUM,
                                        MediaStore.Audio.Media.DURATION,
                                },
                                MediaStore.Audio.Media.ALBUM_ID + "=?", new String[] {String.valueOf(albumId)},
                                MediaStore.Audio.Media.TRACK + " ASC");

                        if(trackCursor == null || !trackCursor.moveToFirst()) {
                            callbackContext.error("The cursor is invalid.");
                            return;
                        }

                        JSONObject result = new JSONObject();
                        result.put("album", CursorHelper.getString(albumCursor, MediaStore.Audio.Albums.ALBUM));
                        result.put("artwork", CursorHelper.getString(albumCursor, MediaStore.Audio.Albums.ALBUM_ART));

                        JSONArray tracks = new JSONArray();
                        do {
                            JSONObject obj = new JSONObject();
                            obj.put("id", CursorHelper.getLong(trackCursor, MediaStore.Audio.Media._ID));
                            obj.put("title", CursorHelper.getString(trackCursor, MediaStore.Audio.Media.TITLE));
                            obj.put("artist", CursorHelper.getString(trackCursor, MediaStore.Audio.Media.ARTIST));
                            obj.put("duration", CursorHelper.getLong(trackCursor, MediaStore.Audio.Media.DURATION));

                            tracks.put(obj);

                        } while(trackCursor.moveToNext());

                        result.put("tracks", tracks);
                        callbackContext.success(result);

                    } catch(JSONException e) {
                        callbackContext.error(e.toString());
                    } finally {
                        if(albumCursor != null) {
                            albumCursor.close();
                        }
                        if(trackCursor != null) {
                            trackCursor.close();
                        }
                    }
                }
            });
            return true;
        }

        return false;
    }
}
