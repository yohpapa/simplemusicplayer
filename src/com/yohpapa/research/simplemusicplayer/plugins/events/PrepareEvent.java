package com.yohpapa.research.simplemusicplayer.plugins.events;

/**
 * Created by YohPapa on 2013/08/16.
 */
public class PrepareEvent {
    private final long[] trackIds;
    private final int startIndex;

    public PrepareEvent(long[] trackIds, int startIndex) {
        this.trackIds = trackIds;
        this.startIndex = startIndex;
    }

    public long[] getTrackIds() {
        return trackIds;
    }

    public int getStartIndex() {
        return startIndex;
    }
}
