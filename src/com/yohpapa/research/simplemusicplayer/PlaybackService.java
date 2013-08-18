/**
 * Created by YohPapa on 2013/08/15.
 */

package com.yohpapa.research.simplemusicplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.yohpapa.research.simplemusicplayer.plugins.events.PauseEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayPauseEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PlayStateChangedEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.PrepareEvent;
import com.yohpapa.research.simplemusicplayer.plugins.events.TrackChangedEvent;
import com.yohpapa.research.simplemusicplayer.plugins.tools.CursorHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class PlaybackService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private static final String TAG = PlaybackService.class.getSimpleName();
    private static final String URI_BASE = PlaybackService.class.getName() + ".";
    private static final String ACTION_PAUSE = URI_BASE + "ACTION_PAUSE";

    private EventBus eventBus = null;

    private long[] trackIds = null;
    private int currentIndex = -1;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private int state = STATE_IDLE;

    private MediaPlayer player = null;
    private List<Runnable> postProcesses = new ArrayList<Runnable>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        player = new MediaPlayer();
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        eventBus = EventBus.getDefault();
        eventBus.registerSticky(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        eventBus.unregister(this);

        if(player.isLooping()) {
            player.reset();
            player.release();
        }

        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(R.id.notification_id);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    public void onEventMainThread(PrepareEvent event) {
        Log.d(TAG, "onEventMainThread: PrepareEvent");

        if(event == null) {
            Log.d(TAG, "The event is null.");
            return;
        }

        int newIndex = event.getStartIndex();
        long[] newTrackIds = event.getTrackIds();

        if(newTrackIds == null) {
            Log.d(TAG, "The new track ID list is null.");
            return;
        }

        if(newIndex >= newTrackIds.length) {
            Log.d(TAG, "The new index is over.");
            return;
        }

        if(trackIds != null) {
            long newTrackId = newTrackIds[newIndex];
            long nowTrackId = trackIds[currentIndex];

            if(nowTrackId == newTrackId) {
                Log.d(TAG, "The track ID is not changed.");
                trackIds = newTrackIds;
                currentIndex = newIndex;
                return;
            }
        }

        trackIds = newTrackIds;
        currentIndex = newIndex;

        prepareToPlay(trackIds[currentIndex]);
    }

    private void prepareToPlay(long trackId) {
        Log.d(TAG, "prepareToPlay trackId: " + trackId);

        try {
            player.reset();
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackIds[currentIndex]);
            player.setDataSource(this, uri);
            player.prepareAsync();

            state = STATE_PREPARING;

        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");

        for(Runnable postProcess : postProcesses) {
            postProcess.run();
        }
        postProcesses.clear();

        state = STATE_PREPARED;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");

        currentIndex = (currentIndex + 1) % trackIds.length;
        prepareToPlay(trackIds[currentIndex]);
        postProcesses.add(new Runnable() {
            @Override
            public void run() {
                playTrack();
                eventBus.post(new TrackChangedEvent(currentIndex));
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError what: " + what + ", extra" + extra);
        return false;
    }

    public void onEventMainThread(PlayEvent event) {
        Log.d(TAG, "onEventMainThread: PlayEvent");

        if(state != STATE_PREPARED) {
            postProcesses.add(new Runnable() {
                @Override
                public void run() {
                    playTrack();
                }
            });
            return;
        }

        playTrack();
    }

    public void onEventMainThread(PauseEvent event) {
        Log.d(TAG, "onEventMainThread: PauseEvent");

        if(!player.isPlaying()) {
            Log.d(TAG, "The player is not playing now.");
            return;
        }
        pauseTrack();
    }

    public void onEventMainThread(PlayPauseEvent event) {
        Log.d(TAG, "onEventMainThread: PlayPauseEvent");

        if(player.isPlaying()) {
            pauseTrack();
        } else {
            if(state == STATE_PREPARED) {
                playTrack();
            } else {
                postProcesses.add(new Runnable() {
                    @Override
                    public void run() {
                        playTrack();
                    }
                });
            }
        }
    }

    private void playTrack() {
        if(!player.isPlaying()) {
            player.start();
            eventBus.post(new PlayStateChangedEvent(PlayStateChangedEvent.STATE_PLAYING, currentIndex));

            updateNotification();
        }
    }

    private void pauseTrack() {
        if(player.isPlaying()) {
            player.pause();
            eventBus.post(new PlayStateChangedEvent(PlayStateChangedEvent.STATE_PAUSED, currentIndex));
        }

        stopSelf();
    }

    private void updateNotification() {
        eventBus.post(new NotificationPrepareEvent(trackIds[currentIndex]));
    }

    public class NotificationPrepareEvent {
        private final long trackId;
        public NotificationPrepareEvent(long trackId) {
            this.trackId  = trackId;
        }

        public long getTrackId() {
            return trackId;
        }
    }

    public class NotificationPreparedEvent {
        private final long trackId;
        private final String title;
        private final String artist;
        private final String album;
        private final Bitmap artwork;

        public NotificationPreparedEvent(long trackId, String title, String artist, String album, Bitmap artwork) {
            this.trackId = trackId;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.artwork = artwork;
        }

        public long getTrackId() { return trackId; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public Bitmap getArtwork() { return artwork; }
    }

    public void onEventAsync(NotificationPrepareEvent event) {
        Log.d(TAG, "onEventAsync: NotificationPrepareEvent");

        Context context = getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        Resources resources = context.getResources();

        Cursor trackCursor = null;
        Cursor albumCursor = null;
        try {
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, event.getTrackId());
            trackCursor = resolver.query(
                                uri,
                                new String[] {
                                    MediaStore.Audio.Media.TITLE,
                                    MediaStore.Audio.Media.ARTIST,
                                    MediaStore.Audio.Media.ALBUM,
                                    MediaStore.Audio.Media.ALBUM_ID,
                                }, null, null, null);

            if(trackCursor == null || !trackCursor.moveToFirst() || trackCursor.getCount() != 1) {
                eventBus.post(new NotificationPreparedEvent(event.getTrackId(), null, null, null, null));
                return;
            }

            String title = CursorHelper.getString(trackCursor, MediaStore.Audio.Media.TITLE);
            String artist = CursorHelper.getString(trackCursor, MediaStore.Audio.Media.ARTIST);
            String album = CursorHelper.getString(trackCursor, MediaStore.Audio.Media.ALBUM);
            long albumId = CursorHelper.getLong(trackCursor, MediaStore.Audio.Media.ALBUM_ID);
            Bitmap artwork = null;

            if(albumId != -1L) {
                uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId);
                albumCursor = resolver.query(
                                    uri,
                                    new String[] {
                                        MediaStore.Audio.Albums.ALBUM_ART,
                                    }, null, null, null);

                if(albumCursor == null || !albumCursor.moveToFirst() || albumCursor.getCount() != 1) {
                    eventBus.post(new NotificationPreparedEvent(event.getTrackId(), title, artist, album, null));
                    return;
                }

                String artworkPath = CursorHelper.getString(albumCursor, MediaStore.Audio.Albums.ALBUM_ART);
                Bitmap buffer = BitmapFactory.decodeFile(artworkPath);
                artwork = Bitmap.createScaledBitmap(
                                    buffer,
                                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                                    false);
                if(artwork != buffer) {
                    buffer.recycle();
                }
            }

            eventBus.post(new NotificationPreparedEvent(event.getTrackId(), title, artist, album, artwork));
            return;

        } finally {
            if(trackCursor != null) {
                trackCursor.close();
            }
            if(albumCursor != null) {
                albumCursor.close();
            }
        }
    }

    public void onEventMainThread(NotificationPreparedEvent event) {
        Log.d(TAG, "onEventMainThread: NotificationPreparedEvent");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        if(event.getArtwork() != null) {
            builder.setLargeIcon(event.getArtwork());
        }
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle(event.getTitle());
        builder.setContentText(event.getArtist());
        builder.setSubText(event.getAlbum());

        // TODO: It does not work well...
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(ACTION_PAUSE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_media_pause, "PAUSE", pendingIntent);

        intent = new Intent(this, SimpleMusicPlayer.class);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        startForeground(R.id.notification_id, builder.build());
//        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
//        manager.notify(R.id.notification_id, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && ACTION_PAUSE.equals(intent.getAction())) {
            pauseTrack();
            return START_REDELIVER_INTENT;
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
