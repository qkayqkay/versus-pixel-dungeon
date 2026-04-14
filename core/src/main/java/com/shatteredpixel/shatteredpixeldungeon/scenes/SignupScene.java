package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.watabou.noosa.Camera;
import com.watabou.utils.RectF;

public class SignupScene extends PixelScene {

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
