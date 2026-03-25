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

    RectF insets = getCommonInsets();
    int w = (int) (Camera.main.width - insets.left + insets.right);
    int h = (int) (Camera.main.height - insets.top + insets.bottom);

    String promptTitle = "Join server";
    String promptText = "Enter the IP of the server.";

    LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<>();
    LinkedHashMap<String, LobbyButton> lobbyButtons = new LinkedHashMap<>();


    @Override
    public void create() {
        super.create();

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
                System.out.println("Create lobby button pressed(no functionality exists yet dum dum)");
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




        fadeIn();
    }

    public void addLobby(String id, Lobby lobby) {
        this.lobbies.put(id, lobby);
        LobbyButton newLobbyButton = new LobbyButton(id, lobby.getName(), lobby.hasPassword());
        newLobbyButton.setRect(0, 0, w - insets.right - insets.left - 50, 30);
        add(newLobbyButton);
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
                System.out.println("Deleting dead lobby: " + entry.getValue().getName() + " - " + entry.getKey());
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
                entry.getValue().destroy(); // remove from scene
                it.remove();
            }
        }
        int y = 50;
        for (Map.Entry<String, LobbyButton> button : lobbyButtons.entrySet()) {
            button.getValue().realign(y, w);
            y+=40;
        }
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