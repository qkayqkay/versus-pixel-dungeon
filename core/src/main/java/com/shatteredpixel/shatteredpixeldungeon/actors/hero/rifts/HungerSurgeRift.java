package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HungerSurge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

public class HungerSurgeRift extends Rift{
    public static final HungerSurgeRift INSTANCE = new HungerSurgeRift();
    public HungerSurgeRift(){
        riftId = "silent_hunger_surge_rift";
        silent = true;
        cost = 5;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting hunger surge rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
}
