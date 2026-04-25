package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

public class LockdownRift extends Rift{
    public static LockdownRift INSTANCE = new LockdownRift();
    public LockdownRift(){
        riftId = "lockdown_rift";
        cost = 3;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting lockdown rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
}
