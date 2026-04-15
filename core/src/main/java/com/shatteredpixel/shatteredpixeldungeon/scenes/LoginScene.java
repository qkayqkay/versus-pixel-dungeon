package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextInput;
import com.watabou.utils.RectF;

import java.util.function.Consumer;

public class LoginScene extends PixelScene {

    RectF insets;
    int w;
    int h;
    StyledButton btnLogin;

    
    @Override
    protected void onBackPressed() {}

    @Override
    public void create() {
        super.create();
        RectF insets = getCommonInsets();
        w = (int) (Camera.main.width - insets.left + insets.right);
        h = (int) (Camera.main.height - insets.top + insets.bottom);


        RenderedTextBlock title = renderTextBlock( Messages.get(this, "title"), 12 );
        add(title);

        RenderedTextBlock usernameText = renderTextBlock( Messages.get(this, "username"), 8 );
        add(usernameText);


        RenderedTextBlock passwordText = renderTextBlock( Messages.get(this, "password"), 8 );
        add(passwordText);



        TextInput passwordInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8,  uiCamera.zoom);
        add(passwordInput);

        TextInput usernameInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8,  uiCamera.zoom);
        add(usernameInput); // adding username last so thats the one with focus first


        RenderedTextBlock errorText = renderTextBlock("", 8);
        errorText.hardlight(0xFF4444); // red colour for errors
        add(errorText);


        btnLogin = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "login") ) {
            @Override
            protected void onClick() {
                System.out.println("username: "+usernameInput.getText()+" and password: "+passwordInput.getText());
                NetworkManager.INSTANCE.login(
                        usernameInput.getText(),
                        passwordInput.getText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                NetworkManager.INSTANCE.self.setName(usernameInput.getText());
                                Game.switchScene(JoinScene.class);
                            }
                        },
                        new Consumer<String>() {
                            @Override
                            public void accept(String reason) {
                                errorText.text(Messages.get(LoginScene.this, reason));
                                System.out.println(reason);
                            }
                        }
                );

            }
        };

        btnLogin.setSize( 80, 20 );
        add(btnLogin);


        StyledButton btnBack = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                Game.switchScene(TitleScene.class);

            }
        };
        btnBack.icon(Icons.EXIT.get());
        btnBack.setSize( 20, 20 );
        add(btnBack);








        title.setPos((w-title.width())/2, 20) ;

        float centerX = w / 2;
        float spacing = 70;

        usernameText.setPos(centerX - spacing - usernameText.width()/2, 90);
        passwordText.setPos(centerX - spacing - passwordText.width()/2, 160);

        usernameInput.setSize(80, 25);
        passwordInput.setSize(80, 25);
        usernameInput.setPos(centerX + spacing - 40, 90-usernameInput.height()/2);
        passwordInput.setPos(centerX + spacing - 40, 160-passwordInput.height()/2);


        btnLogin.setPos((w- btnLogin.width())/2, h-30);
        btnBack.setPos(w-btnBack.width()-insets.right, insets.top);

        fadeIn();

    }



    @Override
    public void update() {
        super.update();
    }
}
