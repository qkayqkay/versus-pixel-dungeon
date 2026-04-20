package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.utils.Align;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextInput;
import com.watabou.utils.RectF;

import java.util.function.Consumer;

public class SignupScene extends PixelScene {

    RectF insets;
    int w;
    int h;
    StyledButton btnSignup;

    @Override
    protected void onBackPressed() {}

    @Override
    public void create() {
        super.create();
        insets = getCommonInsets();
        w = (int) (Camera.main.width - insets.left + insets.right);
        h = (int) (Camera.main.height - insets.top + insets.bottom);

        TitleBackground BG = new TitleBackground( Camera.main.width, Camera.main.height);
        add( BG );

        RenderedTextBlock title = renderTextBlock(Messages.get(this, "title"), 12);
        add(title);

        RenderedTextBlock usernameText = renderTextBlock(Messages.get(this, "username"), 8);
        add(usernameText);

        RenderedTextBlock passwordText = renderTextBlock(Messages.get(this, "password"), 8);
        add(passwordText);

        RenderedTextBlock confirmPasswordText = renderTextBlock(Messages.get(this, "confirm_password"), 8);
        add(confirmPasswordText);

        RenderedTextBlock errorText = renderTextBlock("", 8);
        errorText.hardlight(0xFF4444); // red colour for errors
        add(errorText);


        TextInput passwordInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8, uiCamera.zoom);
        passwordInput.setPasswordMode(true);
        add(passwordInput);

        TextInput confirmPasswordInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8, uiCamera.zoom);
        confirmPasswordInput.setPasswordMode(true);
        add(confirmPasswordInput);

        TextInput usernameInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, 8, uiCamera.zoom);
        add(usernameInput); // adding username last so thats the one with focus first

        btnSignup = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "signup")) {
            @Override
            protected void onClick() {
                if (!passwordInput.getText().equals(confirmPasswordInput.getText())) {
                    errorText.text(Messages.get(SignupScene.class, "passwords_dont_match"));
                    errorText.setPos((w - errorText.width()) / 2, 185);
                    return;
                }
                btnSignup.enable(false);
                NetworkManager.INSTANCE.register(
                        usernameInput.getText(),
                        passwordInput.getText(),
                        new Runnable() {
                            @Override
                            public void run() {
                                NetworkManager.INSTANCE.self.setName(usernameInput.getText());
                                NetworkManager.INSTANCE.self.setGuestStatus(false);
                                Game.switchScene(JoinScene.class);
                            }
                        },
                        new Consumer<String>() {
                            @Override
                            public void accept(String reason) {
                                btnSignup.enable(true);
                                errorText.text(Messages.get(SignupScene.this, reason)); // reason eg "usertaken"
                                errorText.setPos((w - errorText.width()) / 2, 185);
                                System.out.println("Reason is: "+reason);
                            }
                        }
                );
            }
        };
        btnSignup.setSize(80, 20);
        add(btnSignup);

        StyledButton btnBack = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
            @Override
            protected void onClick() {
                ShatteredPixelDungeon.switchNoFade(AccountOptionsScene.class);
            }
        };
        btnBack.icon(Icons.EXIT.get());
        btnBack.setSize(20, 20);
        add(btnBack);

        // layout
        title.setPos((w - title.width()) / 2, 20);

        float centerX = w / 2;
        float spacing = 70;

        usernameText.setPos(centerX - spacing - usernameText.width() / 2, 70);
        passwordText.setPos(centerX - spacing - passwordText.width() / 2, 110);
        confirmPasswordText.setPos(centerX - spacing - confirmPasswordText.width() / 2, 150);

        usernameInput.setSize(80, 25);
        passwordInput.setSize(80, 25);
        confirmPasswordInput.setSize(80, 25);

        usernameInput.setPos(centerX + spacing - 40, 70 - usernameInput.height() / 2);
        passwordInput.setPos(centerX + spacing - 40, 110 - passwordInput.height() / 2);
        confirmPasswordInput.setPos(centerX + spacing - 40, 150 - confirmPasswordInput.height() / 2);

        errorText.setPos(w/2, 185);

        btnSignup.setPos((w - btnSignup.width()) / 2, h - 30);
        btnBack.setPos(w - btnBack.width() - insets.right, insets.top);

        fadeIn();
    }

    @Override
    public void update() {
        super.update();
    }
}