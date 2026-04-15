package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.RectF;

public class AccountOptionsScene extends PixelScene{


 // nothing here yet.

    RectF insets;
    int w;
    int h;

    float gap = 15f;


    @Override
    public void create() {
        super.create();
        insets = getCommonInsets();
        w = (int) (Camera.main.width - insets.left + insets.right);
        h = (int) (Camera.main.height - insets.top + insets.bottom);

        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(AccountOptionsScene.class, "title"), 12);
        title.setSize(30, 15);
        title.setPos((w - title.width()) / 2, 10 + insets.top);
        add(title);

        StyledButton loginBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "login")){
            @Override
            protected void onClick() {
                super.onClick();
                Game.switchScene(LoginScene.class);
            }
        };
        loginBtn.setSize(90, 20);
        float y = (h*0.3f)+(loginBtn.height())/2;
        loginBtn.setPos((w-loginBtn.width())/2, y);
        add(loginBtn);


        StyledButton signupBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "register")){
            @Override
            protected void onClick() {
                super.onClick();
                Game.switchScene(SignupScene.class);
            }
        };
        signupBtn.setSize(90, 20);
        y += (loginBtn.height()/2)+gap;
        signupBtn.setPos((w-signupBtn.width())/2, y);
        add(signupBtn);


        StyledButton guestBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "continue_guest")){
            @Override
            protected void onClick() {
                super.onClick();
                //NetworkManager.INSTANCE.self.
            }
        };
        guestBtn.setSize(90, 20);
        y += (signupBtn.height()/2)+gap;
        guestBtn.setPos((w-guestBtn.width())/2, y);
        //add(guestBtn); shhhh im working on it

    }



    @Override
    public void update() {
        super.update();
    }
}