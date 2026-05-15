package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

public class StartFreeze extends FlavourBuff {

    private long endTime = Long.MAX_VALUE;


    public void setEndTime(long endTimeMs) {
        endTime = endTimeMs;
    }

    @Override
    public boolean attachTo(Char target) {
        if (super.attachTo(target)) {
            target.invisible++;
            target.paralysed++;

            Emitter.freezeEmitters = true;
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.sprite != null) {
                    mob.sprite.add(CharSprite.State.PARALYSED);
                }
            }
            Dungeon.observe();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean act() {
        Hunger hunger = target.buff(Hunger.class);
        if (hunger != null && !hunger.isStarving()) {
            hunger.satisfy(100f);
        }
        spend(TICK);
        return true;
    }

    public boolean shouldDetach() {
        return Game.realTime >= endTime;
    }

    @Override
    public int icon() {
        return BuffIndicator.TIME;
    }

    @Override
    public void tintIcon(Image icon) {
        icon.hardlight(0.85f, 0f, 0.35f);
    }

    @Override
    public void detach() {
        if (target.invisible > 0) target.invisible--;
        if (target.paralysed > 0) target.paralysed--;

        Emitter.freezeEmitters = false;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.paralysed <= 0) {
                mob.sprite.remove(CharSprite.State.PARALYSED);
            }
        }

        Dungeon.observe();
        super.detach();
    }
}