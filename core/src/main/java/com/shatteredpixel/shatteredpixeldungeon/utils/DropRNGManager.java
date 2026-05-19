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

package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DropRNGManager {

	private static final String KEYS = "keys";
	private static final String STATES = "states";

	private static final HashMap<String, Random.LCG> rngs = new HashMap<>();
	private static long baseSeed;

	public static void reset( long seed ){
		baseSeed = seed;
		rngs.clear();
	}

	public static Random.LCG get( String key ){
		Random.LCG rng = rngs.get( key );
		if (rng == null){
			rng = new Random.LCG( seedForKey( key ) );
			rngs.put( key, rng );
		}
		return rng;
	}

	public static void storeInBundle( Bundle bundle ){
		ArrayList<String> keys = new ArrayList<>( rngs.keySet() );
		Collections.sort( keys );

		String[] keyArray = new String[keys.size()];
		long[] stateArray = new long[keys.size()];

		for (int i = 0; i < keys.size(); i++){
			String key = keys.get( i );
			keyArray[i] = key;
			stateArray[i] = rngs.get( key ).seed;
		}

		bundle.put( KEYS, keyArray );
		bundle.put( STATES, stateArray );
	}

	public static void restoreFromBundle( Bundle bundle, long seed ){
		reset( seed );

		if (bundle == null || bundle.isNull() || !bundle.contains( KEYS ) || !bundle.contains( STATES )){
			return;
		}

		String[] keys = bundle.getStringArray( KEYS );
		long[] states = bundle.getLongArray( STATES );

		if (keys == null || states == null){
			return;
		}

		int count = Math.min( keys.length, states.length );
		for (int i = 0; i < count; i++){
			Random.LCG rng = new Random.LCG( 0 );
			rng.seed = states[i];
			rngs.put( keys[i], rng );
		}
	}

	private static long seedForKey( String key ){
		long hash = 0xcbf29ce484222325L;

		hash ^= baseSeed;
		hash *= 0x100000001b3L;

		for (int i = 0; i < key.length(); i++){
			hash ^= key.charAt( i );
			hash *= 0x100000001b3L;
		}

		return mix( hash );
	}

	private static long mix( long value ){
		value ^= value >>> 32;
		value *= 0xbea225f9eb34556dL;
		value ^= value >>> 29;
		value *= 0xbea225f9eb34556dL;
		value ^= value >>> 32;
		value *= 0xbea225f9eb34556dL;
		value ^= value >>> 29;
		return value;
	}
}
