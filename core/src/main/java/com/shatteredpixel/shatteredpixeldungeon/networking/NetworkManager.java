package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StartFreeze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.google.gson.*;


public enum NetworkManager {

    INSTANCE;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isListening = false;
    private Consumer<LinkedHashMap<String, Lobby>> lobbyListCallback;
    private Runnable onChatReceived;
    private Runnable onJoinError;
    private Runnable onLevelChanged;

    public long countdownUntil = -1;
    public boolean shouldCountdown = false;
    public boolean shouldFreeze = true;


    private Consumer<Lobby> lobbyInfoCallback;
    private LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();
    public ArrayList<ChatMessage> chat = new ArrayList<>();

    public Player self; //self. Basically this represents the player of the client itself.
    public ArrayList<Player> players = new ArrayList<>();

    public volatile long freezeUntil = -1;

    public void connect(String ip, int port) throws Exception {
        System.out.println("Connecting...");
        // Only connect if the socket is null or closed
        if (socket == null || socket.isClosed()) {
            Dungeon.dataFetcher = new DataFetcher();
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String serverOK, heading, data;
            String[] parts;
            while((serverOK = in.readLine()) != null){
                 parts = serverOK.split(":", 2);
                 heading = parts[0];
                 data = parts[1];
                 if(heading.equals("PLAYERID")){
                     String playerID = data;
                     self = new Player(data, "temp");
                     players.add(self);
                     System.out.println("Connected to Server! Player ID is: "+data);
                     startListening();
                     break;
                 }
            }
        }
        else{
            System.out.println("Failed to connect to server! Format is ip;port");
        }
    }

    public boolean isConnected(){return (socket!=null);}

