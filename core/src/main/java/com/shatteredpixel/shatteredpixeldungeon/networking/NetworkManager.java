package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;

public enum NetworkManager {

    INSTANCE;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isListening = false;
    private Consumer<LinkedHashMap<String, Lobby>> lobbyCallback;
    private LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();

    public String playerID;

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
                     playerID = data;
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

    private void handleIncomingMessage(String message) {
        System.out.println("Caught incoming data: "+message);
        String[] headerData = message.split(":", 2);
        String header = headerData[0];
        String data = headerData[1];

        if (header.equals("BROADCAST")) {
            this.broadcast(data);
        }
        else if (header.equals("LISTLOBBY")) {
            System.out.println("Caught lobby list: " + data);
            LinkedHashMap<String, Lobby> parsedLobbies = new LinkedHashMap<>();
            String[] lobbyPairs = data.split(";");
            for (String pair : lobbyPairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length < 2) continue;
                String lobbyID = keyValue[0];
                String[] lobbyData = keyValue[1].split(",", 2);
                String lobbyName = lobbyData[0];
                boolean hasPassword = lobbyData.length > 1 && lobbyData[1].equals("true");
                parsedLobbies.put(lobbyID, new Lobby(lobbyName, hasPassword));
            }
            if (lobbyCallback != null) {
                lobbyCallback.accept(parsedLobbies);
            }
            System.out.println("LOBBIES: " + parsedLobbies);
        }
        else if (header.equals("READYGAME")) {
            String[] dataList = data.split("=");
            Long seed = Long.parseLong(dataList[1]);

            System.out.println("Server is starting the game! Seed: " + seed);
            Dungeon.initSeed(seed);

            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Game.switchScene(HeroSelectScene.class);
                }
            });
        }

        else if (header.equals("STARTGAME")) {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Game.switchScene(InterlevelScene.class);
                }
            });
        }
    }

    private void broadcast(String message){
        System.out.println("SERVER BROADCAST: " + message);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void isReady(){
        this.send("ISREADY:");
    }

    public void disconnect() {
        try {
            isListening = false;
            if (socket != null) socket.close();
        } catch (Exception e) { /* ignore */ }
    }

    public void listLobbies(Consumer<LinkedHashMap<String, Lobby>> callback) {
        this.lobbyCallback = new Consumer<LinkedHashMap<String, Lobby>>() {
            @Override
            public void accept(LinkedHashMap<String, Lobby> result) {
                lobbies = result;
                if (callback != null) callback.accept(result);
            }
        };
        this.send("LISTLOBBY:");
    }
        // TODO, maybe add data values like playercount = true to include the playercount?


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
        this.send(msg.toString());
    }
    public void joinLobby(String lobbyID){ // if there's no password in the server we're trying to join
        this.joinLobby(lobbyID, "None");
    }

    public LinkedHashMap<String, Lobby> getLobbies() {
        return lobbies;
    }

}