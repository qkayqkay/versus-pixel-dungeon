// Dropdown.java
package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

public class Dropdown extends Component {

    private ArrayList<StyledButton> buttons = new ArrayList<>();

    public Dropdown() {
        // Empty — caller adds buttons, then calls layout()
    }

    public void layout(float x, float y, float w, ArrayList<StyledButton> btns) {
        float btnH = 16f;
        float gap = 2f;
        float currentY = y;

        for (StyledButton btn : btns) {
            btn.setRect(x, currentY, w, btnH);
            add(btn);
            buttons.add(btn);
            currentY += btnH + gap;
        }

        float totalH = currentY - y;
        int margin = 2;
        ColorBlock bg = new ColorBlock(w + 2 * margin, totalH + 2 * margin, 0xB3000000);
        bg.x = x - margin;
        bg.y = y - margin;
        addToBack(bg);

        setSize(w, totalH);
    }

    public void close() {
        for (StyledButton btn : buttons) {
            btn.killAndErase();
        }
        buttons.clear();
        killAndErase();
    }
}