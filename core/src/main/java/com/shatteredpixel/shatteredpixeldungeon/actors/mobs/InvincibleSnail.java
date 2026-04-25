package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TormentedSpiritSprite;
import com.watabou.utils.Random;

public class InvincibleSnail extends Wraith {
    {
        spriteClass = TormentedSpiritSprite.class;
        baseSpeed = 0.5f; // incredibly slow

    }

    @Override
    public int damageRoll() {
        return Random.NormalIntRange( 1 + (level*7)/2, 2 + (level*7) );
    } // roughly 700% damage

    @Override
    protected boolean act() {
        Buff.prolong(this, Invulnerability.class, Invulnerability.DURATION);
        Buff invul = this.buff(Invulnerability.class);
        invul.fx(false);
        this.beckon(Dungeon.hero.pos);
        return super.act();
    }
}
