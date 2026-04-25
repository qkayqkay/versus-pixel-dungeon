package com.shatteredpixel.shatteredpixeldungeon.networking;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class RiftManager {
    public static RiftManager INSTANCE;
    public RiftManager(){
        INSTANCE = this;
    }

    public void afflictRift(String data){
        JsonObject json = JsonParser.parseString(data).getAsJsonObject();

        String riftID = json.get("rift_id").getAsString();
        String casterID = json.get("caster").getAsString();
        boolean silent = json.get("silent").getAsBoolean();

        JsonArray targetsArray = json.getAsJsonArray("targets");
        String[] targetIDs = new String[targetsArray.size()];
        for (int i = 0; i < targetsArray.size(); i++) {
            targetIDs[i] = targetsArray.get(i).getAsString();
        }

        HashMap<String, Object> params = new Gson().fromJson(
                json.getAsJsonObject("params"),
                HashMap.class
        );

        System.out.println("Caster: " + casterID);
        System.out.println("Silent: " + silent);
        System.out.println("Targets: " + java.util.Arrays.toString(targetIDs));
        System.out.println("Params: " + params);

        if(riftID.equals("alarm_rift")){
            for (Mob mob : Dungeon.level.mobs) {
                mob.beckon( Dungeon.hero.pos );
            }

            if (Dungeon.level.heroFOV[Dungeon.hero.pos]) {
                CellEmitter.center( Dungeon.hero.pos ).start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
            }

            Sample.INSTANCE.play( Assets.Sounds.ALERT );
        }

        if(riftID.equals("floor_scramble_rift")){
            ScrollOfTeleportation.teleportChar(Dungeon.hero);
            BArray.setFalse(Dungeon.level.visited);
            BArray.setFalse(Dungeon.level.mapped);
            GameScene.updateFog();
            Dungeon.observe();
        }

        if(riftID.equals("disarming_rift")) {
            Hero hero = Dungeon.hero;
            KindOfWeapon weapon = hero.belongings.weapon;

            if (weapon != null && !weapon.cursed) {

                int cell;
                int tries = 50;
                do {
                    cell = Dungeon.level.randomRespawnCell( null );
                    if (tries-- < 0 && cell != -1) break;

                    PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
                } while (cell == -1 || PathFinder.distance[cell] < 10 || PathFinder.distance[cell] > 20);

                if (tries < 0){
                    return;
                }

                hero.belongings.weapon = null;
                Dungeon.quickslot.clearItem(weapon);
                weapon.updateQuickslot();

                Dungeon.level.drop(weapon, cell).seen = true;
                for (int i : PathFinder.NEIGHBOURS9) {
                    Dungeon.level.mapped[cell + i] = true;
                }
                GameScene.updateFog(cell, 1);
                Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
                CellEmitter.get(hero.pos).burst(Speck.factory(Speck.LIGHT), 4);
            }
        }
        if(riftID.equals("trap_trigger_rift")){
            SparseArray<Trap> traps = Dungeon.level.traps;
            Trap nearestTrap = null;
            float nearestDist = Float.MAX_VALUE;
            for (Trap trap : traps.values()) {
                float dist = Dungeon.level.trueDistance(trap.pos, Dungeon.hero.pos);
                if(dist < nearestDist){
                    nearestDist = dist;
                    nearestTrap = trap;
                }

            }
            if(nearestTrap != null) {
                nearestTrap.trigger();
            }
        }
        if (riftID.equals("cursed_gift_rift")) {
            ArrayList<EquipableItem> allEquipable = Dungeon.hero.belongings.getAllItems(EquipableItem.class);
            ArrayList<EquipableItem> potentialItems = new ArrayList<>();

            for (EquipableItem item : allEquipable) {
                if (!item.cursed) {
                    continue;
                }
                // check STR requirement for weapons and armor
                if (item instanceof Weapon) {
                    Weapon w = (Weapon) item;
                    if (w.STRReq() > Dungeon.hero.STR()) {
                        continue;
                    }
                } else if (item instanceof Armor) {
                    Armor a = (Armor) item;
                    if (a.STRReq() > Dungeon.hero.STR()) {
                        continue;
                    }
                }
                potentialItems.add(item);
            }

            if (!potentialItems.isEmpty()) {
                EquipableItem chosen = potentialItems.get((int)(Math.random() * potentialItems.size()));
                EquipableItem.equipCursed(Dungeon.hero); //plays the curse particle+sound effect
                chosen.doEquip(Dungeon.hero);
            }
        }
        if (riftID.equals("dementia_rift")) {
            if(Math.random() > 0.5) { // 50/50 chance
                HashSet<Class<? extends Scroll>> known = Scroll.getKnown();
                if (!known.isEmpty()) {
                    ArrayList<Class<? extends Scroll>> list = new ArrayList<>(known);
                    Class<? extends Scroll> chosenClass = list.get((int) (Math.random() * list.size()));
                    Scroll chosen = Reflection.newInstance(chosenClass);
                    chosen.setKnown(false);
                    Item.updateQuickslot();
                }
            }
            else{
                HashSet<Class<? extends Potion>> known = Potion.getKnown();
                if (!known.isEmpty()) {
                    ArrayList<Class<? extends Potion>> list = new ArrayList<>(known);
                    Class<? extends Potion> chosenClass = list.get((int) (Math.random() * list.size()));
                    Potion chosen = Reflection.newInstance(chosenClass);
                    chosen.setKnown(false);
                    Item.updateQuickslot();
                }
            }
        }
    }
}
