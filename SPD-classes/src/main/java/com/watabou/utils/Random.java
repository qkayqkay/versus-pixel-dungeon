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

package com.watabou.utils;

import com.watabou.noosa.Game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Random {

	private static final long multiplier = 0x5DEECE66DL;
	private static final long addend = 0xBL;
	private static final long mask = (1L << 48) - 1;

	// Custom LCG implementation that exposes its seed and matches java.util.Random behavior
	public static class LCG extends java.util.Random {
		public long seed;

		public LCG(long seed) {
			// Random constructor calls setSeed
			setSeed(seed);
		}

		@Override
		public synchronized void setSeed(long seed) {
			this.seed = (seed ^ multiplier) & mask;
		}

		@Override
		protected synchronized int next(int bits) {
			seed = (seed * multiplier + addend) & mask;
			return (int) (seed >>> (48 - bits));
		}
	}

	//we store a stack of random number generators, which may be seeded deliberately or randomly.
	//top of the stack is what is currently being used to generate new numbers.
	//the base generator is always created with no seed, and cannot be popped.
	private static ArrayDeque<LCG> generators;
	
	//visualGenerator is used for purely aesthetic effects that shouldn't affect gameplay determinism
	private static LCG visualGenerator = new LCG(System.currentTimeMillis());

	private static long gameplayCount = 0;
	private static long visualCount = 0;

	static {
		resetGenerators();
	}

	public static synchronized void resetGenerators(){
		generators = new ArrayDeque<>();
		generators.push(new LCG(System.currentTimeMillis()));
		gameplayCount = 0;
		visualCount = 0;
	}

	public static synchronized void seedBaseGenerator(long seed){
		generators.peekLast().setSeed(seed);
		gameplayCount = 0;
	}

	public static synchronized void pushGenerator(){
		generators.push( new LCG(System.currentTimeMillis()) );
	}

	public static synchronized void pushGenerator( long seed ){
		generators.push( new LCG( scrambleSeed(seed) ) );
	}

	//scrambles a given seed, this helps eliminate patterns between the outputs of similar seeds
	//Algorithm used is MX3 by Jon Maiga (jonkagstrom.com), CC0 license.
	private static synchronized long scrambleSeed( long seed ){
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		return seed;
	}

	public static synchronized void popGenerator(){
		if (generators.size() == 1){
			Game.reportException( new RuntimeException("tried to pop the last random number generator!"));
		} else {
			generators.pop();
		}
	}

	private static void logRNG(boolean gameplay, String method, Object result) {
		String caller = "unknown";
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stack.length; i++) {
			String className = stack[i].getClassName();
			if (!className.contains("com.watabou.utils.Random") && !className.contains("java.lang.Thread")) {
				caller = stack[i].getFileName() + ":" + stack[i].getLineNumber();
				break;
			}
		}
		if (gameplay) {
			System.out.println(String.format("[RNG_GAME #%d] %s -> %s (%s)", ++gameplayCount, method, result.toString(), caller));
		}
	}

	// --- Visual Generator Methods ---

	public static synchronized float FloatVisual() {
		float res = visualGenerator.nextFloat();
		return res;
	}

	public static float FloatVisual( float max ) {
		return FloatVisual() * max;
	}

	public static float FloatVisual( float min, float max ) {
		return min + FloatVisual(max - min);
	}

	public static synchronized int IntVisual( int max ) {
		if (max <= 0) return 0;
		int res = visualGenerator.nextInt(max);
		return res;
	}

	public static int IntVisual( int min, int max ) {
		return min + IntVisual(max - min);
	}

	public static int IntRangeVisual( int min, int max ) {
		return min + IntVisual(max - min + 1);
	}
	
	public static int NormalIntRangeVisual( int min, int max ) {
		return min + (int)((FloatVisual() + FloatVisual()) * (max - min + 1) / 2f);
	}

	public static int chancesVisual( float[] chances ) {
		int length = chances.length;
		float sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
		}
		if (sum <= 0){
			return -1;
		}
		float value = FloatVisual( sum );
		sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
			if (value < sum) {
				return i;
			}
		}
		return -1;
	}

	public static float NormalFloatVisual( float min, float max ) {
		return min + ((FloatVisual(max - min) + FloatVisual(max - min))/2f);
	}

	public static<T> T elementVisual( T[] array ) {
		return array[IntVisual(array.length)];
	}
	
	public static<T> T elementVisual( Collection<? extends T> collection ) {
		int size = collection.size();
		return size > 0 ? (T)collection.toArray()[IntVisual( size )] : null;
	}

	@SafeVarargs
	public static<T> T oneOfVisual(T... array ) {
		return array[IntVisual(array.length)];
	}

	public static int indexVisual( Collection<?> collection ) {
		return IntVisual(collection.size());
	}

	public synchronized static<T> void shuffleVisual( List<?extends T> list){
		Collections.shuffle(list, visualGenerator);
	}

	// --- Gameplay Generator Methods ---

	//returns a uniformly distributed float in the range [0, 1)
	public static synchronized float Float() {
		return Float(true);
	}

	public static synchronized float Float( boolean useGeneratorStack ) {
		float res;
		if (useGeneratorStack)  res = generators.peekFirst().nextFloat();
		else                    res = generators.peekLast().nextFloat();
		
		logRNG(useGeneratorStack, "Float", res);
		return res;
	}

	//returns a uniformly distributed float in the range [0, max)
	public static float Float( float max ) {
		return Float() * max;
	}

	//returns a uniformly distributed float in the range [min, max)
	public static float Float( float min, float max ) {
		return min + Float(max - min);
	}
	
	//returns a triangularly distributed float in the range [min, max)
	public static float NormalFloat( float min, float max ) {
		return min + ((Float(max - min) + Float(max - min))/2f);
	}

	//returns a uniformly distributed int in the range [-2^31, 2^31)
	public static synchronized int Int() {
		return Int(true);
	}

	//returns a uniformly distributed int in the range [-2^31, 2^31)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized int Int( boolean useGeneratorStack ) {
		if (useGeneratorStack)  return generators.peekFirst().nextInt();
		else                    return generators.peekLast().nextInt();
	}

	//returns a uniformly distributed int in the range [0, max)
	public static synchronized int Int( int max ) {
		return Int(max, true);
	}

	//returns a uniformly distributed int in the range [0, max)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized int Int( int max, boolean useGeneratorStack ) {
		int res;
		if (max <= 0)                   res = 0;
		else if (useGeneratorStack)     res = generators.peekFirst().nextInt(max);
		else                            res = generators.peekLast().nextInt(max);

		logRNG(useGeneratorStack, "Int(" + max + ")", res);
		return res;
	}

	//returns a uniformly distributed int in the range [min, max)
	public static int Int( int min, int max ) {
		return min + Int(max - min);
	}

	//returns a uniformly distributed int in the range [min, max]
	public static int IntRange( int min, int max ) {
		return min + Int(max - min + 1);
	}

	//returns a triangularly distributed int in the range [min, max]
	//this makes results more likely as they get closer to the middle of the range
	public static int NormalIntRange( int min, int max ) {
		return min + (int)((Float() + Float()) * (max - min + 1) / 2f);
	}

	//returns an inverse triangularly distributed int in the range [min, max]
	//this makes results more likely as they get further from the middle of the range
	public static int InvNormalIntRange( int min, int max ){
		float roll1 = Float(), roll2 = Float();
		if (Math.abs(roll1-0.5f) >= Math.abs(roll2-0.5f)){
			return min + (int)(roll1*(max - min + 1));
		} else {
			return min + (int)(roll2*(max - min + 1));
		}
	}

	//returns a uniformly distributed long in the range [-2^63, 2^63)
	public static synchronized long Long() {
		return Long(true);
	}

	//returns a uniformly distributed long in the range [-2^63, 2^63)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized long Long( boolean useGeneratorStack ) {
		if (useGeneratorStack)  return generators.peekFirst().nextLong();
		else                    return generators.peekLast().nextLong();
	}

	//returns a mostly uniformly distributed long in the range [0, max)
	public static long Long( long max ) {
		long result = Long();
		if (result < 0) result += Long.MAX_VALUE;
		//modulo isn't perfect, but as long as max is reasonably below 2^63 it's close enough
		return result % max;
	}

	//returns an index from chances, the probability of each index is the weight values in changes
	//negative values are treated as 0
	public static int chances( float[] chances ) {
		
		int length = chances.length;
		
		float sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
		}

		if (sum <= 0){
			return -1;
		}
		
		float value = Float( sum );
		sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
			if (value < sum) {
				return i;
			}
		}
		
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	//returns a key element from chances, the probability of each key is the weight value it maps to
	public static <K> K chances( HashMap<K,Float> chances ) {
		
		int size = chances.size();

		Object[] values = chances.keySet().toArray();
		float[] probs = new float[size];
		float sum = 0;
		for (int i=0; i < size; i++) {
			probs[i] = chances.get( values[i] );
			sum += probs[i];
		}
		
		if (sum <= 0) {
			return null;
		}
		
		float value = Float( sum );
		
		sum = 0;
		for (int i=0; i < size; i++) {
			sum += probs[i];
			if (value < sum) {
				return (K)values[i];
			}
		}
		
		return null;
	}
	
	public static int index( Collection<?> collection ) {
		return Int(collection.size());
	}

	@SafeVarargs
	public static<T> T oneOf(T... array ) {
		return array[Int(array.length)];
	}
	
	public static<T> T element( T[] array ) {
		return element( array, array.length );
	}
	
	public static<T> T element( T[] array, int max ) {
		return array[Int(max)];
	}
	
	@SuppressWarnings("unchecked")
	public static<T> T element( Collection<? extends T> collection ) {
		int size = collection.size();
		return size > 0 ?
			(T)collection.toArray()[Int( size )] :
			null;
	}

	public synchronized static<T> void shuffle( List<?extends T> list){
		Collections.shuffle(list, generators.peek());
	}

	public static void shuffle( int[] array ) {
		for (int i=0; i < array.length - 1; i++) {
			int j = Int( i, array.length );
			if (j != i) {
				int t = array[i];
				array[i] = array[j];
				array[j] = t;
			}
		}
	}

	public static<T> void shuffle( T[] array ) {
		for (int i=0; i < array.length - 1; i++) {
			int j = Int( i, array.length );
			if (j != i) {
				T t = array[i];
				array[i] = array[j];
				array[j] = t;
			}
		}
	}
	
	public static<U,V> void shuffle( U[] u, V[]v ) {
		for (int i=0; i < u.length - 1; i++) {
			int j = Int( i, u.length );
			if (j != i) {
				U ut = u[i];
				u[i] = u[j];
				u[j] = ut;
				
				V vt = v[i];
				v[i] = v[j];
				v[j] = vt;
			}
		}
	}

	// --- State Save/Restore ---

	public static synchronized long[] getStackStates() {
		long[] states = new long[generators.size()];
		int i = 0;
		for (LCG lcg : generators) {
			states[i++] = lcg.seed;
		}
		return states;
	}

	public static synchronized void restoreStackStates(long[] states) {
		generators.clear();
		for (int i = states.length - 1; i >= 0; i--) {
			LCG lcg = new LCG(0);
			lcg.seed = states[i]; // Direct seed injection
			generators.push(lcg);
		}
	}

	public static synchronized long getVisualState() {
		return visualGenerator.seed;
	}

	public static synchronized void restoreVisualState(long state) {
		visualGenerator.seed = state;
	}
}
