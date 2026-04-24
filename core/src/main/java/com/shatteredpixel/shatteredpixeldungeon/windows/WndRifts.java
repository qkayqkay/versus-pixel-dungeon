package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.RiftTier;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;

import java.awt.*;
import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

public class WndRifts extends Window {

    protected static final int WIDTH  = 120;
    public static final int BTN_SIZE  = 20;

    public WndRifts(Hero hero, boolean info) {


        IconTitle title;
        if (!info) {
            title = new IconTitle(Icons.get(Icons.TALENT),
                    Messages.titleCase("Multiplayer Rifts"));
        } else {
            title = new IconTitle(Icons.INFO.get(),
                    Messages.titleCase("Rift Info"));
        }
        title.setRect(0, 0, WIDTH, 0);
        add(title);

        // toggle between cast mode and info mode
        IconButton btnInfo = new IconButton(info ? Icons.get(Icons.TALENT) : Icons.INFO.get()) {
            @Override
            protected void onClick() {
                GameScene.show(new WndRifts(hero, !info));
                hide();
            }
        };
        btnInfo.setRect(WIDTH - 16, 0, 16, 16);
        add(btnInfo);

        RenderedTextBlock msg;
        if (info) {
            msg = PixelScene.renderTextBlock("Tap a rift to read its description.", 6);
        } else {
            msg = PixelScene.renderTextBlock("Select a rift to invoke.", 6);
        }
        msg.maxWidth(WIDTH);
        msg.setPos(0, title.bottom() + 4);
        add(msg);

        int top = (int) msg.bottom() + 4;

        for (int i = 1; i <= Talent.MAX_TALENT_TIERS; i++) {
            ArrayList<Rift> rifts = Rift.getRiftList(hero, i);

            if (!rifts.isEmpty() && i != 1) {
                top += BTN_SIZE + 2;
                ColorBlock sep = new ColorBlock(WIDTH, 1, 0xFF000000);
                sep.y = top;
                add(sep);
                top += 3;
            }
            RiftTier tierObj = new RiftTier(i);
            ArrayList<RiftButton> riftBtns = new ArrayList<>();
            boolean unlockedTier = tierObj.isUnlocked(hero);

            for (Rift rift : rifts) {
                RiftButton riftBtn = new RiftButton(rift, info, unlockedTier);
                add(riftBtn);
                riftBtns.add(riftBtn);
                if(!unlockedTier){
                    riftBtn.enable(false);
                }

        }

            int left = 2 + (WIDTH - riftBtns.size() * (BTN_SIZE + 4)) / 2;
            for (RiftButton btn : riftBtns) {
                btn.setRect(left, top, BTN_SIZE, BTN_SIZE);
                left += btn.width() + 4;
            }


            if (!riftBtns.isEmpty()) {
                tierObj.buildOverlay(this, hero, top, WIDTH, BTN_SIZE);
            }

        }

        resize(WIDTH, top + BTN_SIZE);

        if (SPDSettings.interfaceSize() != 2) {
            offset(0, (int) (GameScene.uiCamera.height / 2 - 30 - height / 2));
        }
    }

    public class RiftButton extends IconButton {

        Rift rift;
        boolean info;
        NinePatch bg;
        ColorBlock btnOverlay;
        boolean tierUnlocked;

        public RiftButton(Rift rift, boolean info, boolean tierUnlocked) {
            super(new HeroIcon(rift));
            this.rift = rift;
            this.info = info;
            this.tierUnlocked = tierUnlocked;

            bg = Chrome.get(Chrome.Type.TOAST);
            addToBack(bg);
        }


        @Override
        protected void layout() {
            super.layout();
            if (bg != null) {
                bg.size(width, height);
                bg.x = x;
                bg.y = y;
            }
            if (rift == null) return;

            if (btnOverlay != null) {
                remove(btnOverlay);
            }

            if (tierUnlocked) {
                float proportion = 1f - rift.castProportion(hero);
                System.out.println(proportion);
                float overlayHeight = height * proportion;
                btnOverlay = new ColorBlock(width, overlayHeight, 0x88000000);
                btnOverlay.x = x;
                btnOverlay.y = y + (height - overlayHeight);
                addToFront(btnOverlay);

                RenderedTextBlock proportionTxt = PixelScene.renderTextBlock(hero.belongings.riftStone.getCharge()+"/"+rift.getCost(), 6);
                proportionTxt.maxWidth((int)width);
                proportionTxt.setPos(x+(width-proportionTxt.width())/2, y+(height-proportionTxt.height())/2);
                addToFront(proportionTxt);
            }

        }

        @Override
        protected void onClick() {
            if (info) {
                GameScene.show(new WndTitledMessage(
                        new HeroIcon(rift),
                        Messages.titleCase(rift.name()),
                        rift.desc()
                ));
            } else {
                hide();

                if (!rift.canCast(hero)) {
                    GLog.w("Can't use this rift right now.");
                    return;
                }
                rift.onCast(hero);
                GLog.i("Invoked rift: " + rift.name());
            }
        }

        @Override
        protected String hoverText() {
            return "_" + Messages.titleCase(rift.name()) + "_\n" + rift.shortDesc();
        }
    }
}