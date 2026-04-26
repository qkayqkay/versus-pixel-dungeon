package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class DementiaRift extends Rift{
    public static final DementiaRift INSTANCE = new DementiaRift();

    public DementiaRift(){
        riftId = "silent_dementia_rift";
        silent = true;
        cost = 5;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting dementia rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
    @Override
    public int icon() {
        return HeroIcon.DEMENTIA;
    }
}
