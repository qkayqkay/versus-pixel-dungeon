package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.networking.Gamemode;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;


public class Dropdown extends Component {

    private Gamemode[] gamemodes;
    private OnGamemodeSelected callback;
    private ArrayList<StyledButton> buttons = new ArrayList<>();



    public interface OnGamemodeSelected {
        void onSelected(Gamemode g);
    }

    public Dropdown(Gamemode[] gamemodes, OnGamemodeSelected callback) {
        this.gamemodes = gamemodes;
        this.callback = callback;
    }

    public void layout(float x, float y, float w) {
        float btnH = 16f;
        float gap = 2f;
        float currentY = y;


        for (Gamemode g : gamemodes) {
            Gamemode captured = g; //capture for the click handler
            StyledButton btn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, g.gamemodeName) {
                Dropdown dropdown;
                @Override
                protected void onClick() {
                    callback.onSelected(captured); // run the calback thingie
                    close();
                }
            };

            btn.setRect(x, currentY, w, btnH);
            add(btn);
            buttons.add(btn);
            currentY += btnH + gap;
        }
        float totalH = currentY - y;
        int margin = 2;
        ColorBlock bg = new ColorBlock(w+2*margin, totalH+2*margin, 0xB3000000);
        bg.x = x-margin;
        bg.y = y-margin;
        addToBack(bg); // add behind the buttons

        setSize(w, totalH);
    }

    public void close() {
        for (StyledButton btn : buttons) {
            btn.enable(false); // for some reason, they dont FUCKING DISAPPEAR WHYY. Does this cause a memory leak? Too bad!
            btn.killAndErase();
        }
        buttons.clear();
        killAndErase();
    }

}