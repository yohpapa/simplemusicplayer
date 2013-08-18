package com.yohpapa.research.simplemusicplayer.plugins.events;

/**
 * Created by YohPapa on 2013/08/16.
 */
public class TrackChangedEvent {
    private final int index;

    public TrackChangedEvent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
