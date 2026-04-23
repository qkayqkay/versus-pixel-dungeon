package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

import java.util.ArrayList;

public abstract class Rift {

    public abstract void onCast(Hero hero);

    public float chargeUse( Hero hero ){
        return 1;
    }

    public boolean canCast( Hero hero ){
        return true;
    }

    public String name(){
        return Messages.get(this, "name");
    }

    public String shortDesc(){
        return Messages.get(this, "short_desc") + " " + Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
    }

    public String desc(){
        return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
    }


    public int icon(){
        return HeroIcon.NONE;
    }

    public void onSpellCast(Hero hero){
        Invisibility.dispel();
        //tome.spendCharge(chargeUse(hero));
        //Talent.onArtifactUsed(hero); what does this do?
    }

    public static ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift> getSpellList(Hero cleric, int tier){
        ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift> spells = new ArrayList<>();

        if (tier == 1) {
            // for example to add one of this tier, do spells.add(riftClass.INSTANCE);

        } else if (tier == 2) {

        } else if (tier == 3){

        }

        return spells;
    }

    public static ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift> getAllSpells() {
        ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift> spells = new ArrayList<>();
        // same thing as getSpellList but without the tier logic, just add all of them
        return spells;
    }
}
