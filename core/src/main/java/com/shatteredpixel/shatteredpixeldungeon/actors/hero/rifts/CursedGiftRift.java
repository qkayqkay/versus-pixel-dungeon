package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class CursedGiftRift extends Rift{
    public static CursedGiftRift INSTANCE = new CursedGiftRift();
    public CursedGiftRift(){
        riftId = "cursed_gift_rift";
        cost = 7;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting cursed gift rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }
    @Override
    public int icon() {
        return HeroIcon.CURSED_GIFT;
    }
}
