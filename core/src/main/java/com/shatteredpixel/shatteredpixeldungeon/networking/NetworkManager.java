package com.shatteredpixel.shatteredpixeldungeon.networking;


import com.badlogic.gdx.utils.TimeUtils;
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
import com.shatteredpixel.shatteredpixeldungeon.ui.BingoBoard;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.google.gson.*;
import com.watabou.noosa.Scene;

import static com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add;

/*
 _     ___   ____ _  __  ___ _   _
| |   / _ \ / ___| |/ / |_ _| \ | |
| |  | | | | |   | ' /   | ||  \| |
| |__| |_| | |___| . \   | || |\  |
|_____\___/ \____|_|\_\ |___|_| \_|

                            &
      &&&&&&&&&&&&&&&&      &&
      &&&&&&&&&&&&&&&&&&&&  &&
       &&&&&&&&&&&&&&&&&&&&&&&&
       &&                &&&&&&
                        &&&&&&&
                      &&&&         &&&&&&&&&&&&&
              & &                 &&&&&&&&&&&&&&&
         &&&&&&&&&&              &&&&&&&&&   &&&
       &&&&&&    &               &&&&&&&&&    &
      &&&&&&&   & &              &&&&& &&&  &&&
      &&&&&&&&                   &&&&&        &
       &&&&        &         &&&&&&&&&       &&&
        &&&&       &       &&     &  & &&    &&
         & &  &         &          &&&  &&&& &
         &&     &     &         &   &&&&
        &     &&  &  &        &&     &&
          &  &   &&&&        &&      &
       &       &   &&&       &&      &
     &&&&      && &  &&      &&      &
      &&&&      &&           &&      &&
      &&&&      &        &   &&      &&
      &&&&&    &          &   &      &&&&&&&
      &&&&&   &   &         & &&   & &&&  &
      &&&&&& &     &          &&&&&   &  &
       &&&&&&       &       &&&&&&&&   &&&
       &&&&&&&                &&&& &&   &    &&&    &&&&
       &&&&&&&&&              && &&&&&&        &&&     &&&&
        &&&&&&&&&&             &&&   &&&&&&&&&&&&&      &&
        &&&&&&&&&&&&&&&&&&                        &&& &
        &&&&&&&&&&&&&&&&                            &
         &&&&&&&&&&&&&&&&&&&&&                       &
         &&&&&&&&&&&&&&&&&&&&&&&&&&  &&&&&           &
          &&&&&&&&&&&&&&&&&&&&&&&&&&&& &&&&&&        &&
           &&&&&&&&&&&&&&&&&&&&&&&    &&& &&          &
                 &&&&&&&&&&&           &&  &&         &
                     &                 &&  &&         &
                  & &                  &   &&         &&
                  & &                 &&    &&         &
                  & &                 &&    &&         &&
                  & &                 &      &&         &
                  & &                 &&&&&&&&&       &&
              &&&   &&&&&            &   &&   &&&&&&
           &&&          &&&         & &  & &  &&  &&&
       &&  &  &&       &  && &        & &    &&&  & &
       &&&&&             && &&&&   &         & &  &  &&
     &&&                    &&  &       &  &&&          &&&
     &&                        &&           &    &&          &&
                               &&             &&&&  &&&&&&&&&&&



 */


public enum NetworkManager {
    INSTANCE;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isListening = false;

    // These are essentially a bunch of callbacks for a variety of things. When the server responds with info,
    // I need to be able to access it later down the line(most of the time this occurs in a scene script)
    private Consumer<LinkedHashMap<String, Lobby>> lobbyListCallback; // when I recieve the list of lobbies
    private Runnable onChatReceived; // when I recieve a chat message(this includes server messages btw)
    private Consumer<String> onJoinError; // when I fail to join a server(eg: wrong password)
    private Runnable onLevelChanged; // when the level changes(used to send announcements if I remember correctly?)
    private Runnable onDisconnected; // callback for when keepalive detects a timeout
    private Runnable onClassUpdate; // when I change class(must notify others)
    private Runnable onLobbyCreated; // when I create an accuont
    private Runnable onSettingsUpdate; // when I recieve setting changes
    private Runnable onLoginSuccess; // when I successfully login
    private Consumer<String> onLoginFail; // when I fail to login
    private Runnable onRegisterSuccess; // when I succeed in registering
    private Consumer<String> onRegisterFail; // when I fail in registering


