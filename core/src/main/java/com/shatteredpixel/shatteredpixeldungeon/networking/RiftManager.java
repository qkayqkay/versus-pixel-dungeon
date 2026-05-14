package com.shatteredpixel.shatteredpixeldungeon.networking;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GoldSink;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HungerSurge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.InvincibleSnail;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;


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
                mob.beckon( hero.pos );
            }

            if (Dungeon.level.heroFOV[hero.pos]) {
                CellEmitter.center( hero.pos ).start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
            }

            Sample.INSTANCE.play( Assets.Sounds.ALERT );
        }

        if(riftID.equals("floor_scramble_rift")){
            ScrollOfTeleportation.teleportChar(hero);
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
                float dist = Dungeon.level.trueDistance(trap.pos, hero.pos);
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
            ArrayList<EquipableItem> allEquipable = hero.belongings.getAllItems(EquipableItem.class);
            ArrayList<EquipableItem> potentialItems = new ArrayList<>();

            for (EquipableItem item : allEquipable) {
                if (!item.cursed) {
                    continue;
                }
                // check STR requirement for weapons and armor
                if (item instanceof Weapon) {
                    Weapon w = (Weapon) item;
                    if (w.STRReq() > hero.STR()) {
                        continue;
                    }
                } else if (item instanceof Armor) {
                    Armor a = (Armor) item;
                    if (a.STRReq() > hero.STR()) {
                        continue;
                    }
                }
                potentialItems.add(item);
            }

            if (!potentialItems.isEmpty()) {
                EquipableItem chosen = potentialItems.get((int)(Math.random() * potentialItems.size()));
                EquipableItem.equipCursed(hero); //plays the curse particle+sound effect
                chosen.doEquip(hero);
            }
        }
        if (riftID.equals("silent_dementia_rift")) {
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
        if(riftID.equals("silent_hunger_surge_rift")){
            Buff.prolong(hero, HungerSurge.class, 30);
        }
        if(riftID.equals("silent_gold_sink_rift")){
            Buff.affect(hero, GoldSink.class);
        }
        if(riftID.equals("invincible_snail_rift")){
            int cell;
            int tries = 50;
            do {
                cell = Dungeon.level.randomDestination( null );
                if (tries-- < 0 && cell != -1) break;

                PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
            } while (cell == -1 || PathFinder.distance[cell] < 10 || PathFinder.distance[cell] > 20);
            if(cell != -1){
                InvincibleSnail snail = new InvincibleSnail();
                snail.pos = cell;
                GameScene.add( snail);
                Dungeon.level.occupyCell(snail);
            }
        }
        if(riftID.equals("rat_swarm_rift")){
            int random = (int) Math.round(Math.random() * 3);
            GLog.n(Messages.get(this, "networking.riftmanager.rats_announce"+random));
            int numRats = (int) Math.round(Math.random()*2 + 4); // between 4-6
            for(int i = 0 ; i < numRats; i++) {
                int cell;
                int tries = 50;
                do {
                    cell = Dungeon.level.randomDestination(null);
                    if (tries-- < 0 && cell != -1) break;
                    PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
                } while (cell == -1 || PathFinder.distance[cell] < 7 || PathFinder.distance[cell] > 15 || Dungeon.level.heroFOV[cell]);
                Rat rat = new Rat();
                rat.givesRiftEnergy = false;
                rat.pos = cell;
                GameScene.add(rat);
                Dungeon.level.occupyCell(rat);
                rat.beckon(hero.pos);
            }
        }
        if(riftID.equals("lockdown_rift")){
            if (Dungeon.level.locked) {
                // Floor is already locked, rift fails
                return;
            }

            int heroPos = hero.pos;
            int width = Dungeon.level.width();
            int heroX = heroPos % width;
            int heroY = heroPos / width;
            int target = -1;

            outer:
            for (int radius = 1; radius <= 20; radius++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        // Only check the "ring" at the current radius, not inner tiles we've already checked
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                            continue;
                        }

                        int checkX = heroX + dx;
                        int checkY = heroY + dy;

                        if (checkX < 0 || checkX >= width || checkY < 0 || checkY >= Dungeon.level.height()) {
                            continue;
                        }

                        int checkPos = checkY * width + checkX;
                        int cell = Dungeon.level.map[checkPos];

                        if (cell == Terrain.DOOR || cell == Terrain.OPEN_DOOR) {
                            target = checkPos;
                            break outer;
                        }
                    }
                }
            }

            if (target != -1) {

                Level.set(target, Terrain.LOCKED_DOOR);
                GameScene.updateMap(target);
                Dungeon.observe();
                int cell;
                int tries = 50;
                do {
                    cell = Dungeon.level.randomDestination( null );
                    if (tries-- < 0 && cell != -1) break;

                    PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
                } while (cell == -1);

                if (tries < 0){
                    return;
                }


                Dungeon.level.drop(new IronKey(Dungeon.depth), cell).seen = true;
                for (int i : PathFinder.NEIGHBOURS9) {
                    Dungeon.level.mapped[cell + i] = true;
                }
                GameScene.updateFog(cell, 1);
            }
        }
    }
}
