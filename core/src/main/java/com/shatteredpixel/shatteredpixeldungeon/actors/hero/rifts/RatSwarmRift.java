package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class RatSwarmRift extends Rift{
    public static final RatSwarmRift INSTANCE = new RatSwarmRift();

    public RatSwarmRift(){
        riftId = "rat_swarm_rift";
        silent = false;
        cost = 3;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting rat swarm rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
    @Override
    public int icon() {
        return HeroIcon.RAT_SWARM;
    }

}
