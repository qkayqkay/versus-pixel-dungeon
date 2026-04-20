package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

public class RespawnCountdown extends FlavourBuff {
 // I am largely copying stuff over from TimeStasis.java
    {
        type = Buff.buffType.POSITIVE;
        actPriority = BUFF_PRIO-3; //acts after all other buffs, so they are prevented
    }
    private long endTime = -1;

    private static final long DURATION_MS = 20000; // 20 real seconds

    public void setEndTime() {
        endTime = Game.realTime + DURATION_MS;
    }

    public long getEndTime(){
        return endTime;
    }

    @Override
    public boolean attachTo(Char target) {
        if (super.attachTo(target)) {
            //target.rooted = true;
            System.out.println("Attaching!");


            target.invisible++;
            target.paralysed++;
            target.next();

            if (Dungeon.hero != null) {
                Dungeon.observe();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void spend(float time) {
        super.spend(time);

        //don't punish the player for going into stasis frequently
        Hunger hunger = Buff.affect(target, Hunger.class);
        if (hunger != null && !hunger.isStarving()) {
            hunger.affectHunger(cooldown(), true);
        }
    }

    @Override
    public boolean act() {
        /*if (endTime != -1 && Game.realTime >= endTime) {
            System.out.println("Condition met!");
            detach();
            InterlevelScene.mode = InterlevelScene.Mode.MULTIPLAYER_RESPAWN;
            Game.switchScene(InterlevelScene.class);
        }*/
        spend(TICK);
        return true;
    }

    @Override
    public void detach() {
        //target.rooted = false;
        System.out.println("RespawnCountdown detached! Stack trace:");
        Thread.currentThread().dumpStack();
        if (target.invisible > 0) target.invisible--;
        if (target.paralysed > 0) target.paralysed--;
        Buff.affect( target, Invisibility.class, 2 );
        super.detach();
        Dungeon.observe();
    }

    @Override
    public int icon() {
        return BuffIndicator.CORRUPT;
    }

    private static final String END_TIME = "end_time";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(END_TIME, endTime);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        endTime = bundle.getLong(END_TIME);
    }
}