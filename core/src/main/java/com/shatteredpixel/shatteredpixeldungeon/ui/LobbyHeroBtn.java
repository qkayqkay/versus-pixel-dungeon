package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.networking.Player;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;

public class LobbyHeroBtn extends StyledButton {

    private Player p;

    public static final int HEIGHT = 24;

    public LobbyHeroBtn(Player p) {
        super(Chrome.Type.GREY_BUTTON_TR, p.getName());
        this.p = p;

        if (p.cl != null) {
            icon(new Image(p.cl.spritesheet(), 0, 90, 12, 15));
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        System.out.println(p.getName());
    }
}