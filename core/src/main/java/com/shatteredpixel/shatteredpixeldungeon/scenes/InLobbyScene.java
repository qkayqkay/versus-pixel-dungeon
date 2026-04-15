package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.ChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.networking.Lobby;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.networking.Player;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class  InLobbyScene extends PixelScene {

    private RectF insets;
    private StyledButton btnReturn;
    private NinePatch chatPanel;
    private ScrollPane chatScroll;

    private Lobby lobby; // lobby that represents this server
    private StyledButton btnReady;
    private ArrayList<Image> playerImages = new ArrayList<>();
    private TextInput chatField;
    private StyledButton btnSend;
    private float w;
    private float h;
    Player self = NetworkManager.INSTANCE.self;

    @Override // so basically when clicking backspace in this game, it actually closes it. To avoid this, any scene that has a text input does something like this.
    protected void onBackPressed() {}

    private void updatePlayerImages() {
    }

    @Override
    public void create() {
        super.create();

        insets = Game.platform.getSafeInsets(PlatformSupport.INSET_BLK).scale(1f / defaultZoom);

        w = (Camera.main.width - insets.left - insets.right);
        h = (Camera.main.height - insets.top - insets.bottom);

        chatPanel = Chrome.get(Chrome.Type.TOAST);
        NinePatch playersPanel = Chrome.get(Chrome.Type.TOAST);
        NinePatch settingsPanel = Chrome.get(Chrome.Type.TOAST);

        for (Image img : playerImages) {

        }


        chatPanel.size(w / 5 - 5, h - insets.top - insets.bottom - 50);
        chatPanel.x = 3 + insets.right;
        chatPanel.y = (h - chatPanel.height()) / 2;
        add(chatPanel);



        playersPanel.size(2 * w / 5 - 5, h - insets.top - insets.bottom - 70);
        playersPanel.x = chatPanel.x + chatPanel.width() + 5;
        playersPanel.y = (h - playersPanel.height()) / 2;
        add(playersPanel);

        settingsPanel.size(2 * w / 5 - 5, h - insets.top - insets.bottom - 70);
        settingsPanel.x = playersPanel.x + playersPanel.width() + 5;
        settingsPanel.y = (h - settingsPanel.height()) / 2;
        add(settingsPanel);

        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(InLobbyScene.class, "title"), 12);
        title.setSize(30, 15);
        title.setPos((w - title.width()) / 2, 10 + insets.top);
        add(title);

        btnReady = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                if (lobby != null && lobby.isAdmin()) {
                    NetworkManager.INSTANCE.initiateStart();
                } else {
                    self.isReadied = !self.isReadied;
                    NetworkManager.INSTANCE.isReady();
                    System.out.println("Readied: " + self.isReadied);
                }
            }
        };
        btnReady.icon(Icons.EXIT.get());
        btnReady.setSize(70, 20);
        btnReady.setPos((w - btnReady.width())/2, h-btnReady.height()-10);
        add(btnReady);

        if (lobby != null && lobby.isAdmin()) {
            btnReady.text(Messages.get(InLobbyScene.class, "start"));
        } else {
            btnReady.text(Messages.get(InLobbyScene.class, "ready"));
        }


        btnReturn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                btnReturn.enable(false);
                NetworkManager.INSTANCE.leaveLobby();
                Game.switchScene(JoinScene.class);

            }
        };
        btnReturn.icon(Icons.EXIT.get());
        btnReturn.setSize(20, 20);
        btnReturn.setPos(w - 25, 0);
        add(btnReturn);

        chatField = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 6,  uiCamera.zoom);
        chatField.setSize(chatPanel.width()-5, 25);
        chatField.setPos(chatPanel.x+(chatPanel.width()-chatField.width())/2,chatPanel.y+chatPanel.height()-chatField.height()-30);
        add(chatField);



        btnSend = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "sendmessage") ) {
            @Override
            protected void onClick() {
                String msg = chatField.getText();
                System.out.println(msg);
                NetworkManager.INSTANCE.sendMessage(msg);
                chatField.clearText();
            }
        };
        btnSend.icon(Icons.STAIRS.get());
        btnSend.setSize( chatPanel.width()-5, 20 );
        btnSend.setPos(chatPanel.x+(chatPanel.width()-btnSend.width())/2, chatPanel.y+chatPanel.height()-btnSend.height()-5);
        add(btnSend); //todo icon

        chatScroll = new ScrollPane(new Component());
        add(chatScroll);
        chatScroll.setRect(
                chatPanel.x + chatPanel.marginLeft(),
                chatPanel.y + chatPanel.marginTop(),
                chatPanel.innerWidth(),
                chatPanel.innerHeight() - chatField.height() - btnSend.height() - 10
        );

        NetworkManager.INSTANCE.setChatCallback(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (ShatteredPixelDungeon.scene() == InLobbyScene.this) {
                            updateChat();
                        }
                    }
                });
            }
        });
        NetworkManager.INSTANCE.setLevelChangedCallback(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (ShatteredPixelDungeon.scene() == InLobbyScene.this) {
                            updateReadyButton();
                        }
                    }
                });
            }
        });

        updateChat();
        updateReadyButton();
        fadeIn();
    }


    private void updateReadyButton() {
        if (lobby != null && lobby.isAdmin()) {
            System.out.println("Updating start button  to start!");
            btnReady.text(Messages.get(InLobbyScene.class, "start"));
        } else {
            btnReady.text(Messages.get(InLobbyScene.class, "ready"));
        }
    }
    private static float timer = -5; // is this fine? I just want these to be called immediately basically, and then again every 5secs after.

    private void onLobbyLoaded() {
        updateReadyButton();
    }

    private void updateChat() {
        if (ShatteredPixelDungeon.scene() == this) {
            Component content = chatScroll.content();
            content.clear();

            float y = 0;
            for (ChatMessage entry : NetworkManager.INSTANCE.chat) {
                RenderedTextBlock message;
                if (!entry.isServerMessage) {
                    message = PixelScene.renderTextBlock(
                            entry.getAuthor().getID() + ": " + entry.getMessage(), 6);
                } else {
                    message = PixelScene.renderTextBlock(
                            "SERVER: " + entry.getMessage(), 6);
                }
                message.maxWidth((int) chatScroll.width());
                message.setPos(0, y);
                content.add(message);
                y += message.height() + 2;
            }

            content.setSize(chatScroll.width(), y);
            chatScroll.scrollTo(0, y);
        }
    }

    @Override
    public void update() {
        super.update();
        if (Game.timeTotal - timer >= 5) {
            timer = Game.timeTotal;
            updateChat();
            NetworkManager.INSTANCE.requestLobbyInfo(new Consumer<Lobby>() {
                @Override
                public void accept(Lobby result) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            if (ShatteredPixelDungeon.scene() == InLobbyScene.this) {
                                lobby = result;
                                onLobbyLoaded();
                                updatePlayerImages();
                            }
                        }
                    });
                }
            });
        }
    }
}