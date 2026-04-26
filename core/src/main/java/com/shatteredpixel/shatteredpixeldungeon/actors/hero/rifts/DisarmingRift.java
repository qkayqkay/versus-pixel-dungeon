package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class DisarmingRift extends Rift{
    public static final DisarmingRift INSTANCE = new DisarmingRift();

    public DisarmingRift(){
        riftId = "disarming_rift";
        cost = 10;
    }
    @Override
    public void onCast(Hero hero) {
        System.out.println("casting disarming rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
    @Override
    public int icon() {
        return HeroIcon.DISARMAMENT;
    }
}
