package com.shatteredpixel.shatteredpixeldungeon.networking;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;

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
    }
}
