package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts.Rift;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRifts;

import java.util.ArrayList;

public class RiftStone extends Artifact {

    {
        // TODO: replace with your actual sprite constant once added to ItemSpriteSheet
        image = ItemSpriteSheet.ARTIFACT_TOME;

        charge    = 10000000;
        chargeCap = 10000000; // TODO reset these to actual values later
        partialCharge = 0;

        exp      = 0;
        levelCap = 0;

        defaultAction = AC_CAST;

        unique = true; // self explanatory ig
        bones  = false; // never drops on death as a bone item
    }

    public static final String AC_CAST = "CAST";

    // actions

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_CAST); // read equipableitem for context. This item is always equipped
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_CAST)) {
            GameScene.show(new WndRifts(hero, false));
        }
    }


    // Charge/points

    /**
      Called when the hero kills an enemy. Awards points toward the charge cap.
      Plug this into your kill-tracking buff (see StoneKillTracker below).
     */
    public void onEnemyKilled() { // modify to take in account enemy type maybe?
        if (charge < chargeCap) {
            charge++;
            if (charge > chargeCap) charge = chargeCap;
            updateQuickslot();
        }
    }

    /**
     * Deducts points when a rift is used.
     */
    public void spendCharge(float chargesSpent) {
        partialCharge -= chargesSpent;
        while (partialCharge < 0) {
            charge--;
            partialCharge++;
        }
        if (charge < 0) charge = 0;
        updateQuickslot();
    }

    /**
     * Whether the hero can currently cast the given rift.
     */
    public boolean canCast(Hero hero, Rift rift) {
        return charge >= rift.chargeUse(hero)
                && rift.canCast(hero);
    }


    // Block all external recharging

    /**
     * Overridden to do nothing — RiftStone is never charged by external sources
     * (rings, talents, scrolls, etc.). Points only come from killing enemies.
     */
    @Override
    public void charge(Hero target, float amount) {}


    // Block equip / unequip — stone is permanent
    /**
     * Equipping is handled at game-start by giving the hero the stone directly.
     * Returning false here prevents the normal equip flow from firing if somehow
     * triggered through the UI (e.g. tapping Equip from the bag).
     */
    @Override
    public boolean doEquip(Hero hero) {
        // Stone is already permanently worn — do nothing
        return false;
    }

    /**
     * Permanently prevents unequipping.
     */
    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        GLog.n(Messages.get(this, "cant_unequip"));
        return false;
    }



    @Override
    public Item random() { // the stone is never cursed.
        return this;
    }

    // -------------------------------------------------------------------------
    // Passive buff — just keeps the stone "active" on the hero
    // -------------------------------------------------------------------------

    @Override
    protected ArtifactBuff passiveBuff() {
        return new StoneKillTracker();
    }

    /**
     * Passive buff that lives on the hero for the whole run.
     * Its job is to be the hook point for kill events.
     *
     * Wire this up by calling:
     *   Buff.affect(hero, RiftStone.StoneKillTracker.class)
     * or simply let activate() handle it when the stone is given to the hero.
     *
     * Then, wherever enemy death is processed (e.g. Char.die() or your mob
     * death callback), check:
     *   RiftStone stone = hero.belongings.getItem(RiftStone.class);
     *   if (stone != null) stone.onEnemyKilled();
     */
    public class StoneKillTracker extends ArtifactBuff { // TODO

        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    @Override
    public boolean keptThroughLostInventory() {
        return true;
    }

    @Override
    public String status() {
        // Always show current points / cap, regardless of curse state
        // (stone can never actually be cursed, but just in case)
        return Messages.format("%d/%d", charge, chargeCap);
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    // -------------------------------------------------------------------------
    // isEquipped — stone lives in a dedicated slot you'll add to HeroBelongings
    // -------------------------------------------------------------------------

    @Override
    public boolean isEquipped(Hero hero) {
        // TODO: once you add hero.belongings.riftStone, change this to:
        return hero != null && hero.belongings.riftStone == this;
    }
}