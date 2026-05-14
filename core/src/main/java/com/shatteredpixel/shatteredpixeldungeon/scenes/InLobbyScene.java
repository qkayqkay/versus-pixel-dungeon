package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.function.Consumer;

public class InLobbyScene extends PixelScene {

    private RectF insets;
    private StyledButton btnReturn;
    private NinePatch chatPanel;
    private ScrollPane chatScroll;

    private static Lobby lobby;
    private StyledButton btnReady;
    private ArrayList<LobbyPlayerBtn> playerBtns = new ArrayList<>();
    private ScrollPane playersScroll;
    private NinePatch playersPanel;

    private TextInput chatField;
    private StyledButton btnSend;

    // Right panel split into two halves
    private NinePatch settingsPanel;
    private NinePatch classPanel;
    private ArrayList<ClassSelectBtn> classBtns = new ArrayList<>();

    private StyledButton gamemodeButton;

    private float w;
    private float h;
    Player self = NetworkManager.INSTANCE.self;

    @Override
    protected void onBackPressed() {}

    //player list buttons(middle panel)
    private class LobbyPlayerBtn extends StyledButton {

        static final int HEIGHT = 24;

        private Player p;

        LobbyPlayerBtn(Player p) {
            super(Chrome.Type.GREY_BUTTON_TR, p.getName());
            this.p = p;

            if (p.cl != null) {
                icon(new Image(p.cl.spritesheet(), 0, 90, 12, 15));
            }
        }

        @Override
        protected void onClick() {
            super.onClick();
            System.out.println(p.getName());
        }
    }

    private void updatePlayerBtns() {
        if (lobby == null) return;

        for (LobbyPlayerBtn btn : playerBtns) {
            btn.killAndErase();
        }
        playerBtns.clear();

        Component content = playersScroll.content();
        content.clear();

        float margin = 4f;
        float btnWidth  = playersScroll.width() - margin * 2;
        float btnHeight = LobbyPlayerBtn.HEIGHT;
        float y = 2f;

        for (Player p : lobby.getPlayers()) {
            LobbyPlayerBtn btn = new LobbyPlayerBtn(p);
            btn.setSize(btnWidth, btnHeight);
            btn.setPos(margin, y);
            content.add(btn);
            playerBtns.add(btn);
            y += btnHeight + 2f;
        }

        content.setSize(playersScroll.width(), y);
    }

    // class selector buttons(bottom-right panel)
    private class ClassSelectBtn extends StyledButton {

        private HeroClass cl;

        ClassSelectBtn(HeroClass cl) {
            super(Chrome.Type.GREY_BUTTON_TR, "");
            this.cl = cl;
            icon(new Image(cl.spritesheet(), 0, 90, 12, 15));
        }

        @Override
        public void update() {
            super.update();
            if (cl == GamesInProgress.selectedClass) {
                icon.brightness(1f);
            } else {
                icon.brightness(0.6f);
            }
        }

        @Override
        protected void onClick() {
            super.onClick();
            GamesInProgress.selectedClass = cl;
            NetworkManager.INSTANCE.updateClass();
            GamesInProgress.randomizedClass = false;
        }
    }

    //scene creation
    @Override
    public void create() {
        super.create();
        TitleBackground BG = new TitleBackground( Camera.main.width, Camera.main.height);
        add( BG );

        insets = Game.platform.getSafeInsets(PlatformSupport.INSET_BLK).scale(1f / defaultZoom);

        w = (Camera.main.width  - insets.left - insets.right);
        h = (Camera.main.height - insets.top  - insets.bottom);

        //chat panel
        chatPanel = Chrome.get(Chrome.Type.TOAST);
        chatPanel.size(w / 5 - 5, h - 50);
        chatPanel.x = 3 + insets.left;
        chatPanel.y = (h - chatPanel.height()) / 2 + insets.top;
        add(chatPanel);

        chatField = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 6, uiCamera.zoom) {
            @Override
            public void onKeyTyped(char c) {
                if (c == '\r' || c == '\n') enterPressed();
            }
            @Override
            public void enterPressed() {
                String msg = getText();
                if (!msg.equals("")) {
                    NetworkManager.INSTANCE.sendMessage(msg);
                    clearText();
                }
            }
        };
        chatField.setSize(chatPanel.width() - 5, 25);
        chatField.setPos(
                chatPanel.x + (chatPanel.width() - chatField.width()) / 2,
                chatPanel.y + chatPanel.height() - chatField.height() - 30);
        add(chatField);

