package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class GoldSink extends Buff {
    public static final int GOLD_PER_TURN = 7;
    public int duration = 30;

    {
        type = buffType.NEGATIVE;
        announced = false;
    }

    @Override
    public boolean act() {
        super.act();
        if (Dungeon.gold > 0) {
            Dungeon.gold = Math.max(0, Dungeon.gold - GOLD_PER_TURN);
        }
        spend(TICK);
        duration--;
        if(duration <= 0){
            this.detach();
        }
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.HASTE;
    }
}
