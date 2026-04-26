package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class TrapTriggerRift extends Rift{
    public static TrapTriggerRift INSTANCE = new TrapTriggerRift();
    public TrapTriggerRift(){
        riftId = "trap_trigger_rift";
        cost = 5;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting trap trigger rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }

    @Override
    public int icon() {
        return HeroIcon.INVICIBLE_SNAIL;
    }
}
