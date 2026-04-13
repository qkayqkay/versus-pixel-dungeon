package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.Lobby;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InLobbyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.RectF;

import static com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera;

public class LobbyButton extends StyledButton {
    private String name;
    private String ID;
    private Lobby lobby;
    private boolean hasPassword;
    private RenderedTextBlock playerCount;
    private WndTextInput passwordInput;




    public LobbyButton(Lobby lobby){
        super(Chrome.Type.GREY_BUTTON_TR, lobby.getName());
        this.name = lobby.getName();
        this.ID = lobby.getID();
        this.hasPassword = lobby.hasPassword();
        this.leftJustify = true;
        this.lobby = lobby;

        this.playerCount = PixelScene.renderTextBlock("", 7);
        add(playerCount);
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }


    @Override
    public void layout() {
        super.layout();
        if (playerCount == null || lobby == null) return;
        playerCount.text(lobby.getPlayerCount() + "/" + lobby.getMaxPlayers());
        playerCount.layout();
        playerCount.setPos(
                this.left() + this.width() - playerCount.width() - 4,
                this.top() + (this.height() - playerCount.height())/2
        );
    }


    @Override
    protected void onClick() {
        if(this.hasPassword==true) {
            passwordInput = new WndTextInput(
                    Messages.get(LobbyButton.class, "passwordtitle"),
                    Messages.get(LobbyButton.class, "passworddescription"),
                    "",
                    20,
                    false,
                    Messages.get(CustomNoteButton.CustomNoteWindow.class, "confirm"),
                    Messages.get(CustomNoteButton.CustomNoteWindow.class, "cancel")) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        NetworkManager.INSTANCE.joinLobby(LobbyButton.this.ID, text);

                    }
                }
            };
            add(passwordInput);
        }
        else{
            NetworkManager.INSTANCE.joinLobby(this.ID);
        }

    }
}

