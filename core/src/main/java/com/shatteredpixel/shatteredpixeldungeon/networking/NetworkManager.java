package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StartFreeze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InLobbyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Game;

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


    private Consumer<Lobby> lobbyInfoCallback;
    private LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();
    public ArrayList<ChatMessage> chat = new ArrayList<>();

    public Player self; //self. Basically this represents the player of the client itself.
    public ArrayList<Player> players = new ArrayList<>();

    public long freezeUntil = -1;

    public void connect(String ip) throws Exception {
        System.out.println("Connecting...");
        // Only connect if the socket is null or closed
        if (socket == null || socket.isClosed()) {
            Dungeon.dataFetcher = new DataFetcher();
            socket = new Socket(ip, 6000);
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
                //chatter = new Player(chatterID, "unknown");
                //players.add(chatter);
                System.out.println("\n\nERROR TO FIX : Check network manager! In theory this should never happen.\n\n");
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
                }

                if (lobbyID != null && lobbyName != null) {
                    parsedLobbies.put(lobbyID, new Lobby(lobbyName, hasPassword, admins, superAdmins, numPlayers));
                }
            }

            if (this.lobbyListCallback != null) {
                this.lobbyListCallback.accept(parsedLobbies);
            }

            System.out.println("LOBBIES: " + parsedLobbies);
        }
        else if (header.equals("INFOLOBBY")) {
            String lobbyID = null;
            String lobbyName = null;
            boolean inGame = false;
            int maxPlayers = 0;
            ArrayList<Player> players = new ArrayList<Player>();
            ArrayList<String> admins = new ArrayList<String>();
            ArrayList<String> superAdmins = new ArrayList<String>();


            String[] fields = data.split(",");
            for (String field : fields) {
                String[] kv = field.split("=", 2);
                if (kv.length < 2) continue;
                String key = kv[0];
                String value = kv[1];

                if (key.equals("id"))               lobbyID = value;
                else if (key.equals("name"))        lobbyName = value;
                else if (key.equals("ingame"))      inGame = value.equals("True");
                else if (key.equals("maxplayers"))  maxPlayers = Integer.parseInt(value);
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
                else if (key.equals("players")) {
                    if (!value.isEmpty()) {
                        for (String id : value.split("\\|")) {
                            System.out.println("Parsing player ID from INFOLOBBY: " + id);
                            System.out.println("Current this.players size: " + this.players.size());
                            Player found = null;
                            for (Player existing : this.players) {
                                if (existing.getID().equals(id)) {
                                    found = existing;
                                    break;
                                }
                            }
                            if (found == null) {
                                found = new Player(id, "unknown");
                                this.players.add(found);
                            }
                            players.add(found); // add to lobby's player list
                        }
                    }
                }
            }

            if (lobbyID != null && lobbies.containsKey(lobbyID)) {
                Lobby lobby = lobbies.get(lobbyID);
                lobby.setID(lobbyID);
                lobby.setInGame(inGame);
                lobby.setMaxPlayers(maxPlayers);
                lobby.setPlayers(players);
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
            System.out.println("ERROR AGAIN CHECK NETMANAGER");
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

    public void sendMessage(String msg){
        send("CHAT:"+self.getID()+"="+msg);
    }

}