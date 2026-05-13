package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RiftStone;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Gamemode {
    private final static int[] bingoDims = new int[] {3,3};

    public static Gamemode current = Gamemode.classic(); //TODO set this
    private static Gamemode[] gamemodes = new Gamemode[]{Gamemode.classic(), Gamemode.classicRift(), Gamemode.bingo()};
    public String gamemodeID;
    public String gamemodeName;
    public boolean equipRiftStone;
    public boolean isTimed;
    public boolean isBingo;
    private boolean hasAmulet = false;
    public boolean bingoReady = false;

    public BingoTask[][] bingoTasks = new BingoTask[bingoDims[1]][bingoDims[0]];

    public Gamemode(String gamemodeID){
        this.gamemodeID = gamemodeID;
        System.out.println(this.getClass().getName());
        System.out.println(this.getClass().getPackage().getName());
        this.gamemodeName = Messages.get(this, gamemodeID+"_name");
    }

    public void onGameStart() {
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
                NetworkManager.INSTANCE.send("VICTORY:amulet_call_it_a_day"); // you can't ascend as of now btw
                NetworkManager.INSTANCE.finalTime = finalTime;
                return;
            }
        }
        if(current.gamemodeID.equals("bingo")){
            for(int i = 0; i < current.bingoTasks.length; i++){ // checking any rows
                if(current.bingoTasks[i][0].isCompleted() && current.bingoTasks[i][1].isCompleted() && current.bingoTasks[i][2].isCompleted()){
                    NetworkManager.INSTANCE.send("VICTORY:bingo_row");
                    return;
                }
            }
            for(int i = 0; i< current.bingoTasks[0].length; i++){ // checking any columns
                if(current.bingoTasks[0][i].isCompleted() && current.bingoTasks[1][i].isCompleted() && current.bingoTasks[2][i].isCompleted()) {
                NetworkManager.INSTANCE.send("VICTORY:bingo_column");
                return;
                }
            }
            // top-left to bottom-right diagonal
            if (current.bingoTasks[0][0].isCompleted() && current.bingoTasks[1][1].isCompleted() && current.bingoTasks[2][2].isCompleted()) {
                NetworkManager.INSTANCE.send("VICTORY:bingo_row");
                return;
            }
            // top-right to bottom-left diagonal
            if (current.bingoTasks[0][2].isCompleted() && current.bingoTasks[1][1].isCompleted() && current.bingoTasks[2][0].isCompleted()) {
                NetworkManager.INSTANCE.send("VICTORY:bingo_row");
                return;
            }
            HashMap<Player, Integer> counts = new HashMap<>(); // now I check if a blackout has occured. The player with the most tiles owned will send the message. This happens last, so other conditions are tested first
            for (int i = 0; i < current.bingoTasks.length; i++) {
                for (int j = 0; j < current.bingoTasks[i].length; j++) {

                    if (current.bingoTasks[i][j].isCompleted()) {

                        Player owner = current.bingoTasks[i][j].owner;

                        counts.put(owner, counts.getOrDefault(owner, 0) + 1);
                    }
                }
            }
            int myCount = counts.getOrDefault(NetworkManager.INSTANCE.self, 0);
            boolean isWinner = true;
            boolean isDraw = false;

            for (Object player : counts.keySet()) {
                if (player == NetworkManager.INSTANCE.self) continue;

                int other = counts.get(player);

                if (other > myCount) {
                    isWinner = false;
                }

                if (other == myCount) {
                    isDraw = true;
                }
            }

            if (isWinner && !isDraw) { // if not a draw, you win
                NetworkManager.INSTANCE.send("VICTORY:bingo_blackout");
            } else if (isDraw) { // if draw, all drawed players send the message
                NetworkManager.INSTANCE.send("VICTORY:bingo_draw");
            }

        }
    }

    public void updateBingo(){
        for(int i = 0; i < this.bingoTasks.length; i++){
            for(int j = 0; j < this.bingoTasks[i].length; j++){
                if(bingoTasks[i][j] != null) {
                    bingoTasks[i][j].check(Dungeon.hero);
                }
            }
        }
    }

    public void loadBingoFromNetwork(String[] taskEntries) {
        int index = 0;
        for (int j = 0; j < bingoDims[1]; j++) {
            for (int i = 0; i < bingoDims[0]; i++) {
                String[] parts = taskEntries[index].split("=", 2);
                String id = parts[0];
                float random = Float.parseFloat(parts[1]);
                BingoCondition condition = null;
                for (BingoCondition bc : BingoCondition.values()) {
                    if (bc.id.equals(id)) {
                        condition = bc;
                        break;
                    }
                }
                if (condition != null) {
                    bingoTasks[j][i] = new BingoTask(condition, random);
                }
                index++;
            }
        }
        bingoReady = true;
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
