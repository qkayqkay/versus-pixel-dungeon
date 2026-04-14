package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

public class WndDisconnected extends WndTitledMessage {
    public WndDisconnected(String message) {
        super(Icons.WARNING.get(), "Disconnected", message);
    }
}