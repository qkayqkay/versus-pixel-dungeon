package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Image;

public class BingoTask {
    public final BingoCondition type;
    public final float random; // only used for some conditions
    private boolean completed = false;
    public String id;
    public Player owner = null;

    public BingoTask(BingoCondition type, float random) {
        this.type = type;
        this.id = type.id;
        this.random = random;
    }

    public boolean check(Hero hero) {
        boolean status = type.check(hero, random);
        if(status && completed != true){
            completed = true;
            NetworkManager.INSTANCE.completeBingoTask(this);
            Gamemode.testVictoryCondition();
        } // we don't handle the else case as I don't want it to switch back if the condition isn't met later
        return status;
    }

    public void setCompleted(boolean value) {
        this.completed = value;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Image createIcon() {
        return Icons.get(type.icon);
    }

    public String getLabel() {
        return type.getLabel(random);
    }
}