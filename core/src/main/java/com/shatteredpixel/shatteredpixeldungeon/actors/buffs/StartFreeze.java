package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;

public class StartFreeze extends FlavourBuff {

    private long endTime = Long.MAX_VALUE;


    public void setEndTime(long endTimeMs) {
        endTime = endTimeMs;
    }

    @Override
    public boolean attachTo(Char target) {
        if (super.attachTo(target)) {
            target.rooted = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean act() {
        return true;
    }

    public boolean shouldDetach() {
        return Game.realTime >= endTime;
    }

    @Override
    public int icon() {
        return BuffIndicator.ROOTS;
    }



    @Override
    public void detach() {
        target.rooted = false;
        super.detach();
    }
}