        btnSend = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "sendmessage")) {
            @Override
            protected void onClick() {
                String msg = chatField.getText();
                NetworkManager.INSTANCE.sendMessage(msg);
                chatField.clearText();
            }
        };
        btnSend.setSize(chatPanel.width() - 5, 20);
        btnSend.setPos(
                chatPanel.x + (chatPanel.width() - btnSend.width()) / 2,
                chatPanel.y + chatPanel.height() - btnSend.height() - 5);
        add(btnSend);

        chatScroll = new ScrollPane(new Component());
        add(chatScroll);
        chatScroll.setRect(
                chatPanel.x + chatPanel.marginLeft(),
                chatPanel.y + chatPanel.marginTop(),
                chatPanel.innerWidth(),
                chatPanel.innerHeight() - chatField.height() - btnSend.height() - 10);


        playersPanel = Chrome.get(Chrome.Type.TOAST);
        playersPanel.size(2 * w / 5 - 5, h - 70);
        playersPanel.x = chatPanel.x + chatPanel.width() + 5;
        playersPanel.y = (h - playersPanel.height()) / 2 + insets.top;
        add(playersPanel);

        playersScroll = new ScrollPane(new Component());
        add(playersScroll);
        playersScroll.setRect(
                playersPanel.x + playersPanel.marginLeft(),
                playersPanel.y + playersPanel.marginTop(),
                playersPanel.innerWidth(),
                playersPanel.innerHeight());

        float rightX      = playersPanel.x + playersPanel.width() + 5;
        float rightWidth  = 2 * w / 5 - 5;
        float rightHeight = h - 70;
        float rightMidY   = (h - rightHeight) / 2 + insets.top;

        float gap         = 4f;
        float halfHeight  = (rightHeight - gap) / 2f;

        settingsPanel = Chrome.get(Chrome.Type.TOAST);
        settingsPanel.size(rightWidth, halfHeight);
        settingsPanel.x = rightX;
        settingsPanel.y = rightMidY;
        add(settingsPanel);

        gamemodeButton = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Gamemode: "+Gamemode.current.gamemodeName) {
            private Dropdown dropdown = null;

            @Override
            protected void onClick() {
                if (dropdown != null) {
                    dropdown.close();
                    dropdown = null;
                    return;
                }
                dropdown = new Dropdown(Gamemode.listGamemodes(), new Dropdown.OnGamemodeSelected() {
                    @Override
                    public void onSelected(Gamemode g) {
                        Gamemode.current = g;
                        updateSettingButtons();
                        NetworkManager.INSTANCE.sendGamemode(g);
                        if(dropdown != null) {
                            dropdown.close();
                            dropdown = null;
                        }
                    }
                });
                add(dropdown);
                dropdown.layout(
                        settingsPanel.x + settingsPanel.marginLeft(),
                        settingsPanel.y + settingsPanel.marginTop() + 18f,
                        settingsPanel.innerWidth());
            }
        };
        gamemodeButton.setRect(
                settingsPanel.x + settingsPanel.marginLeft(),
                settingsPanel.y + settingsPanel.marginTop(),
                settingsPanel.innerWidth(),
                16f);
        add(gamemodeButton);

        classPanel = Chrome.get(Chrome.Type.TOAST);
        classPanel.size(rightWidth, halfHeight);
        classPanel.x = rightX;
        classPanel.y = rightMidY + halfHeight + gap;
        add(classPanel);

        // build class buttons
        classBtns.clear();
        for (HeroClass cl : HeroClass.values()) {
            ClassSelectBtn btn = new ClassSelectBtn(cl);
            add(btn);
            classBtns.add(btn);
        }
        layoutClassBtns();

        //title
        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(InLobbyScene.class, "title"), 12);
        title.setPos((w - title.width()) / 2 + insets.left, 10 + insets.top);
        add(title);

        //ready/start button
        btnReady = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                if (lobby != null && lobby.isAdmin()) {
                    NetworkManager.INSTANCE.initiateStart();
                } else {
                    NetworkManager.INSTANCE.isReady(); //isReadied is flipped in here
                }
            }
        };
        btnReady.icon(Icons.EXIT.get());
        btnReady.setSize(70, 20);
        btnReady.setPos((w - btnReady.width()) / 2 + insets.left, insets.top + h - btnReady.height() - 10);
        add(btnReady);
        updateReadyButton();

        // return button(to leave the lobby)
        btnReturn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                btnReturn.enable(false);
                lobby = null;
                NetworkManager.INSTANCE.leaveLobby();
                Game.switchScene(JoinScene.class);
            }
        };
        btnReturn.icon(Icons.EXIT.get());
        btnReturn.setSize(20, 20);
        btnReturn.setPos(insets.left + w - 25, insets.top);
        add(btnReturn);

        // callbacks
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

        NetworkManager.INSTANCE.setClassUpdateCallback(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (ShatteredPixelDungeon.scene() == InLobbyScene.this) {
                            updatePlayerBtns();
                        }
                    }
                });
            }
        });

        NetworkManager.INSTANCE.setSettingsUpdate(new Runnable() {
            @Override
            public void run() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (ShatteredPixelDungeon.scene() == InLobbyScene.this) {
                            updateSettingButtons();
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
                            updateSettingButtons();
                        }
                    }
                });
            }
        });

        updateChat();
        updateReadyButton();
        updatePlayerBtns();
        fadeIn();
    }

    // class panel, with a 2 column 3 row grid
    private void layoutClassBtns() {
        if (classBtns.isEmpty()) return;

        float innerX = classPanel.x + classPanel.marginLeft();
        float innerY = classPanel.y + classPanel.marginTop();
        float innerW = classPanel.innerWidth();
        float innerH = classPanel.innerHeight();

        int cols     = 3;
        int rows     = (int) Math.ceil((float) classBtns.size() / cols);
        float btnW   = (innerW - (cols - 1) * 2f) / cols;
        float btnH   = (innerH - (rows - 1) * 2f) / rows;

        for (int i = 0; i < classBtns.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            ClassSelectBtn btn = classBtns.get(i);
            btn.setRect(
                    innerX + col * (btnW + 2f),
                    innerY + row * (btnH + 2f),
                    btnW,
                    btnH);
        }
    }


    private void updateReadyButton() {
        if (lobby != null && lobby.isAdmin()) {
            btnReady.text(Messages.get(InLobbyScene.class, "start"));
        } else {
            btnReady.text(Messages.get(InLobbyScene.class, "ready"));
        }
    }

    private void onLobbyLoaded() {
        updateReadyButton();
        updatePlayerBtns();
        updateSettingButtons();
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

    private void updateSettingButtons() {
        gamemodeButton.text("Gamemode: " + Gamemode.current.gamemodeName);
        if(!NetworkManager.INSTANCE.self.isAdmin()){
            gamemodeButton.enable(false);
        }
        else{
            gamemodeButton.enable(true);
        }
    }

    private static float timer = -4; // so it's instant

    @Override
    public void update() {
        super.update();
        if (Game.timeTotal - timer >= 4) {
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
                            }
                        }
                    });
                }
            });
        }
    }
}