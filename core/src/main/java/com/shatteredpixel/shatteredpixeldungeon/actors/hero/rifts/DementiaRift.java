package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

public class DementiaRift extends Rift{
    public static final DementiaRift INSTANCE = new DementiaRift();

    private DementiaRift() {
        riftId = "dementia_rift";
        cost = 8;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting dementia rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }

}

