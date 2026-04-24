package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRifts;
import com.watabou.noosa.ColorBlock;

public class RiftTier {

    private int tier;

    public RiftTier(int tier) {
        this.tier = tier;
    }

    public boolean isUnlocked(Hero hero) {
        return hero.belongings.riftStone.isTierUnlocked(tier);
    }

    public String lockedText() {
        return Messages.get(WndRifts.class, "locked_tier"); // same for all tiers so using this
    }

    public String unlockConditionText() {
        return Messages.get(this, "unlock_condition"+this.tier);
    }

    public void buildOverlay(Window wnd, Hero hero, int top, int width, int btnSize) {

        if (isUnlocked(hero)) return;

        int marginY = 3;
        int overlayHeight = btnSize + marginY * 2;

        ColorBlock overlay = new ColorBlock(width, overlayHeight, 0x88000000);
        overlay.y = top - marginY;

        wnd.add(overlay);

        RenderedTextBlock locked = PixelScene.renderTextBlock(lockedText(), 6);
        locked.maxWidth(width);
        float centerHeight = (overlay.height()-locked.height())/2;
        float distance = 5f;
        locked.setPos((width - locked.width())/2, overlay.y + centerHeight+distance);

        wnd.add(locked);

        RenderedTextBlock condition = PixelScene.renderTextBlock(unlockConditionText(), 6);
        condition.maxWidth(width);
        condition.setPos((width - condition.width())/2, overlay.y+centerHeight-distance);

        wnd.add(condition);
    }
}