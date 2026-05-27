package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.Lobby;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;

public class JoinScene extends PixelScene{
    private static final int WIDTH			= 120;
    private static final int BTN_HEIGHT		= 20;
    private static final float SMALL_GAP	= 2;
    private static final float LARGE_GAP	= 8;




    {
        inGameScene = true;
    }

    StyledButton btnRefresh = null;
    StyledButton btnCreate = null;
    StyledButton btnReturn = null;

    ScrollPane lobbyList = null;

    RectF insets;
    int w;
    int h;


    LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();
    LinkedHashMap<String, LobbyButton> lobbyButtons = new LinkedHashMap<>();


    @Override
    public void create() {
        super.create();

        insets = getCommonInsets();
        w = (int) (Camera.main.width - insets.left + insets.right);
        h = (int) (Camera.main.height - insets.top + insets.bottom);



        TitleBackground BG = new TitleBackground( Camera.main.width, Camera.main.height);
        add( BG );

        RenderedTextBlock title = null;
        title = renderTextBlock( Messages.get(this, "text"), 8 );
        title.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(title);



        btnRefresh = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "refresh") ) {
            @Override
            protected void onClick() {
                NetworkManager.INSTANCE.listLobbies(new Consumer<LinkedHashMap<String, Lobby>>() {
                    @Override
                    public void accept(LinkedHashMap<String, Lobby> result) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                refreshLobbyMenu(result);
                                refreshLobbyButtons();
                            }
                        });
                    }
                });
            }
        };

        btnRefresh.icon(Icons.NEWS.get());
        btnRefresh.setSize( WIDTH, BTN_HEIGHT );
        add(btnRefresh);


        btnCreate = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "create_server") ) {
            @Override
            protected void onClick() {
                System.out.println("Create lobby button pressed");
                ShatteredPixelDungeon.switchNoFade(LobbyCreationScene.class);

            }
        };
        btnCreate.icon(Icons.PREFS.get());
        btnCreate.setSize( WIDTH, BTN_HEIGHT );
        add( btnCreate );




        btnReturn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "return") ) {
            @Override
            protected void onClick() {
                btnReturn.enable(false);
                System.out.println("Return to title button pressed");
                ShatteredPixelDungeon.switchNoFade(AccountOptionsScene.class);

            }
        };
        btnReturn.icon(Icons.EXIT.get());
        btnReturn.setSize( WIDTH, BTN_HEIGHT );
        add( btnReturn );





        title.setPos((w-title.width())/2, 30) ;
        btnCreate.setPos(w/6- btnRefresh.width()/2, h-30);
        btnRefresh.setPos(w/2- btnRefresh.width()/2, h-30);
        btnReturn.setPos(5*w/6- btnRefresh.width()/2, h-30);

        lobbyList = new ScrollPane(new Component());
        add(lobbyList);

// Position it between the title and the bottom buttons
// title is at y=30, buttons are at h-30, so the space between is roughly:
        lobbyList.setRect(
                insets.left,       // x
                50,                // y (just below title)
                w - insets.left - insets.right,  // width
                h - 80             // height (leaves room for title above and buttons below)
        );
        lobbyList.scrollTo(0, 0);


        NetworkManager.INSTANCE.setJoinErrorCallback(new Consumer<String>() {
            @Override
            public void accept(String reason) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if(ShatteredPixelDungeon.scene() == JoinScene.this) {
                            add(new WndMessage(Messages.get(JoinScene.class, reason)));
                        }
                    }
                });
            }
        });

        this.lobbies = NetworkManager.INSTANCE.getLobbies();
        for (Map.Entry<String, Lobby> entry : this.lobbies.entrySet()) {
            addLobby(entry.getKey(), entry.getValue());
        }
        refreshLobbyButtons();
        fadeIn();

    }


    public void addLobby(String id, Lobby lobby) {
        this.lobbies.put(id, lobby);
        if(!lobby.isInGame()) {
            LobbyButton newLobbyButton = new LobbyButton(lobby);
            lobbyList.content().add(newLobbyButton);
            lobbyButtons.put(id, newLobbyButton);
        }
    }

    private void refreshLobbyMenu(LinkedHashMap<String, Lobby> result) {
        for (Map.Entry<String, Lobby> entry : result.entrySet()) {
            String id = entry.getKey();
            Lobby updatedLobby = entry.getValue();

            if (!this.lobbies.containsKey(id)) {
                // Brand new lobby, addLobby handles the inGame check
                System.out.println("New lobby detected! Adding: " + updatedLobby.getName() + " - " + id);
                this.addLobby(id, updatedLobby);
            } else {
                // Lobby already known — check if inGame status changed
                Lobby existingLobby = this.lobbies.get(id);
                System.out.println("Lobby " + id + " - existing inGame: " + existingLobby.isInGame() + ", updated inGame: " + updatedLobby.isInGame());

                if (!existingLobby.isInGame() && updatedLobby.isInGame()) {
                    // Lobby just started a game, remove its button
                    System.out.println("Removing button for lobby: " + id);
                    LobbyButton btn = lobbyButtons.get(id);
                    if (btn != null) {
                        btn.destroy();
                        lobbyButtons.remove(id);
                    }
                } else if (existingLobby.isInGame() && !updatedLobby.isInGame()) {
                    // Lobby just finished a game, add its button back
                    LobbyButton newBtn = new LobbyButton(updatedLobby);
                    lobbyList.content().add(newBtn);
                    lobbyButtons.put(id, newBtn);
                }
            }
        }

        // Remove lobbies that no longer exist at all
        Iterator<Map.Entry<String, Lobby>> it = this.lobbies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Lobby> entry = it.next();
            if (!result.containsKey(entry.getKey())) {
                System.out.println("Deleting dead lobby: " + entry.getValue().getName() + " - " + entry.getKey());
                LobbyButton btn = lobbyButtons.get(entry.getKey());
                if (btn != null) {
                    btn.destroy();
                    lobbyButtons.remove(entry.getKey());
                }
                it.remove();
            }
        }

        this.lobbies = result;
        System.out.println("Updated lobbies: " + lobbies);
    }

    public void refreshLobbyButtons() {
        float y = 0;
        for (Map.Entry<String, LobbyButton> entry : lobbyButtons.entrySet()) {
            String lobbyID = entry.getKey();
            LobbyButton button = entry.getValue();
            Lobby lobby = this.lobbies.get(lobbyID);

            if (lobby == null) continue;

            float width = lobbyList.width() - 20;
            float x = (lobbyList.width() - width) / 2;
            button.setRect(x, y, width, 30);
            button.setLobby(this.lobbies.get(lobbyID));
            button.layout();
            y += 32;
        }

        lobbyList.content().setSize(lobbyList.width(), y);
    }


    private static float timer = -3;

    @Override
    public void update() {
        super.update();
        if(Game.timeTotal-timer >= 3) {
            timer=Game.timeTotal;
            System.out.println("Refreshing lobby menu...");
            NetworkManager.INSTANCE.listLobbies(new Consumer<LinkedHashMap<String, Lobby>>() {
                @Override
                public void accept(LinkedHashMap<String, Lobby> result) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            if (ShatteredPixelDungeon.scene() == JoinScene.this) {
                                refreshLobbyMenu(result);
                                refreshLobbyButtons();
                            }
                        }
                    });
                }
            });;

        }
    }
}