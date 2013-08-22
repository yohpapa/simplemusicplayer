/**
 * Created by YohPapa on 2013/08/14.
 */

package com.yohpapa.research.simplemusicplayer.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yohpapa.research.simplemusicplayer.PlaybackService;
import com.yohpapa.research.simplemusicplayer.plugins.events.PauseEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayPauseEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayStateChangedEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.TrackChangedEvent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;

public class PlaybackController extends CordovaPlugin {
    private static final String TAG = PlaybackController.class.getSimpleName();

    private EventBus eventBus = null;

    private long[] trackList = null;
    private int startIndex = 0;

    private CallbackContext onPlayStateChanged = null;
    private CallbackContext onTrackChanged = null;

//    private String onPlayStateChangedCallbackId = null;

    public PlaybackController() {
        eventBus = EventBus.getDefault();
        eventBus.register(this);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        eventBus = EventBus.getDefault();
        eventBus.register(this);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        eventBus.unregister(this);
    }

    /**
     * @see CordovaPlugin
     * @param action
     * @param args
     * @param callbackContext
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if("setTracks".equals(action)) {
            return executeSetTracks(args, callbackContext);
        } else if("setIndex".equals(action)) {
            return executeSetIndex(args, callbackContext);
        } else if("setPlayStateChangedCallback".equals(action)) {
            return executeSetPlayStateChangedCallback(callbackContext);
        } else if("setTrackChangedCallback".equals(action)) {
            return executeSetTrackChangedCallback(callbackContext);
        } else if("playTrack".equals(action)) {
            return executePlayTrack(callbackContext);
        } else if("pauseTrack".equals(action)) {
            return executePauseTrack(callbackContext);
        } else if("togglePlayPause".equals(action)) {
            return executeTogglePlayPause(callbackContext);
        } else if("nextTrack".equals(action)) {
            return true;
        } else if("prevTrack".equals(action)) {
            return true;
        }

        return false;
    }

    private boolean executeSetTracks(JSONArray args, CallbackContext callbackContext) throws JSONException {
        trackList = new long[args.length()];
        for(int i = 0; i < trackList.length; i ++) {
            trackList[i] = args.getLong(i);
        }
        callbackContext.success();
        return true;
    }

    private boolean executeSetIndex(JSONArray args, CallbackContext callbackContext) throws JSONException {
        startIndex = args.getInt(0);

        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_SELECT);
        intent.putExtra(PlaybackService.PRM_START_INDEX, startIndex);
        intent.putExtra(PlaybackService.PRM_TRACK_LIST, trackList);
        ComponentName name = context.startService(intent);
        if(name != null) {
            callbackContext.success();
        } else {
            callbackContext.error("context.startService has been failed.");
        }

        return true;
    }

    private boolean executePlayTrack(CallbackContext callbackContext) {
        eventBus.post(new PlayEvent());
        callbackContext.success();
        return true;
    }

    private boolean executePauseTrack(CallbackContext callbackContext) {
        eventBus.post(new PauseEvent());
        callbackContext.success();
        return true;
    }

    private boolean executeSetPlayStateChangedCallback(CallbackContext callbackContext) {
        onPlayStateChanged = callbackContext;
//        onPlayStateChangedCallbackId = callbackContext.getCallbackId();
        return true;
    }

    private boolean executeSetTrackChangedCallback(CallbackContext callbackContext) {
        onTrackChanged = callbackContext;
        return true;
    }

    private boolean executeTogglePlayPause(CallbackContext callbackContext) {
        eventBus.postSticky(new PlayPauseEvent());
        callbackContext.success();
        return true;
    }

    public void onEvent(PlayStateChangedEvent event) {
        if(onPlayStateChanged != null) {
            try {
                JSONObject parameter = new JSONObject();
                parameter.put("state", event.getState());
                parameter.put("index", event.getIndex());
//                onPlayStateChanged.success(parameter);
                PluginResult result = new PluginResult(PluginResult.Status.OK, parameter);
                result.setKeepCallback(true);
                onPlayStateChanged.sendPluginResult(result);

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void onEvent(TrackChangedEvent event) {
        if(onTrackChanged != null) {
            try {
                JSONObject parameter = new JSONObject();
                parameter.put("index", event.getIndex());
                onTrackChanged.success(parameter);

            } catch(JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
