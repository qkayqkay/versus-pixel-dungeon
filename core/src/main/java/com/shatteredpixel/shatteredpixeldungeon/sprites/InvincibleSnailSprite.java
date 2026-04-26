package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

public class InvincibleSnailSprite extends MobSprite {
    public InvincibleSnailSprite() {
        super();

        texture( Assets.Sprites.WRAITH );

        TextureFilm frames = new TextureFilm( texture, 14, 15 );

        int c = 18;

        idle = new MovieClip.Animation( 5, true );
        idle.frames( frames, c+0, c+1 );

        run = new MovieClip.Animation( 10, true );
        run.frames( frames, c+0, c+1 );

        attack = new MovieClip.Animation( 10, false );
        attack.frames( frames, c+0, c+2, c+3 );

        die = new MovieClip.Animation( 8, false );
        die.frames( frames, c+0, c+4, c+5, c+6, c+7 );

        play( idle );
    }

    @Override
    public int blood() {
        return 0x88BB0000;
    }
}