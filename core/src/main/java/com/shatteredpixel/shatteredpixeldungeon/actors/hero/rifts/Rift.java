package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HungerSurge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RiftStone;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public abstract class Rift {

    protected String riftId = "";
    protected boolean silent = false;
    protected String[] targetIds = new String[0];
    protected HashMap<String, Object> params = new HashMap<>();
    protected int cost;

    public void onCast(Hero hero) {
        System.out.println("casting!");
        onRiftCast();
        hero.belongings.riftStone.spendCharge(cost);

        params = new HashMap<>();

        JsonObject json = new JsonObject();
        json.addProperty("rift_id", riftId);
        json.addProperty("silent", silent);

        JsonArray targetsArray = new JsonArray();
        for (String id : targetIds) {
            targetsArray.add(id);
        }
        json.add("targets", targetsArray);

        JsonObject paramsObj = new JsonObject();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object val = entry.getValue();

            if (val instanceof Number) {
                paramsObj.addProperty(entry.getKey(), (Number) val);
            } else if (val instanceof Boolean) {
                paramsObj.addProperty(entry.getKey(), (Boolean) val);
            } else {
                paramsObj.addProperty(entry.getKey(), val.toString());
            }
        }
        json.add("params", paramsObj);
        NetworkManager.INSTANCE.send("RIFT:" + json.toString());
    }

    public float chargeUse(Hero hero){
        return cost;
    }

    public boolean canCast(Hero hero){
        return hero.belongings.riftStone.getCharge() >= cost;
    }

    public int getCost(){
        return cost;
    }

    public float castProportion(Hero hero){ // this is used to calculate the gray overlay over each spellbutton
        RiftStone stone = hero.belongings.riftStone;
        if (stone == null || cost <= 0) return 1f;
        return Math.min(1f, (float) stone.getCharge() / cost);
    }

    public String name(){
        return Messages.get(this, "name");
    }

    public String shortDesc(){
        return Messages.get(this, "short_desc") + " " +
                Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
    }

    public String desc(){
        return Messages.get(this, "desc") + "\n\n" +
                Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
    }

    public int icon(){
        return HeroIcon.NONE;
    }

    public void onRiftCast(){
        Invisibility.dispel();
        // Talent.onArtifactUsed(hero); what does this do? Figure it out
    }

    public static ArrayList<Rift> getRiftList(Hero hero, int tier){
        ArrayList<Rift> rifts = new ArrayList<>();

        if (tier == 1) {
            rifts.add(TrapTriggerRift.INSTANCE);
            rifts.add(RatSwarmRift.INSTANCE);
            rifts.add(LockdownRift.INSTANCE);
        } else if (tier == 2) {
            rifts.add(AlarmRift.INSTANCE);
            rifts.add(DementiaRift.INSTANCE);


        } else if (tier == 3){
            rifts.add(CursedGiftRift.INSTANCE);
            rifts.add(HungerSurgeRift.INSTANCE);
            rifts.add(InvincibleSnailRift.INSTANCE);

        }
        else if (tier == 4) {
            rifts.add(GoldSinkRift.INSTANCE);
            rifts.add(DisarmingRift.INSTANCE);
            rifts.add(FloorScrambleRift.INSTANCE);
        }
        return rifts;
    }

    public static ArrayList<Rift> getAllRifts() {
        ArrayList<Rift> rifts = new ArrayList<>();
        rifts.add(AlarmRift.INSTANCE);
        rifts.add(FloorScrambleRift.INSTANCE);
        rifts.add(DisarmingRift.INSTANCE);
        rifts.add(TrapTriggerRift.INSTANCE);
        rifts.add(CursedGiftRift.INSTANCE);
        rifts.add(DementiaRift.INSTANCE);
        rifts.add(HungerSurgeRift.INSTANCE);
        rifts.add(GoldSinkRift.INSTANCE);
        rifts.add(InvincibleSnailRift.INSTANCE);
        rifts.add(RatSwarmRift.INSTANCE);
        rifts.add(LockdownRift.INSTANCE);

        return rifts;
    }
}