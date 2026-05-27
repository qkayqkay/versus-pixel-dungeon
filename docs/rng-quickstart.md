# RNG Quickstart

When adding new random code, first decide what the random result changes.

For detailed policy and existing code patterns, see `docs/rng.md`. This document is a practical picker for common cases.

## 1. Normal Gameplay Results

Use normal `Random.*` calls when the result can affect game state, such as HP, damage, hit chance, buffs, items, map state, or monster behavior.

```java
if (Random.Float() < procChance) {
    Buff.affect( enemy, Poison.class );
}
```

Default rule: use the main gameplay RNG unless there is a clear reason to use a separate stream.

## 2. Pure Presentation

Use `Random.*Visual` for particles, sound pitch, animation variation, decoration, or purely cosmetic dialogue.

```java
float angle = Random.FloatVisual( 360f );
int particles = Random.IntRangeVisual( 3, 6 );
```

Visual RNG must not decide combat, drops, positions, item choices, or target selection.

## 3. Short Deterministic Block

Use `Random.Scope` when a short block should be reproducible from a specific seed.

```java
try (Random.Scope ignored = Random.useGenerator( Dungeon.seedCurDepth() + 1 )) {
    int roll = Random.Int( 4 );
    // Random.* calls inside this block use the generator created from that seed.
}
```

When the `try` block ends, Java automatically calls `close()`, and `Random.Scope.close()` pops the generator.

## 4. Mob Drops

Inside `Mob` subclasses, use `useDropRNG(...)` for mob drops.

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    loot = createLoot();
}
```

Common stream names:

- `check`: whether the drop happens
- `loot`: which item drops
- `drop_pos`: extra drop position
- `extra_loot`: special-mob extra item
- `extra_count`: special-mob extra count

Do not push `DropRNGManager.get(...)` directly. Use `useDropRNG(...)` so mob-specific key rules are applied. This matters for mobs such as `Swarm`, which override the drop RNG key.

## 5. New Persistent/Named Stream

Use a persistent stream when RNG state must continue across calls or after save/load.

Minimal object-owned shape:

```java
private static final String MY_RNG = "my_rng";

private Random.LCG myRNG;

private void initMyRNG() {
    myRNG = new Random.LCG( Dungeon.seedCurDepth() + 12345 );
}

@Override
public void storeInBundle( Bundle bundle ) {
    super.storeInBundle( bundle );
    if (myRNG != null) {
        bundle.put( MY_RNG, myRNG.seed );
    }
}

@Override
public void restoreFromBundle( Bundle bundle ) {
    super.restoreFromBundle( bundle );
    if (bundle.contains( MY_RNG )) {
        myRNG = new Random.LCG( 0 );
        myRNG.seed = bundle.getLong( MY_RNG );
    } else {
        initMyRNG();
    }
}
```

Use it like this:

```java
try (Random.Scope ignored = Random.useGenerator( myRNG )) {
    int index = Random.Int( candidates.size() );
}
```

`new Random.LCG(seed)` starts a new stream. On save/load restore, assign the saved `myRNG.seed` value directly so the already-consumed state continues.

If a global manager owns multiple RNG streams, save them through a manager-owned child `Bundle`.

```java
Bundle myRNG = new Bundle();
MyRNGManager.storeInBundle( myRNG );
bundle.put( MY_RNG, myRNG );
```

Restore it like this:

```java
MyRNGManager.restoreFromBundle( bundle.getBundle( MY_RNG ), seed );
```

The save system handles the actual file write. This child bundle only matters if the parent bundle is later written to a file. If the RNG state does not need to continue after save/load, do not store it in a bundle.

Before creating a persistent stream, decide:

- where it is reset/seeded for a new run
- where it is stored/restored during save/load
- where it is cleared when the run resets
- whether its key is stable, if it is key-based

## Avoid

- `Math.random()` in game logic
- seedless `Random.pushGenerator()` in new shared gameplay or levelgen code
- using `Random.*Visual` to decide gameplay results
- `pushGenerator(...)` structures where `popGenerator()` can be skipped
- silently putting unclear randomness into main/drop/visual RNG without deciding its multiplayer policy

If it is unclear, decide first whether the result is shared, local, or cosmetic. That decision determines the RNG stream.
