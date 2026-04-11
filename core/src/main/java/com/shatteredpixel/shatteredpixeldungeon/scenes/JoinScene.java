package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.DataFetcher;
import com.shatteredpixel.shatteredpixeldungeon.networking.Lobby;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Random;
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

    StyledButton btnConnect = null;
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

        RenderedTextBlock title = null;
        title = renderTextBlock( Messages.get(this, "text"), 8 );
        title.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(title);



        btnConnect = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "join_server") ) {
            @Override
            protected void onClick() {
                btnConnect.enable(false);

            }
        };

        btnConnect.icon(Icons.STAIRS.get());
        btnConnect.setSize( WIDTH, BTN_HEIGHT );
        add( btnConnect );


        btnCreate = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "create_server") ) {
            @Override
            protected void onClick() {
                System.out.println("Create lobby button pressed");
                Game.switchScene(LobbyCreationScene.class);

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
                Game.switchScene(TitleScene.class);

            }
        };
        btnReturn.icon(Icons.EXIT.get());
        btnReturn.setSize( WIDTH, BTN_HEIGHT );
        add( btnReturn );





        title.setPos((w-title.width())/2, 30) ;
        btnCreate.setPos(w/6-btnConnect.width()/2, h-30);
        btnConnect.setPos(w/2-btnConnect.width()/2, h-30);
        btnReturn.setPos(5*w/6-btnConnect.width()/2, h-30);

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


        NetworkManager.INSTANCE.setJoinErrorCallback(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Wrong password!"); // replace with your popup later
                    }
                });
            }
        });

        fadeIn();

    }


    public void addLobby(String id, Lobby lobby) {
        this.lobbies.put(id, lobby);
        LobbyButton newLobbyButton = new LobbyButton(id, lobby.getName(), lobby.hasPassword());
        lobbyList.content().add(newLobbyButton);
        lobbyButtons.put(id, newLobbyButton);
    }

    private void refreshLobbyMenu(LinkedHashMap<String, Lobby> result) {
        for (Map.Entry<String, Lobby> entry : result.entrySet()) {
            String id = entry.getKey();
            if (!this.lobbies.containsKey(id)) {
                System.out.println("New lobby detected! Adding: " + entry.getValue().getName() + " - " + id);
                this.addLobby(id, entry.getValue());
            }
        }

        Iterator<Map.Entry<String, Lobby>> it = this.lobbies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Lobby> entry = it.next();
            if (!result.containsKey(entry.getKey())) {
                System.out.println("Deleting dead lobby: "+entry.getValue().getName()+" - "+entry.getKey());
                it.remove();
                refreshLobbyButtons();
            }
        }

        this.lobbies = result;
        System.out.println("Updated lobbies: " + lobbies);
    }

    public void refreshLobbyButtons() {
        Iterator<Map.Entry<String, LobbyButton>> it = lobbyButtons.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LobbyButton> entry = it.next();
            if (!this.lobbies.containsKey(entry.getKey())) {
                entry.getValue().destroy();
                it.remove();
            }
        }

        float y = 0;  // y=0 now, because positions are relative to the content, not the screen
        for (Map.Entry<String, LobbyButton> button : lobbyButtons.entrySet()) {
            button.getValue().setRect(0, y, lobbyList.width(), 30);
            y += 32;  // 30 height + 2 gap
        }

        // This is the critical line that enables scrolling
        lobbyList.content().setSize(lobbyList.width(), y);
    }


    private float timer = -5; // is this fine? I just want these to be called immediately basically, and then again every 5secs after.

    @Override
    public void update() {
        super.update();
        if(Game.timeTotal-timer >= 5) {
            timer=Game.timeTotal;
            System.out.println("Refreshing lobby menu...");
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
            });;

        }
    }
}