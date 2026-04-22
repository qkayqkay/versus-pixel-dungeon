package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.RectF;

public class LossScene extends PixelScene {
    private static final int WIDTH      = 160;
    private static final int BTN_HEIGHT = 20;
    private static final float SMALL_GAP = 2;
    private static final float LARGE_GAP = 8;

    { inGameScene = true; }

    @Override
    public void create() {
        super.create();

        RectF insets = getCommonInsets();
        int w = (int)(Camera.main.width  - insets.left - insets.right);
        int h = (int)(Camera.main.height - insets.top  - insets.bottom);

        // title
        RenderedTextBlock title = renderTextBlock(Messages.get(this, "title"), 18);
        title.maxWidth(WIDTH);
        title.align(RenderedTextBlock.CENTER_ALIGN);
        add(title);

        // description
        RenderedTextBlock text = renderTextBlock(Messages.get(this, "text"), 12);
        text.maxWidth(PixelScene.landscape() ? 2 * WIDTH - 4 : WIDTH);
        text.align(RenderedTextBlock.CENTER_ALIGN);
        add(text);

        // final time
        float finalTime = NetworkManager.INSTANCE.finalTime;
        int hours   = (int)(finalTime / 3600);
        int minutes = (int)((finalTime % 3600) / 60);
        int seconds = (int)(finalTime % 60);
        String timeStr = String.format("%02dh:%02dm:%02ds", hours, minutes, seconds);

        RenderedTextBlock timeDisplay = renderTextBlock("Final time: " + timeStr, 9);
        timeDisplay.setPos(5, 5);
        timeDisplay.camera = uiCamera;
        add(timeDisplay);

        // button
        StyledButton returnBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Return to Lobby") {
            @Override
            protected void onClick() {
                Game.switchScene(InLobbyScene.class);
            }
        };
        add(returnBtn);

        // layout - stack title, text, button in the vertical center
        float totalHeight = title.height() + SMALL_GAP + text.height() + LARGE_GAP + BTN_HEIGHT;
        float startY = insets.top + (h - totalHeight) / 2f;

        title.setPos(insets.left + (w - title.width()) / 2f, startY);
        align(title);

        text.setPos(insets.left + (w - text.width()) / 2f, startY + title.height() + SMALL_GAP);
        align(text);

        returnBtn.setRect(
                insets.left + (w - WIDTH) / 2f,
                startY + title.height() + SMALL_GAP + text.height() + LARGE_GAP,
                WIDTH, BTN_HEIGHT
        );

        fadeIn();
    }
}