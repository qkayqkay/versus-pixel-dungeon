package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InLobbyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.RectF;

public class LobbyButton extends StyledButton {
    private String name;
    private String ID;
    private boolean hasPassword;
    private RenderedTextBlock playerCount;
    private WndTextInput passwordInput;



    public LobbyButton(String ID, String name, boolean hasPassword){
        super(Chrome.Type.GREY_BUTTON_TR, name );
        this.name = name;
        this.ID = ID;
        this.hasPassword = hasPassword;
        if(hasPassword==true){
            System.out.println(name+" has password");
        }
    }

    public void realign(int initialY, int screenWidth){ //prob another way to pass in the width but I HATE GUI ARGHHHH
            this.setRect(
                    (screenWidth-this.width())/2,
                    (height()/3f - text.height()/2f) + initialY, this.width(), this.height()
            );
    }

    @Override
    protected void onClick() {
        if(this.hasPassword==true) {
            passwordInput = new WndTextInput(
                    Messages.get(this, "passwordTitle"), // TODO: Why isn't this working?
                    Messages.get(this, "passwordDescription"),
                    "",
                    20,
                    false,
                    Messages.get(CustomNoteButton.CustomNoteWindow.class, "confirm"),
                    Messages.get(CustomNoteButton.CustomNoteWindow.class, "cancel")) {
                @Override
                public void onSelect(boolean positive, String text) {
                    System.out.println("text size is: "+passwordInput.textSize);
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


            /*playerCount = new RenderedTextBlock("this is a text!", 10);
            playerCount.setPos(
                    30,30
            );
            addToFront(playerCount);*/ // isnt fucking working for who knows what reason.
