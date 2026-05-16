// GamemodeDropdown.java
package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.networking.Gamemode;

import java.util.ArrayList;

public class GamemodeDropdown extends Dropdown {

    public interface OnGamemodeSelected {
        void onSelected(Gamemode g);
    }

    public GamemodeDropdown(Gamemode[] gamemodes, OnGamemodeSelected callback) {
        super();
        ArrayList<StyledButton> btns = buildButtons(gamemodes, callback);
        // Store them so layout() can be called by the caller with these
        this.pendingButtons = btns;
    }

    // Held here so the caller can pass them to layout()
    public ArrayList<StyledButton> pendingButtons;

    private ArrayList<StyledButton> buildButtons(Gamemode[] gamemodes, OnGamemodeSelected callback) {
        ArrayList<StyledButton> btns = new ArrayList<>();
        for (Gamemode g : gamemodes) {
            Gamemode captured = g;
            StyledButton btn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, g.gamemodeName) {
                @Override
                protected void onClick() {
                    callback.onSelected(captured);
                    close();
                }
            };
            btns.add(btn);
        }
        return btns;
    }
}