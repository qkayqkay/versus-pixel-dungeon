package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class GoldSinkRift extends Rift{
    public static final GoldSinkRift INSTANCE = new GoldSinkRift();

    public GoldSinkRift(){
        riftId = "silent_gold_sink_rift";
        silent = false;
        cost = 5;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting gold sink rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
    @Override
    public int icon() {
        return HeroIcon.GOLD_SINK;
    }
}
