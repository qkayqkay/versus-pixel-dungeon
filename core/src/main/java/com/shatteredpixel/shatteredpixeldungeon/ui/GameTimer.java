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
    private float elapsed = 0;
    private boolean stopped;
    private boolean paused;

    public GameTimer(float initialTime) {
        instance = this;
        this.elapsed = initialTime;
    }

    public GameTimer(){
        this(0f);
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
        if(!paused) {
            elapsed += Game.elapsed;
        }

        int hours = (int)(elapsed/3600);
        int minutes = (int)(elapsed / 60);
        int seconds = (int)(elapsed % 60);

        timerText.text(String.format("%02dh:%02dm:%02ds", hours, minutes, seconds));
        layout();
    }

    public float getGameTime(){
        return elapsed;
    }

    public void pauseTimer(){
        paused = true;
    }

    public boolean resumeTimer(){
        if(!stopped){
            paused = false;
            return true; //unpausing was successful
        }
        else{
            return false; // timer was already stopped
        }
    }

    public float stopTimer(){
        stopped = true;
        return elapsed;
    }

}