    public void startListening() {
        System.out.println("Now listening!");
        isListening = true;
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
                while (isListening && (serverMessage = in.readLine()) != null) {
                    handleIncomingMessage(serverMessage);
                }
            } catch (Exception e) {
                System.err.println("Lost connection to server: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleIncomingMessage(String message) { // im ngl this got so boring to write that some of this is just AI. Serialization booo.
        String[] headerData = message.split(":", 2);
        String header = headerData[0];
        String data = headerData[1];
        System.out.println("Caught incoming data: "+message+", header is: "+header);

        if (header.equals("EVENT")) {
            System.out.println("SERVER EVENT: " + data);
        }

        else if (header.equals("CHAT")) {
            String[] chatEntries = data.split("=", 2);
            String chatterID = chatEntries[0];
            String msg = chatEntries[1];

            Player chatter = null;
            for (Player player : players) {
                if (player.getID().equals(chatterID)) {
                    chatter = player;
                    break;
                }
            }

            if (chatter == null) {
                chatter = new Player(chatterID, "unknown");
                players.add(chatter);
            }

            chat.add(new ChatMessage(chatter, msg));
            if (onChatReceived != null) {
                onChatReceived.run();
            }
        }
        else if (header.equals("SERVERCHAT")){
            chat.add(new ChatMessage(data));
            if (onChatReceived != null) {
                onChatReceived.run();
            }
        }

        else if (header.equals("JOINNOTIFY")) {
            if (data.equals("canenter")) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Correct password!");
                        Game.switchScene(InLobbyScene.class);
                    }
                });
            } else {
                if (onJoinError != null) {
                    onJoinError.run();
                }
            }
        }
        else if (header.equals("CLASSUPDATE")){
            String[] entries = data.split("=");
            String id = entries[0];
            String cl = entries[1];
            HeroClass heroClass = HeroClass.valueOf(cl);
            for(Player p : players){
                if(p.getID().equals(id)){
                    p.cl = heroClass;
                }
            }
        }
        else if (header.equals("LISTLOBBY")) {
            System.out.println("Caught lobby list: " + data);
            LinkedHashMap<String, Lobby> parsedLobbies = new LinkedHashMap();
            String[] lobbyEntries = data.split(";");

            for (String entry : lobbyEntries) {
                if (entry.isEmpty()) continue;

                String lobbyID = null;
                String lobbyName = null;
                boolean hasPassword = false;
                ArrayList<String> admins = new ArrayList<String>();
                ArrayList<String> superAdmins = new ArrayList<String>();
                int numPlayers = 0;
                int maxPlayers = 0;

                String[] fields = entry.split(",");
                for (String field : fields) {
                    String[] kv = field.split("=", 2);
                    if (kv.length < 2) continue;
                    String key = kv[0];
                    String value = kv[1];

                    if (key.equals("lobbyid"))       lobbyID = value;
                    else if (key.equals("lobbyname")) lobbyName = value;
                    else if (key.equals("haspassword")) hasPassword = value.equals("true");
                    else if (key.equals("admins")) {
                        if (!value.isEmpty()) {
                            for (String id : value.split("\\|")) {
                                admins.add(id);
                            }
                        }
                    }
                    else if (key.equals("superadmins")) {
                        if (!value.isEmpty()) {
                            for (String id : value.split("\\|")) {
                                superAdmins.add(id);
                            }
                        }
                    }
                    else if (key.equals("numplayers"))  numPlayers = Integer.parseInt(value);
                    else if (key.equals("maxplayers"))  maxPlayers = Integer.parseInt(value);
                }

                if (lobbyID != null && lobbyName != null) {
                    Lobby newLobby = new Lobby(lobbyName, hasPassword, admins, superAdmins, numPlayers, maxPlayers);
                    newLobby.setID(lobbyID);
                    parsedLobbies.put(lobbyID, newLobby);
                }
            }

            if (this.lobbyListCallback != null) {
                this.lobbyListCallback.accept(parsedLobbies);
            }

            System.out.println("LOBBIES: " + parsedLobbies);
        }
        else if (header.equals("INFOLOBBY")) {
            JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
            String lobbyID = obj.get("id").getAsString();
            String lobbyName = obj.get("name").getAsString();
            boolean inGame = obj.get("ingame").getAsBoolean();
            int maxPlayers = obj.get("maxplayers").getAsInt();

            ArrayList<String> admins = new ArrayList<String>();
            for (JsonElement el : obj.getAsJsonArray("admins")) {
                admins.add(el.getAsString());
            }

            ArrayList<String> superAdmins = new ArrayList<String>();
            for (JsonElement el : obj.getAsJsonArray("superadmins")) {
                superAdmins.add(el.getAsString());
            }

            ArrayList<Player> lobbyPlayers = new ArrayList<Player>();
            for (JsonElement el : obj.getAsJsonArray("players")) {
                JsonObject playerObj = el.getAsJsonObject();
                String id = playerObj.get("id").getAsString();
                String name = playerObj.get("name").getAsString();

                Player found = null;
                for (Player existing : this.players) {
                    if (existing.getID().equals(id)) {
                        found = existing;
                        break;
                    }
                }
                if (found == null) {
                    found = new Player(id, name);
                    this.players.add(found);
                } else {
                    found.name = name;
                }
                lobbyPlayers.add(found);
            }

            if (lobbyID != null && lobbies.containsKey(lobbyID)) {
                Lobby lobby = lobbies.get(lobbyID);
                lobby.setID(lobbyID);
                lobby.setName(lobbyName);
                lobby.setInGame(inGame);
                lobby.setMaxPlayers(maxPlayers);
                lobby.setPlayers(lobbyPlayers);
                lobby.setAdmins(admins);
                lobby.setSuperAdmins(superAdmins);

                if (this.lobbyInfoCallback != null) {
                    this.lobbyInfoCallback.accept(lobby);
                }
                System.out.println("Got LOBBYINFO for lobby ID: " + lobbyID);
            }
        }
        else if (header.equals("CHANGELEVEL")){
            String[] dataList = data.split("=", 2);
            String target = dataList[0];
            int l = Integer.parseInt(dataList[1]);
            for(Player p : this.players){
                if(p.getID().equals(target)){
                    p.changeLevel(l);
                }
            }
            if (onLevelChanged != null) {
                onLevelChanged.run();
            }
        }
        else if (header.equals("ISREADY")) {
            for (Player p : players) {
                if (p.getID().equals(data)) {
                    p.isReadied = !p.isReadied;
                    if(p.isReadied){
                        chat.add(new ChatMessage(p.getID()+" is ready!"));
                    }
                    if(!p.isReadied){
                        chat.add(new ChatMessage(p.getID()+" unchecked."));
                    }
                    if (onChatReceived != null) {
                        onChatReceived.run();
                    }
                    break;
                }
            }
        }
        else if (header.equals("READYGAME")) {
            String[] dataList = data.split("=");
            Long seed = Long.parseLong(dataList[1]);
            Game.switchScene(HeroSelectScene.class);
            System.out.println("Server is starting the game! Seed: " + seed);
            Dungeon.initSeed(seed);

        }

        else if (header.equals("STARTGAME")) {
            long startTimeMs = (long) Double.parseDouble(data);
            freezeUntil = startTimeMs;
            countdownUntil = startTimeMs;
            shouldCountdown = true;
            //shouldFreeze = true; I can't bother to figure out why but this doesn't work if I add it with 2+ players but works fine with 1 wtf.
        }

        else if (header.equals("GAMEEND")) {
            String[] parts = data.split(",", 2);
            String[] winners = parts[0].replace("winners=", "").split("\\|");
            String endType = parts[1].replace("victorytype=", "");
            System.out.println("Gameend!");

            if (endType.equals("amuletwin")) {
                if (Arrays.asList(winners).contains(self.getID())) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Game.switchScene(VictoryScene.class);
                            System.out.println("not death");
                        }
                    });
                } else {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Dungeon.hero.die(null);
                            GameScene.gameOver();
                            GLog.n(Messages.get("amuletloss"), winners[0]); // I can take the first elements since there should only be 1 in this case.
                            System.out.println("Someone has acquired the amulet of yendor before you!");
                        }
                    });
                }
            }
        }

    }


    private void broadcast(String message){
        System.out.println(message);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void isReady(){
        this.send("ISREADY:");
    }

    public void updateClass(){
        self.cl = GamesInProgress.selectedClass;
        this.send("CLASSUPDATE:"+self.cl.name());
    }


    public void disconnect() {
        try {
            isListening = false;
            if (socket != null) socket.close();
        } catch (Exception e) { /* ignore */ }
    }

    public void requestLobbyInfo(Consumer<Lobby> callback) {
        this.lobbyInfoCallback = callback;
        this.send("INFOLOBBY:");
    }


    public void listLobbies(Consumer<LinkedHashMap<String, Lobby>> callback) {
        this.lobbyListCallback = new Consumer<LinkedHashMap<String, Lobby>>() {
            @Override
            public void accept(LinkedHashMap<String, Lobby> result) {
                lobbies = result;
                if (callback != null) callback.accept(result);
            }
        };
        this.send("LISTLOBBY:");
    }
        // TODO, maybe add data values like playercount = true to include the playercount?

    public void setChatCallback(Runnable callback) {
        this.onChatReceived = callback;
    }
    public void setJoinErrorCallback(Runnable callback) {
        this.onJoinError = callback;
    }
    public void setLevelChangedCallback(Runnable callback) {
        this.onLevelChanged = callback;
    }


    public void createLobby(String lobbyName, String lobbyPassword){
        StringBuilder msg = new StringBuilder("CREATELOBBY:");
        msg.append("name="+lobbyName+";password="+lobbyPassword);
        this.send(msg.toString());
    }
    public void createLobby(String lobbyName){ // if there's no password in the server we're trying to create
        this.createLobby(lobbyName, "None");
    }

    public void joinLobby(String lobbyID, String lobbyPassword){
        StringBuilder msg = new StringBuilder("JOINLOBBY:");
        msg.append("id="+lobbyID+";password="+lobbyPassword);
        if (!lobbies.containsKey(lobbyID)) {
            System.out.println("ERROR AGAIN CHECK NETMANAGER. We got lobbyID: "+lobbyID);
        }
        this.send(msg.toString());
    }
    public void joinLobby(String lobbyID){ // if there's no password in the server we're trying to join
        this.joinLobby(lobbyID, "None");
    }

    public LinkedHashMap<String, Lobby> getLobbies() {
        return lobbies;
    }

    public void initiateStart() {
        this.send("INITIATESTART:");
    }

    public void leaveLobby(){
        this.send("LEAVELOBBY:");
        this.chat.clear();
    }

    public void sendMessage(String msg){
        send("CHAT:"+self.getID()+"="+msg);
    }

}