# RNG Guide

This project separates random number generation by intent. The goal is not to make every player see the exact same run. The goal is to keep shared experiences stable where needed, while preserving vanilla-like local behavior where that matters.

## Core Rules

- Use `Random.*` for normal gameplay randomness.
- Use `Random.*Visual` only for randomness that cannot affect game state.
- Use a named/special RNG stream when a feature must not consume or perturb the main gameplay RNG.
- For new temporary or named RNG scopes, prefer `try (Random.Scope ignored = Random.useGenerator(...))` over manual `pushGenerator` / `popGenerator`.
- Do not use `Math.random()` for game logic.

## Scoped RNG Usage

`Random.Scope` is a small helper for Java's try-with-resources syntax. When the `try` block ends, Java automatically calls `close()` on the scope object. `Random.Scope.close()` pops the generator, so the RNG stack is restored even if the block returns early or throws an exception.

Manual try/finally style:

```java
Random.pushGenerator( Dungeon.seedCurDepth() + 1 );
try {
    int roll = Random.Int( 4 );
    // Use roll for this deterministic block.
} finally {
    Random.popGenerator();
}
```

Preferred style:

```java
try (Random.Scope ignored = Random.useGenerator( Dungeon.seedCurDepth() + 1 )) {
    int roll = Random.Int( 4 );
    // Use roll for this deterministic block.
}
```

The two snippets are meant to do the same thing:

1. Push a temporary generator seeded from the current depth.
2. Run the random calls inside the block using that generator.
3. Pop the RNG when the block ends.

The scoped version is safer because the pop is tied to the lifetime of the `try` block. It also makes nested RNG usage easier to read.

`Random.useGenerator(seed)` and `Random.useGenerator(lcg)` are intentionally different:

- `Random.useGenerator(seed)` creates a new temporary generator from that seed every time. The same seed starts the same sequence.
- `Random.useGenerator(lcg)` pushes an existing generator and consumes its current state. Use this for persistent streams whose state must advance across calls or save/load.

## Existing Manual Patterns

The codebase still contains older manual RNG patterns, especially in vanilla level generation. These are not the same thing as Drop RNG, and they should not be treated as bugs just because they use `pushGenerator` directly.

### Seeded Levelgen Scopes

Some systems push a seed for a whole deterministic section:

- `Dungeon.init()` pushes `seed + 1` while initializing labels, colors, gems, rooms, and item generator state, then resets the generator stack before seeding the base gameplay RNG.
- `Dungeon.seedForDepth(...)` pushes the run seed, advances through a fixed number of `Random.Long()` calls, returns the depth seed, then pops.
- `Level.create()` pushes `Dungeon.seedCurDepth()` around level feeling selection, map build, mob creation, and item creation.

Representative shape:

```java
Random.pushGenerator( Dungeon.seedCurDepth() );

// Level feeling, map layout, mobs, and items all use this depth seed.
build();
createMobs();
createItems();

Random.popGenerator();
```

This is the old vanilla-style seeded scope model. It gives each depth a stable deterministic generation pass.

### Derived Substreams

Some levelgen code creates temporary substreams with `Random.Long()`.

`RegularLevel.createItems()` uses this pattern for optional or player/meta-dependent content such as darkness torches, rose petals, cached rations, guide pages, lore pages, ebony mimics, and spyglass loot. The parent levelgen stream consumes one long at a fixed point, and the optional block runs inside its own temporary generator.

Representative shape:

```java
Random.pushGenerator( Random.Long() );

// Optional or player-dependent work happens here.
// The outer levelgen stream only consumed one fixed Long().
int roll = Random.Int( candidates.size() );

Random.popGenerator();
```

This prevents held items, talents, document progress, and similar state from changing later levelgen RNG consumption.

### No-Seed Isolation Blocks

Some existing code calls `Random.pushGenerator()` with no seed. That creates a temporary generator from the current time.

Representative shape:

```java
Random.pushGenerator();

// Local isolation only. This is not deterministic.
int roll = Random.Int( candidates.size() );

Random.popGenerator();
```

This pattern exists in a few legacy isolation paths, such as early guide-page placement and generator restore setup. Do not use it for new shared gameplay or levelgen logic. If a new result needs to be reproducible, use an explicit seed or a named/persistent stream.

### Persistent Special Streams

Other RNGs are persistent `LCG` streams whose state can survive across calls or saves. Examples include bones RNG, spawn RNG, and Drop RNG.

Drop RNG should use the DropRNG API and scoped usage. Spawn RNG is intentionally local and vanilla-like, because monster spawning depends on local FOV and current map state.

