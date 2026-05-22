/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class CMD extends Item {

	public static final String AC_SUMMON = "SUMMON";

	{
		image = ItemSpriteSheet.PHANTOM_MEAT;
		defaultAction = AC_SUMMON;
		unique = true;
		bones = false;
	}

	@Override
	public String defaultAction() {
		return DeviceCompat.isDebug() ? super.defaultAction() : null;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (DeviceCompat.isDebug()) {
			actions.add( AC_SUMMON );
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_SUMMON ) && !DeviceCompat.isDebug()) {
			return;
		}

		super.execute( hero, action );

		if (action.equals( AC_SUMMON )) {
			GameScene.show( new WndTextInput(
					"SUMMON",
					"Enter a mob class name, e.g. Eye or /summon Eye.",
					"",
					1000,
					true,
					"SUMMON",
					"CANCEL" ) {
				@Override
				public void onSelect( boolean positive, String text ) {
					if (!positive) {
						return;
					}

					for (String line : text.split( "\\r?\\n" )) {
						summonFromInput( line );
					}
				}
			} );
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}

	private static void summonFromInput( String input ) {
		String mobName = input.trim();
		if (mobName.startsWith( "/summon " )) {
			mobName = mobName.substring( "/summon ".length() ).trim();
		}

		if (!mobName.isEmpty()) {
			summonMob( mobName );
		}
	}

	private static void summonMob( String mobName ) {
		try {
			Class<?> cls = Class.forName( mobClassName( mobName ) );
			if (!Mob.class.isAssignableFrom( cls )) {
				GLog.w( "%s is not a mob.", mobName );
				return;
			}

			Mob mob = (Mob)cls.getDeclaredConstructor().newInstance();
			mob.HT = mob.HP = 1;
			mob.defenseSkill = 0;

			int spawnPos = spawnPos();
			if (spawnPos == -1) {
				GLog.w( "No open adjacent cell to summon %s.", mobName );
				return;
			}

			mob.pos = spawnPos;
			if (mob.state != mob.PASSIVE) {
				mob.state = mob.WANDERING;
			}
			GameScene.add( mob );
			mob.beckon( Dungeon.hero.pos );
			Dungeon.level.occupyCell( mob );
			Dungeon.observe();
			GLog.i( "Summoned %s.", mobName );

		} catch (ClassNotFoundException e) {
			GLog.w( "No mob class found: %s.", mobName );
		} catch (Exception e) {
			GLog.w( "Failed to summon %s: %s", mobName, e.getClass().getSimpleName() );
		}
	}

	private static String mobClassName( String mobName ) {
		if (mobName.startsWith( "com.shatteredpixel.shatteredpixeldungeon." )) {
			return mobName;
		}
		return "com.shatteredpixel.shatteredpixeldungeon.actors.mobs." + mobName;
	}

	private static int spawnPos() {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int pos = Dungeon.hero.pos + offset;
			if (pos >= 0
					&& pos < Dungeon.level.length()
					&& Actor.findChar( pos ) == null
					&& Dungeon.level.passable[pos]) {
				return pos;
			}
		}
		return -1;
	}
}
