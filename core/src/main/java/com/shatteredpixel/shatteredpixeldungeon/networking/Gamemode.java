package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RiftStone;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Gamemode {
    public static Gamemode current = Gamemode.classic(); //TODO set this
    private static Gamemode[] gamemodes = new Gamemode[]{Gamemode.classic(), Gamemode.classicRift(), Gamemode.bingo()};
    public String gamemodeID;
    public String gamemodeName;
    public boolean equipRiftStone;
    public boolean isTimed;
    public boolean isBingo;
    private boolean hasAmulet = false;

    private final int[] bingoDims = new int[] {3,3};
    public BingoTask[][] bingoTasks = new BingoTask[bingoDims[1]][bingoDims[0]];

    public Gamemode(String gamemodeID){
        this.gamemodeID = gamemodeID;
        System.out.println(this.getClass().getName());
        System.out.println(this.getClass().getPackage().getName());
        this.gamemodeName = Messages.get(this, gamemodeID+"_name");
    }

    public void onGameStart() {
        ArrayList<BingoCondition> conditionsList = new ArrayList<>(Arrays.asList(BingoCondition.getConditions()));
        Random rand = new Random();
        for (int j = 0; j < bingoDims[1]; j++) {
            for (int i = 0; i < bingoDims[0]; i++) {
                int n = rand.nextInt(conditionsList.size());
                float random = rand.nextFloat();
                BingoTask bingoTask = new BingoTask(conditionsList.get(n), random);
                bingoTasks[j][i] = bingoTask;
                conditionsList.remove(n);
            }
        }
    }

    public void onHeroCreate(Hero h){ // called when the hero object is created.
        if(equipRiftStone) {
            RiftStone stone = new RiftStone();
            h.belongings.riftStone = stone;
            stone.activate(h);
        }

    }

    public static void testVictoryCondition(){ // maybe modify to just update shit all the time. AKA a updateShit() method that fills up the bingo for example whenever hero.dopickup is called or mob.destroy
        if(current.gamemodeID.equals("classic") || current.gamemodeID.equals("classic_rift")){
            if(current.hasAmulet){
                float finalTime = GameTimer.instance.stopTimer();
                NetworkManager.INSTANCE.send("VICTORY:");
                NetworkManager.INSTANCE.finalTime = finalTime;
            }
        }
        if(current.gamemodeID.equals("bingo")){
            // TODO add bingo logic, check if any row/column/diag is filled here, and then if so win. Maybe add a separate win() method? What about a die method later on?
        }
    }

    public void updateBingo(){
        for(int i = 0; i < this.bingoTasks.length; i++){
            for(int j = 0; j < this.bingoTasks[i].length; j++){
                bingoTasks[i][j].check(Dungeon.hero);
            }
        }
    }

    public void pickUpAmulet(){
        hasAmulet = true;
        testVictoryCondition();
    }

    public static Gamemode fromID(String id) {
        switch (id) {
            case "classic": return classic();
            case "classic_rift": return classicRift();
            case "bingo": return bingo();
            default: return classic();
        }
    }

    public static Gamemode classic() {
        Gamemode g = new Gamemode("classic");
        g.isTimed = true;
        return g;
    }

    public static Gamemode classicRift() {
        Gamemode g = new Gamemode("classic_rift");
        g.isTimed = true;
        g.equipRiftStone = true;
        return g;
    }

    public static Gamemode bingo() {
        Gamemode g = new Gamemode("bingo");
        g.isBingo = true;
        return g;
    }

    public static Gamemode[] listGamemodes(){
        return gamemodes;
    }
}