Representative Drop RNG shape:

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    loot = createLoot();
}
```

Representative persistent generator shape:

```java
Random.pushGenerator( spawnRNG );
Mob mob = createMob();
mob.pos = randomRespawnCell( mob );
Random.popGenerator();
```

This shows an existing persistent generator shape. For new persistent streams, prefer `Random.Scope` with `Random.useGenerator(lcg)` or at least a manual `try/finally`, so the generator is always popped if the block exits early.

Any new persistent or named stream also needs an explicit lifecycle:

- reset/seed it for a new run
- store and restore its state through save/load
- clear it when the run is reset
- keep its keys stable if it is key-based

Minimal object-owned stream shape:

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

Use the stream with `Random.useGenerator(myRNG)`:

```java
try (Random.Scope ignored = Random.useGenerator( myRNG )) {
    int index = Random.Int( candidates.size() );
}
```

`new Random.LCG(seed)` starts a new stream. Save/load restoration should restore the saved `lcg.seed` value directly so the stream continues from the already-consumed state. For global or key-based managers, centralize the same reset/store/restore lifecycle in the manager.

Manager-owned streams should be saved through a child `Bundle` owned by the manager:

```java
Bundle myRNG = new Bundle();
MyRNGManager.storeInBundle( myRNG );
bundle.put( MY_RNG, myRNG );
```

Restore it from that child bundle:

```java
MyRNGManager.restoreFromBundle( bundle.getBundle( MY_RNG ), seed );
```

The actual file write belongs to the save system. The child bundle only matters if the parent bundle is later written by the save system. If the RNG state does not need to continue after save/load, do not store it in a bundle. The RNG manager only needs to define how its state is stored into and restored from a bundle when persistence is required.

Do not rewrite existing vanilla levelgen blocks only to make their syntax uniform. When adding new scoped randomness or touching risky manual push/pop code, prefer the scoped API.

## RNG Streams

### Main Gameplay RNG

Use `Random.Float()`, `Random.Int(...)`, `Random.chances(...)`, etc. for vanilla gameplay logic and ordinary gameplay decisions.

Examples:

- attack rolls
- damage rolls
- proc chances
- trap/search checks
- item generation that is part of normal gameplay flow

If a random result changes HP, buffs, map state, inventory, monster behavior, or player-visible gameplay information, it is not visual RNG.

### Visual RNG

Use `Random.*Visual` only for presentation.

Allowed examples:

- sound pitch variation
- particle position, color, count, speed, or lifetime
- animation variation
- purely cosmetic dialogue line selection
- UI background decoration

Visual RNG must not decide gameplay outcomes. If a value can change combat, drops, map discovery, item choice, movement, actor timing, or target selection, do not use visual RNG.

### Drop RNG

Mob drops use `DropRNGManager` so that drop outcomes can be kept stable independently from the main gameplay RNG.

Inside `Mob` subclasses, use:

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    ...
}
```

Do not call `DropRNGManager.get(...)` and `Random.pushGenerator(...)` directly from mob drop code.

Drop RNG keys are based on `Mob.dropRNGKey()`. This is important because special mobs can override the key. For example, `Swarm` includes its generation in the key so split swarms do not all share exactly the same drop stream.

Current common streams:

- `check`: whether a normal mob drop happens
- `loot`: what the normal mob drop is
- `drop_pos`: random placement for extra drops, including `Level.drop(...)` rerolls
- `extra_loot`: extra special-mob item generation
- `extra_count`: extra special-mob drop count

### Spawn RNG

Spawn RNG is intentionally local and vanilla-like.

Monster spawning depends on local FOV, passability, occupied cells, and the player's current state. Two players are not expected to have identical FOV or identical spawn positions. Do not force spawn placement to be deterministic just to make clients match.

The purpose of `spawnRNG` is to isolate local spawn randomness from the main gameplay RNG, not to make spawn results shared across all players.

### Bones RNG

Bones/remains placement uses a separate RNG so that save data, meta progress, and player-specific state do not perturb main gameplay RNG.

Use the existing bones generator when working on remains/bones placement. Do not mix bones placement into visual RNG.

### Seeded One-Off RNG

For a new short deterministic block, prefer scoped usage:

```java
try (Random.Scope ignored = Random.useGenerator( seed )) {
    if (reqSecrets <= 0 && Random.Int( 4 ) < hintChance) {
        GLog.p( Messages.get( this, "secret_hint" ) );
    }
}
```

This is equivalent to pushing a temporary generator and popping it in a `finally` block, but it is harder to accidentally leave the RNG stack corrupted.

## Deferred / Policy-Pending Areas

These areas need explicit design before being normalized:

- Rift RNG
- random subclass/armor ability/talent buttons
- attack indicator target choice
- Ring of Wealth, Lucky enchantment, and Soul Eater bonus reward streams

Do not silently move these into another stream without deciding whether the result should be shared, local, or purely client-side.

## Checklist for New Randomness

Before adding a random call, answer:

1. Can the result affect game state or player decisions?
2. Should all players get the same result?
3. Can this random call change how much main RNG is consumed?
4. Does it need to survive save/load independently?
5. Is it only visual/audio/dialogue flavor?
6. If it uses a persistent or named stream, where is it reset, stored, and restored?

Use the answers to choose the stream:

- gameplay outcome: main RNG unless a named stream is required
- cosmetic only: visual RNG
- mob drop stability: drop RNG
- local vanilla spawn behavior: spawn RNG
- bones/remains placement: bones RNG
- unclear multiplayer policy: document first, then implement
