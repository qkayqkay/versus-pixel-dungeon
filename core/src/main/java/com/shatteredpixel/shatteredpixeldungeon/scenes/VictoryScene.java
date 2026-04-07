package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.effects.BadgeBanner;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

public class VictoryScene extends PixelScene {
    private static final int WIDTH      = 160;
    private static final int BTN_HEIGHT = 20;
    private static final float SMALL_GAP = 2;
    private static final float LARGE_GAP = 8;

    private Image amulet;
    private float scale = 2f;
    private float amuletY;
    private float targetY;
    private Flare flare;
    private float speed = 0.7f;

    private RenderedTextBlock title;
    private RenderedTextBlock text;
    private StyledButton returnBtn;

    private float timer = 0;
    private float startTimer = 2f;
    private boolean moving = false;
    private boolean doneMoving = false;
    private float textAlpha = 0f;
    private float btnAlpha = 0f;
    private boolean textDone = false;

    { inGameScene = true; }

    @Override
    public void create() {
        super.create();

        RectF insets = getCommonInsets();
        int w = (int)(Camera.main.width  - insets.left - insets.right);
        int h = (int)(Camera.main.height - insets.top  - insets.bottom);

        // amulet
        amulet = new Image(Assets.Sprites.AMULET);
        targetY = amulet.height + insets.top;
        amulet.scale.set(scale);
        add(amulet);

        // title
        title = renderTextBlock(Messages.get(this, "title"), 18);
        title.maxWidth(WIDTH);
        title.align(RenderedTextBlock.CENTER_ALIGN);
        title.alpha(0);
        add(title);

        // description
        text = renderTextBlock(Messages.get(this, "text"), 12);
        text.maxWidth(PixelScene.landscape() ? 2 * WIDTH - 4 : WIDTH);
        text.align(RenderedTextBlock.CENTER_ALIGN);
        text.alpha(0);
        add(text);

        // button
        returnBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Return to Lobby") {
            @Override
            protected void onClick() {
                Game.switchScene(InLobbyScene.class);
            }
        };
        returnBtn.alpha(0);
        add(returnBtn);

        // layout - position everything relative to amulet center
        float totalHeight = amulet.height * scale
                + LARGE_GAP + title.height()
                + SMALL_GAP + text.height()
                + LARGE_GAP + BTN_HEIGHT;

        float topY = (h - (amulet.height*scale)) / 2f;

        amulet.x = (w - amulet.width * scale) / 2f;
        amuletY   = topY;
        amulet.y  = amuletY;
        align(amulet);

        float contentY = amuletY + amulet.height + LARGE_GAP;

        title.setPos(insets.left + (w - title.width()) / 2f, contentY);
        align(title);

        contentY += title.height() + SMALL_GAP;

        text.setPos(insets.left + (w - text.width()) / 2f, contentY);
        align(text);

        contentY += text.height() + LARGE_GAP;

        returnBtn.setRect(insets.left + (w - WIDTH) / 2f, contentY, WIDTH, BTN_HEIGHT);



        flare = new Flare(8, 48).color(0xFFDDBB, true);
        flare.scale.set(scale);
        flare.angularSpeed = +30;
        flare.show(amulet, 0);

        fadeIn();
    }

    @Override
    public void update() {
        super.update();

        if (startTimer > 0) {
            startTimer -= Game.elapsed;
            if (startTimer <= 0) {
                moving = true;
            }
        }

        if (moving) {
            scale    += (1f - scale)          * Game.elapsed * speed;
            amuletY  += (targetY - amuletY)   * Game.elapsed * speed;

            amulet.x = (Camera.main.width - amulet.width * scale) / 2f;
            amulet.y = amuletY;
            amulet.scale.set(scale);
            flare.scale.set(scale);
            flare.point(amulet.center());

            if (Math.abs(targetY - amuletY) < 2f) {
                amuletY = targetY;
                moving = false;
                doneMoving = true;
            }
        }

        if (doneMoving && !textDone) {
            textAlpha += (1f - textAlpha) * Game.elapsed * speed;
            title.alpha(textAlpha);
            text.alpha(textAlpha);

            if (textAlpha > 0.95f) {
                title.alpha(1f);
                text.alpha(1f);
                textDone = true;
            }
        }

        if (textDone) {
            btnAlpha += (1f - btnAlpha) * Game.elapsed * speed;
            returnBtn.alpha(btnAlpha);
            if (btnAlpha > 0.95f) {
                returnBtn.alpha(1f);
            }
        }

        if ((timer -= Game.elapsed) < 0) {
            timer = Random.Float(0.5f, 5f);
            Speck star = (Speck) recycle(Speck.class);
            star.reset(0, amulet.x + 10.5f, amulet.y + 5.5f, Speck.DISCOVER);
            add(star);
        }
    }
}