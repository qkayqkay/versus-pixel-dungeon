package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

public class Dropdown extends Component {

    private static final ArrayList<Dropdown> openDropdowns = new ArrayList<>();

    private ArrayList<StyledButton> buttons = new ArrayList<>();
    private Runnable onClosed = null;


    public Dropdown() {
    }

    public void layout(float x, float y, float w, ArrayList<StyledButton> btns) {
        // Close all other open dropdowns first
        for (int i = openDropdowns.size() - 1; i >= 0; i--) {
            Dropdown other = openDropdowns.get(i);
            if (other != this) {
                other.close();
            }
        }
        openDropdowns.add(this);

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

    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    public void close() {
        openDropdowns.remove(this);
        for (StyledButton btn : buttons) {
            btn.enable(false);
            btn.killAndErase();
        }
        buttons.clear();
        if (onClosed != null) {
            onClosed.run();
        }
        killAndErase();
    }

}