package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.DataFetcher;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextInput;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

public class LobbyCreationScene extends PixelScene{
    private static final int WIDTH			= 120;
    private static final float SMALL_GAP	= 2;
    private static final float LARGE_GAP	= 8;



    {
        inGameScene = true;
    }

    StyledButton btnCreate = null;

    @Override
    protected void onBackPressed() {}

    @Override
    public void create() {
        super.create();
        RectF insets = getCommonInsets();
        int w = (int) (Camera.main.width - insets.left + insets.right);
        int h = (int) (Camera.main.height - insets.top + insets.bottom);


        RenderedTextBlock title = renderTextBlock( Messages.get(this, "title"), 12 );
        title.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(title);

        RenderedTextBlock nameText = renderTextBlock( Messages.get(this, "name"), 8 );
        nameText.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(nameText);

        RenderedTextBlock passwordCheck = renderTextBlock( Messages.get(this, "password_check"), 8 );
        passwordCheck.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(passwordCheck);

        RenderedTextBlock passwordText = renderTextBlock( Messages.get(this, "password_input"), 8 );
        passwordText.maxWidth( PixelScene.landscape() ? 2*WIDTH-4 : WIDTH);
        add(passwordText);



        TextInput nameInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8,  uiCamera.zoom);
        add(nameInput);

        TextInput passwordInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8,  uiCamera.zoom);
        add(passwordInput);


        CheckBox checkBox = new CheckBox("X");
        checkBox.setPos(3*w/4, 100);
        add(checkBox);


        btnCreate = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "create") ) {
            @Override
            protected void onClick() {
                btnCreate.enable(false);
                System.out.println("Name: "+nameInput.getText()+" and password: "+passwordInput.getText());
                NetworkManager.INSTANCE.createLobby(nameInput.getText(), passwordInput.getText());
                Game.switchScene(JoinScene.class);

            }
        };
        btnCreate.icon(Icons.STAIRS.get());
        btnCreate.setSize( WIDTH, 20 );
        add(btnCreate);


        StyledButton btnBack = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                Game.switchScene(JoinScene.class);

            }
        };
        btnBack.icon(Icons.EXIT.get());
        btnBack.setSize( WIDTH, 20 );
        add(btnBack);








        title.setPos((w-title.width())/2, 20) ;
        nameText.setPos(w/4-nameText.width()/2, 60) ;
        passwordCheck.setPos(w/4-nameText.width()/2, 100) ;
        passwordText.setPos(w/4-nameText.width()/2, 140) ;

        nameInput.setRect(3*w/4-40,60-25/2, 80, 25);
        passwordInput.setRect(3*w/4-40,140-25/2, 80, 25);



        btnCreate.setPos((w- btnCreate.width())/2, h-30);
        btnBack.setPos(w- btnBack.width()-insets.right, btnBack.height()+insets.top);

        fadeIn();
    }


    private float timer = 0;

    @Override
    public void update() {
        super.update();

        if ((timer -= Game.elapsed) < 0) {
            timer = Random.Float( 0.5f, 5f );
        }
    }
}