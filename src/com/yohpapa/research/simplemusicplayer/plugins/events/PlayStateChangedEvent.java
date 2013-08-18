/**
 * Created by YohPapa on 2013/08/16.
 */

package com.yohpapa.research.simplemusicplayer.plugins.events;

public class PlayStateChangedEvent {

    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;

    private final int state;
    private final int index;

    public PlayStateChangedEvent(int state, int index) {
        this.state = state;
        this.index = index;
    }

    public int getState() {
        return state;
    }

    public int getIndex() {
        return index;
    }
}
