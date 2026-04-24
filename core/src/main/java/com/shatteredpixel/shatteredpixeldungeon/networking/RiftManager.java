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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

import java.util.HashMap;


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

        if(riftID.equals("dementia_rift")){
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
    }
}