    public long countdownUntil = -1;
    public boolean shouldCountdown = false;
    public boolean shouldFreeze = true;


    private Consumer<Lobby> lobbyInfoCallback;
    private LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();
    public ArrayList<ChatMessage> chat = new ArrayList<>();

    public Player self; //self. Basically this represents the player of the client itself.
    private String pendingLobbyID; // the lobby you're trying to join, when waiting on the server to respond yes or no to let you in
    public ArrayList<Player> players = new ArrayList<>();

    public volatile long freezeUntil = -1;
    public float finalTime;
    public long lastPongReceived;

    public void connect(String ip, int port) throws Exception {
        if (socket != null && !socket.isClosed()) {
            System.out.println("Failed to connect to server! Already connected.");
            return;
        }
        Dungeon.dataFetcher = new DataFetcher();
        new RiftManager();

        final WndMessage wndConnecting = new WndMessage("Connecting...");
        PixelScene.showWindow(wndConnecting);

        final int finalPort = port;
        final String finalIp = ip;

        Thread connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(finalIp, finalPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String serverOK;
                    while ((serverOK = in.readLine()) != null) {
                        String[] parts = serverOK.split(":", 2);
                        String heading = parts[0];
                        String data = parts[1];

                        if (heading.equals("NEWCONNECT")) {
                            String[] nameParts = data.split("=", 2);
                            String id = nameParts[0];
                            String name = nameParts[1];
                            self = new Player(id, name); // self represents the Player object of the current client.
                            players.add(self);
                            System.out.println("Connected to Server! Player ID is: " + data);

                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    wndConnecting.destroy();
                                }
                            });

                            startListening();
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Connection handshake failed: " + e.getMessage());
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            wndConnecting.destroy();
                        }
                    });
                }
            }
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }

    public boolean isConnected(){return (socket!=null);}

    public void startListening() {
        System.out.println("Now listening!");
        isListening = true;

        // Initialise lastPongReceived so the keepalive doesn't immediately fire
        lastPongReceived = System.currentTimeMillis();

        Thread listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String serverMessage;
                    while (isListening && (serverMessage = in.readLine()) != null) {
                        handleIncomingMessage(serverMessage);
                    }
                } catch (Exception e) {
                    System.err.println("Lost connection to server: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();

        lastPongReceived = System.currentTimeMillis();

        Thread keepAlive = new Thread(new Runnable() {
            @Override
            public void run() {
                int missedPongs = 0;
                while (isListening) {
                    try {
                        send("PING:");
                        Thread.sleep(5000);

                        long timeSinceLastPong = System.currentTimeMillis() - lastPongReceived;
                        if (timeSinceLastPong > 7000) {
                            missedPongs++;
                            System.err.println("Missed pong " + missedPongs + "/3");
                            if (missedPongs >= 3) {
                                System.err.println("3 missed pongs — disconnecting.");
                                disconnect();
                                if (onDisconnected != null) {
                                    Gdx.app.postRunnable(new Runnable() {
                                        @Override
                                        public void run() {
                                            onDisconnected.run();
                                        }
                                    });
                                }
                                break;
                            }
                        } else {
                            missedPongs = 0; //reset if we got a pong
                        }
                    } catch (Exception e) {
                        System.err.println("Keepalive thread error: " + e.getMessage());
                        break;
                    }
                }
            }
        });
        keepAlive.setDaemon(true);
        keepAlive.start();
    }

    private void handleIncomingMessage(String message) { // im ngl this got so boring to write that some of this is just AI. Serialization booo.
        String[] headerData = message.split(":", 2);
        if (headerData.length < 2) {
            System.err.println("Malformed message (no colon): " + message);
            return;
        }
        String header = headerData[0];
        String data = headerData[1];
        if(!header.equals("PONG")) { // or else this fills up the console
            System.out.println("Caught incoming data: " + message + ", header is: " + header);
        }
        if (header.equals("PONG")) {
            lastPongReceived = System.currentTimeMillis();
        }

        else if (header.equals("EVENT")) {
            System.out.println("SERVER EVENT: " + data);
        }

        else if (header.equals("RIFT")){
            RiftManager.INSTANCE.afflictRift(data);
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
                chatter = new Player(chatterID, "Unknown");
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

        else if (header.equals("KICKED")){
            self.setLobby(null);
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Scene currentScene = Game.scene();
                    if(currentScene != null) {
                        currentScene.add(new WndTitledMessage(Icons.WARNING.get(), Messages.get(this, "wnd_kicked_title"), Messages.get(this, "wnd_kicked_desc")));
                    }
                    Game.switchScene(JoinScene.class);
                }
            });
        }

        else if (header.equals("BANNED")){
            self.setLobby(null);
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Scene currentScene = Game.scene();
                    if(currentScene != null) {
                        currentScene.add(new WndTitledMessage(Icons.WARNING.get(), Messages.get(this, "wnd_banned_title"), Messages.get(this, "wnd_banned_desc")));
                    }
                    Game.switchScene(JoinScene.class);
                }
            });
        }

        else if (header.equals("JOINNOTIFY")) {
            if (data.equals("canenter")) {
                if (pendingLobbyID != null && lobbies.containsKey(pendingLobbyID)) {
                    Lobby lobby = lobbies.get(pendingLobbyID);
                    self.setLobby(lobby);
                } else {
                    System.err.println("JOINNOTIFY: pending lobby not found!");
                }

                pendingLobbyID = null;

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Correct password!");
                        Game.switchScene(InLobbyScene.class);
                    }
                });
            } else {
                pendingLobbyID = null;
                if (onJoinError != null) {
                    final String reason = data; // "wrongpassword", "isbanned", "lobbynotfound", "maxcapacity". Im not updating this list so check the server.
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            onJoinError.accept(reason);
                        }
                    });
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
            if (onClassUpdate != null) {
                onClassUpdate.run();
            }
        }
        else if (header.equals("GAMEMODEUPDATE")) {
            Gamemode.current = Gamemode.fromID(data);
            if (onSettingsUpdate != null) {
                onSettingsUpdate.run();
            }
        }
        else if (header.equals("CHANGEMAXPLAYERCOUNT")) {
            int newMax = Integer.parseInt(data);
            if (self.getLobby() != null) {
                self.getLobby().setMaxPlayers(newMax);
            }
            if (onSettingsUpdate != null) {
                onSettingsUpdate.run();
            }
        }
        else if (header.equals("BINGOCOMPLETE")) {
            String[] entries = data.split("=");
            String targetPlayer = entries[0];
            String targetBingo = entries[1];

            // this whole for loop basically looks through all the players to find the person who completed the task, then finds the corresponding task, and assigns that task to the player
            for(Player player : self.getLobby().getPlayers()){
                if(player.getID().equals(targetPlayer)){
                    System.out.println("Found player!");
                    BingoTask[][] tasks = Gamemode.current.bingoTasks;
                    for(int i = 0; i<tasks.length; i++){
                        for(int j = 0; j<tasks[0].length; j++){
                            if(tasks[i][j].id.equals(targetBingo)){
                                System.out.println("Found bingo+owner!");
                                tasks[i][j].owner = player;
                                tasks[i][j].setCompleted(true);
                            }
                        }
                    }

                }
            }


        }

        else if (header.equals("LISTLOBBY")) { // for when outside a lobby. Some data can just not be sent.
            System.out.println("Caught lobby list: " + data);
            LinkedHashMap<String, Lobby> parsedLobbies = new LinkedHashMap();
            String[] lobbyEntries = data.split(";");

            for (String entry : lobbyEntries) {
                if (entry.isEmpty()) continue;

                String lobbyID = null;
                String lobbyName = null;
                boolean inGame = false;
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
                    else if (key.equals("ingame")) inGame = value.equals("true");
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
                    Lobby newLobby = new Lobby(lobbyName, inGame, hasPassword, admins, superAdmins, numPlayers, maxPlayers);
                    newLobby.setID(lobbyID);
                    parsedLobbies.put(lobbyID, newLobby);
                }
            }

            if (this.lobbyListCallback != null) {
                this.lobbyListCallback.accept(parsedLobbies);
            }

            System.out.println("LOBBIES: " + parsedLobbies);
        }
        else if (header.equals("CREATENOTIFY")) {
            // data = the new lobby's ID assigned by the server
            String newLobbyID = data;
            Lobby newLobby = new Lobby(newLobbyID, false, false, new ArrayList<String>(), new ArrayList<String>(), 1, 4);
            newLobby.setID(newLobbyID);
            lobbies.put(newLobbyID, newLobby);
            self.setLobby(newLobby);

            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Game.switchScene(InLobbyScene.class);
                }
            });

            if (onLobbyCreated != null) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        onLobbyCreated.run();
                    }
                });
            }
        }
        else if (header.equals("INFOLOBBY")) { // for when youre in a lobby, get more info
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
                int color = playerObj.has("color") ? playerObj.get("color").getAsInt() : 0;


                HeroClass heroClass = null;
                if (playerObj.has("class") && !playerObj.get("class").isJsonNull()) {
                    try {
                        heroClass = HeroClass.valueOf(playerObj.get("class").getAsString());
                    } catch (IllegalArgumentException e) {
                        heroClass = null;
                    }
                }

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
                if (superAdmins.contains(id)) {
                    found.level = 2;
                } else if (admins.contains(id)) {
                    found.level = 1;
                } else {
                    found.level = 0;
                }
                found.setClass(heroClass);
                found.color = color;
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
        else if (header.equals("LOGINNOTIFY")) {
            if (data.equals("success")) {
                if (onLoginSuccess != null) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            onLoginSuccess.run();
                        }
                    });
                }
            } else {
                if (onLoginFail != null) {
                    final String reason = data;
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            onLoginFail.accept(reason);
                        }
                    });
                }
            }
        }
        else if (header.equals("REGISTERNOTIFY")) {
            if (data.equals("success")) {
                if (onRegisterSuccess != null) onRegisterSuccess.run();
            } else {
                if (onRegisterFail != null) {
                    System.out.println("Failed to signup!");
                    final String reason = data;
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            onRegisterFail.accept(reason);
                        }
                    });
                }
            }
        }
        else if (header.equals("ISREADY")) {
            for (Player p : players) {
                if (p.getID().equals(data)) {
                    p.isReadied = !p.isReadied;
                    if(p.isReadied){
                        chat.add(new ChatMessage(p.getName()+" is ready!"));
                    }
                    if(!p.isReadied){
                        chat.add(new ChatMessage(p.getName()+" unchecked."));
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
            shouldFreeze = true;
        }

        else if (header.equals("STARTGAME")) {
            String[] parts = data.split(",", 2);
            long startTimeMs = (long) Double.parseDouble(parts[0].replace("STARTTIME=", ""));
            freezeUntil = startTimeMs;
            countdownUntil = startTimeMs;
            shouldCountdown = true;
             //I can't bother to figure out why but this doesn't work if I add it with 2+ players but works fine with 1 wtf.

            Gamemode.current = Gamemode.fromID(Gamemode.current.gamemodeID);
            if (parts.length > 1 && parts[1].startsWith("BINGODATA:")) {
                String bingoRaw = parts[1].replace("BINGODATA:", "");
                String[] taskEntries = bingoRaw.split(";");
                Gamemode.current.loadBingoFromNetwork(taskEntries);
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (Game.scene() instanceof GameScene) {
                            GameScene.resetBingoBoard();
                        }
                    }
                });
            }
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
                            Game.switchScene(LossScene.class);
                            GLog.n(Messages.get("amuletloss"), winners[0]); // I can take the first elements since there should only be 1 in this case.
                            System.out.println("Someone has acquired the amulet of yendor before you!");
                        }
                    });
                }
            }
            if (endType.equals("bingowin") || endType.equals("bingoblackout")) {
                if (Arrays.asList(winners).contains(self.getID())) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Game.switchScene(VictoryScene.class);
                        }
                    });
                } else {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Game.switchScene(LossScene.class);
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

    public void kickPlayer(Player target){
        if(self.isAdmin() && self.isHigherLevel(target)){
            send("KICKPLAYER:"+target.getID());
        }
    }

    public void banPlayer(Player target){
        if(self.isAdmin() && self.isHigherLevel(target)){
            send("BANPLAYER:"+target.getID());
        }
    }

    public void promotePlayer(Player target){
        if(self.isAdmin() && self.isHigherLevel(target)){
            send("PROMOTEPLAYER:"+target.getID());
        }
    }

    public void changeMaxPlayers(float maxPlayers){
        this.send("CHANGEMAXPLAYERCOUNT:"+ (int) maxPlayers);
    }
    public void sendGamemode(Gamemode gamemode){
        this.send("SETGAMEMODE:"+gamemode.gamemodeID);
    }

    public void completeBingoTask(BingoTask task){
        this.send("BINGOCOMPLETE:"+task.id);
    }

    public void requestLobbyInfo(Consumer<Lobby> callback) {
        this.lobbyInfoCallback = callback;
        this.send("INFOLOBBY:");
    }

    public void login(String username, String password, Runnable onSuccess, Consumer<String> onFail) {
        this.onLoginSuccess = onSuccess;
        this.onLoginFail = onFail;
        this.send("LOGIN:" + username + "," + password); // TODO MAKE SURE TO MAKE COMMAS IMPOSSIBLE
    }

    public void register(String username, String password, Runnable onSuccess, Consumer<String> onFail) {
        this.onRegisterSuccess = onSuccess;
        this.onRegisterFail = onFail;
        this.send("REGISTER:" + username + "," + password); // TODO MAKE SURE TO MAKE COMMAS IMPOSSIBLE
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
    public void setJoinErrorCallback(Consumer<String> callback) {
        this.onJoinError = callback;
    }
    public void setLevelChangedCallback(Runnable callback) {
        this.onLevelChanged = callback;
    }
    public void setDisconnectedCallback(Runnable callback) {
        this.onDisconnected = callback;
    }
    public void setLobbyCreatedCallback(Runnable callback) { this.onLobbyCreated = callback; }
    public void setClassUpdateCallback(Runnable callback) {
        this.onClassUpdate = callback;
    }
    public void setSettingsUpdate(Runnable callback) { this.onSettingsUpdate = callback; }



    public void createLobby(String lobbyName, String lobbyPassword){
        StringBuilder msg = new StringBuilder("CREATELOBBY:");
        msg.append("name="+lobbyName+";password="+lobbyPassword);
        this.send(msg.toString());
    }
    public void createLobby(String lobbyName){ // if there's no password in the server we're trying to create
        this.createLobby(lobbyName, "None");
    }

    public void joinLobby(String lobbyID, String lobbyPassword){
        pendingLobbyID = lobbyID;

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
        send("CHAT:"+self.getName()+"="+msg);
    }

}