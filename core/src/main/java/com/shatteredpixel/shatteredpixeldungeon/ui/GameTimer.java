package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;

import java.awt.*;

import static com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.defaultZoom;

public class GameTimer extends Component {
    public static GameTimer instance;

    private RenderedTextBlock timerText;
    private static long startTimeMs = -1;  // time when timer began
    private static long pausedAtMs = -1;   // when we paused
    private static long totalPausedMs = 0; // total ms spent paused
    private boolean stopped = false;

    public GameTimer(float initialTimeSeconds) {
        instance = this;
        if (startTimeMs == -1) { // set the starttime
            startTimeMs = System.currentTimeMillis() - (long)(initialTimeSeconds * 1000);
        }
    }

    public GameTimer() {
        this(0f);
    }

    public float getElapsedSeconds() {
        if (startTimeMs == -1) return 0f;
        long now = (pausedAtMs != -1) ? pausedAtMs : System.currentTimeMillis();
        return (now - startTimeMs - totalPausedMs) / 1000f;
    }

    @Override
    protected void createChildren() {
        timerText = PixelScene.renderTextBlock("", 9);
        add(timerText);
    }

    @Override
    protected void layout() {
        timerText.setPos(x, y);
    }

    @Override
    public void update() {
        super.update();
        float elapsed = getElapsedSeconds();

        int hours   = (int)(elapsed / 3600);
        int minutes = (int)((elapsed % 3600) / 60);
        int seconds = (int)(elapsed % 60);

        timerText.text(String.format("%02dh:%02dm:%02ds", hours, minutes, seconds));
        layout();
    }

    public float getGameTime() {
        return getElapsedSeconds();
    }

    public void pauseTimer() {
        if (pausedAtMs == -1) {
            pausedAtMs = System.currentTimeMillis();
        }
    }

    public boolean resumeTimer() {
        if (stopped) return false;
        if (pausedAtMs != -1) {
            totalPausedMs += System.currentTimeMillis() - pausedAtMs;
            pausedAtMs = -1;
        }
        return true;
    }

    public float stopTimer() {
        float elapsed = getElapsedSeconds();
        stopped = true;
        return elapsed;
    }

    public static void reset() {
        startTimeMs = -1;
        pausedAtMs = -1;
        totalPausedMs = 0;
    }
}