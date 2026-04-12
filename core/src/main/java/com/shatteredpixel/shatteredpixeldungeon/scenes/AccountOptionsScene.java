package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.Lobby;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AccountOptionsScene extends PixelScene{


 // nothing here yet.

    RectF insets;
    int w;
    int h;


    @Override
    public void create() {
        super.create();

        insets = getCommonInsets();
        w = (int) (Camera.main.width - insets.left + insets.right);
        h = (int) (Camera.main.height - insets.top + insets.bottom);

    }



    @Override
    public void update() {
        super.update();
    }
}