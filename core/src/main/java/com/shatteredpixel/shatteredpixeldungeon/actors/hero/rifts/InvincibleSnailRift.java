package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.InvincibleSnail;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

public class InvincibleSnailRift extends Rift{
        public static final InvincibleSnailRift INSTANCE = new InvincibleSnailRift();

        public InvincibleSnailRift(){
            riftId = "invincible_snail_rift";
            silent = false;
            cost = 7;
        }

        @Override
        public void onCast(Hero hero) {
            System.out.println("casting invincible snail rift!");
            //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
            super.onCast(hero);
        }

        @Override
        public int icon() {
            return HeroIcon.INVICIBLE_SNAIL;
        }
    }
