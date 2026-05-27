# RNG 가이드

이 프로젝트는 랜덤을 "무엇을 위해 쓰는가"에 따라 나눕니다. 모든 플레이어가 완전히 같은 런을 보는 것이 목표는 아닙니다. 공유되어야 하는 경험은 안정적으로 유지하고, 바닐라에 가까운 로컬 동작이 중요한 부분은 그대로 보존하는 것이 목표입니다.

## 핵심 규칙

- 일반 게임플레이 랜덤에는 `Random.*`를 사용합니다.
- 게임 상태에 영향을 줄 수 없는 표현용 랜덤에만 `Random.*Visual`을 사용합니다.
- main gameplay RNG를 소비하거나 흔들면 안 되는 기능은 별도 named/special RNG stream을 사용합니다.
- 새 임시 RNG scope나 named RNG scope를 만들 때는 직접 `pushGenerator` / `popGenerator`를 쓰기보다 `try (Random.Scope ignored = Random.useGenerator(...))`를 우선 사용합니다.
- 게임 로직에는 `Math.random()`을 사용하지 않습니다.

## Scoped RNG 사용법

`Random.Scope`는 Java의 try-with-resources 문법을 쓰기 위한 작은 도구입니다. `try` 블록이 끝나면 Java가 scope 객체의 `close()`를 자동으로 호출합니다. `Random.Scope.close()`는 generator를 pop하므로, 블록 중간에 `return`되거나 예외가 발생해도 RNG stack이 복구됩니다.

수동 try/finally 방식:

```java
Random.pushGenerator( Dungeon.seedCurDepth() + 1 );
try {
    int roll = Random.Int( 4 );
    // 이 deterministic block 안에서 roll을 사용합니다.
} finally {
    Random.popGenerator();
}
```

권장 방식:

```java
try (Random.Scope ignored = Random.useGenerator( Dungeon.seedCurDepth() + 1 )) {
    int roll = Random.Int( 4 );
    // 이 deterministic block 안에서 roll을 사용합니다.
}
```

두 코드는 의도상 같은 일을 합니다.

1. 현재 depth에서 파생한 임시 generator를 push합니다.
2. 블록 안의 랜덤 호출을 그 generator로 실행합니다.
3. 블록이 끝나면 RNG를 pop합니다.

scoped 방식은 pop이 `try` 블록의 생명주기와 묶이기 때문에 더 안전합니다. RNG를 중첩해서 사용할 때도 훨씬 읽기 쉽습니다.

`Random.useGenerator(seed)`와 `Random.useGenerator(lcg)`는 의도가 다릅니다.

- `Random.useGenerator(seed)`는 그 seed에서 시작하는 새 임시 generator를 매번 만듭니다. 같은 seed를 넣으면 같은 sequence에서 시작합니다.
- `Random.useGenerator(lcg)`는 이미 존재하는 generator를 push하고 현재 상태를 소비합니다. 호출 사이 또는 save/load 이후에도 상태가 이어져야 하는 persistent stream에 사용합니다.

## 기존 수동 패턴

코드베이스에는 아직 오래된 수동 RNG 패턴이 남아 있습니다. 특히 바닐라 맵 생성 쪽이 그렇습니다. 이런 코드는 Drop RNG와 같은 계열이 아니며, `pushGenerator`를 직접 쓴다는 이유만으로 버그라고 보면 안 됩니다.

### Seeded Levelgen Scope

일부 시스템은 deterministic한 큰 구간 전체에 seed를 push합니다.

- `Dungeon.init()`은 label, color, gem, room, item generator 상태를 초기화하는 동안 `seed + 1`을 push하고, 이후 generator stack을 reset한 뒤 base gameplay RNG를 seed합니다.
- `Dungeon.seedForDepth(...)`는 run seed를 push하고 정해진 횟수만큼 `Random.Long()`을 넘긴 뒤 depth seed를 반환하고 pop합니다.
- `Level.create()`는 `Dungeon.seedCurDepth()`를 push한 상태로 level feeling, map build, mob creation, item creation을 처리합니다.

대표적인 형태:

```java
Random.pushGenerator( Dungeon.seedCurDepth() );

// Level feeling, map layout, mobs, items가 모두 이 depth seed를 사용합니다.
build();
createMobs();
createItems();

Random.popGenerator();
```

이것이 오래된 바닐라식 seeded scope 모델입니다. 각 depth의 생성 과정을 안정적으로 재현하기 위한 구조입니다.

### Derived Substream

일부 levelgen 코드는 `Random.Long()`으로 임시 하위 stream을 만듭니다.

`RegularLevel.createItems()`는 darkness torch, rose petal, cached rations, guide page, lore page, ebony mimic, spyglass loot처럼 optional하거나 플레이어/meta 상태에 의존하는 요소에 이 패턴을 사용합니다. 부모 levelgen stream은 고정된 위치에서 long 하나만 소비하고, optional block은 자기 임시 generator 안에서 실행됩니다.

대표적인 형태:

```java
Random.pushGenerator( Random.Long() );

// Optional하거나 player-dependent한 작업은 여기에서 처리합니다.
// 바깥 levelgen stream은 고정된 Long() 하나만 소비합니다.
int roll = Random.Int( candidates.size() );

Random.popGenerator();
```

이렇게 하면 held item, talent, document progress 같은 상태가 이후 levelgen RNG 소비량을 바꾸지 않습니다.

### No-Seed Isolation Block

기존 코드 일부는 seed 없이 `Random.pushGenerator()`를 호출합니다. 이 경우 현재 시간을 seed로 하는 임시 generator가 만들어집니다.

대표적인 형태:

```java
Random.pushGenerator();

// 로컬 격리 목적입니다. deterministic하지 않습니다.
int roll = Random.Int( candidates.size() );

Random.popGenerator();
```

이 패턴은 early guide-page placement나 generator restore setup 같은 몇몇 legacy isolation 경로에 남아 있습니다. 새 shared gameplay 또는 levelgen 로직에는 사용하지 마세요. 새 결과를 재현해야 한다면 명시적인 seed나 named/persistent stream을 사용합니다.

### Persistent Special Stream

다른 RNG들은 호출이나 save/load를 넘어 상태가 유지되는 persistent `LCG` stream입니다. bones RNG, spawn RNG, Drop RNG가 여기에 가깝습니다.

Drop RNG는 DropRNG API와 scoped 사용을 우선합니다. Spawn RNG는 local FOV와 현재 map state에 의존하므로 의도적으로 로컬이고 바닐라스럽게 유지합니다.

대표적인 Drop RNG 형태:

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    loot = createLoot();
}
```

대표적인 persistent generator 형태:

```java
Random.pushGenerator( spawnRNG );
Mob mob = createMob();
mob.pos = randomRespawnCell( mob );
Random.popGenerator();
```

이 예시는 현재 존재하는 persistent generator 형태를 보여주는 것입니다. 새 persistent stream을 만들 때는 `Random.useGenerator(lcg)`와 `Random.Scope`를 우선 사용하고, 어렵다면 최소한 수동 `try/finally`로 감싸서 블록이 중간에 끝나도 generator가 반드시 pop되게 합니다.

새 persistent 또는 named stream에는 명시적인 lifecycle도 필요합니다.

- 새 run에서 reset/seed되는 위치
- save/load에서 state를 store/restore하는 위치
- run reset 때 clear되는 위치
- key 기반이라면 안정적인 key 규칙

object가 직접 소유하는 stream의 최소 형태:

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

사용할 때는 `Random.useGenerator(myRNG)`로 감쌉니다.

```java
try (Random.Scope ignored = Random.useGenerator( myRNG )) {
    int index = Random.Int( candidates.size() );
}
```

`new Random.LCG(seed)`는 새 stream을 시작할 때 사용합니다. Save/load 복원에서는 저장해둔 `lcg.seed` 값을 직접 넣어야 이미 소비한 상태에서 이어집니다. 전역 또는 key 기반 manager라면 같은 reset/store/restore lifecycle을 manager 안에 모읍니다.

manager가 소유하는 stream은 manager 전용 child `Bundle`을 통해 저장합니다.

```java
Bundle myRNG = new Bundle();
MyRNGManager.storeInBundle( myRNG );
bundle.put( MY_RNG, myRNG );
```

복원할 때는 그 child bundle을 manager에 넘깁니다.

```java
MyRNGManager.restoreFromBundle( bundle.getBundle( MY_RNG ), seed );
```

실제 파일 저장은 save system의 책임입니다. 이 child bundle은 parent bundle이 이후 save system에 의해 파일로 저장될 때만 의미가 있습니다. save/load 이후 RNG 상태를 이어갈 필요가 없다면 bundle에 저장하지 않아도 됩니다. RNG manager는 persistence가 필요할 때만 자기 상태를 bundle에 저장하고 bundle에서 복원하는 방법을 정의하면 됩니다.

기존 바닐라 levelgen block을 문법 통일만을 위해 전부 고칠 필요는 없습니다. 새 scoped randomness를 추가하거나 위험한 수동 push/pop 코드를 만질 때 scoped API를 우선 사용합니다.

## RNG 스트림

### Main Gameplay RNG

`Random.Float()`, `Random.Int(...)`, `Random.chances(...)` 등은 바닐라 게임플레이 로직과 일반적인 게임플레이 결정에 사용합니다.

예시:

- 명중 판정
- 피해량 판정
- proc 확률
- 함정/탐색 판정
- 일반 게임 흐름에 포함되는 아이템 생성

랜덤 결과가 HP, buff, map state, inventory, monster behavior, 또는 플레이어가 볼 수 있는 게임플레이 정보에 영향을 준다면 visual RNG가 아닙니다.

### Visual RNG

`Random.*Visual`은 표현용 랜덤에만 사용합니다.

허용 예시:

- 사운드 pitch 변화
- 파티클 위치, 색, 개수, 속도, 수명
- 애니메이션 변화
- 순수 cosmetic 대사 선택
- UI 배경 장식

Visual RNG는 게임플레이 결과를 결정하면 안 됩니다. 전투, 드랍, 맵 발견, 아이템 선택, 이동, actor timing, target selection에 영향을 줄 수 있다면 visual RNG를 쓰면 안 됩니다.

### Drop RNG

몹 드랍은 `DropRNGManager`를 사용합니다. 목적은 드랍 결과를 main gameplay RNG와 독립적으로 안정화하는 것입니다.

`Mob` subclass 안에서는 다음 형태를 사용합니다.

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    loot = createLoot();
}
```

몹 드랍 코드에서 `DropRNGManager.get(...)`과 `Random.pushGenerator(...)`를 직접 호출하지 마세요.

Drop RNG key는 `Mob.dropRNGKey()`를 기반으로 합니다. 특수 몹이 이 key를 override할 수 있기 때문에 중요합니다. 예를 들어 `Swarm`은 generation을 key에 포함해서, 분열된 swarm들이 완전히 같은 drop stream을 공유하지 않게 합니다.

현재 자주 쓰는 stream:

- `check`: 일반 몹 드랍이 발생하는지
- `loot`: 일반 몹 드랍 아이템이 무엇인지
- `drop_pos`: 추가 드랍 위치, `Level.drop(...)` 내부 reroll 포함
- `extra_loot`: 특수 몹의 추가 아이템 생성
- `extra_count`: 특수 몹의 추가 드랍 개수

### Spawn RNG

Spawn RNG는 의도적으로 로컬이며 바닐라스럽게 유지합니다.

몹 생성은 로컬 FOV, passability, 점유된 칸, 플레이어의 현재 상태에 의존합니다. 두 플레이어의 FOV나 spawn 위치가 같을 것으로 기대하지 않습니다. 클라이언트 결과를 맞추기 위해 spawn placement를 억지로 deterministic하게 만들지 않습니다.

`spawnRNG`의 목적은 로컬 spawn randomness를 main gameplay RNG에서 분리하는 것이지, 모든 플레이어의 spawn 결과를 공유시키는 것이 아닙니다.

### Bones RNG

Bones/remains 배치는 별도 RNG를 사용합니다. save data, meta progress, 플레이어별 상태가 main gameplay RNG를 흔들지 않게 하기 위해서입니다.

Bones/remains 배치를 다룰 때는 기존 bones generator를 사용하세요. bones 배치를 visual RNG에 섞으면 안 됩니다.

### Seeded One-Off RNG

새로 짧은 deterministic 블록을 만들 때는 scoped 사용을 권장합니다.

```java
try (Random.Scope ignored = Random.useGenerator( seed )) {
    if (reqSecrets <= 0 && Random.Int( 4 ) < hintChance) {
        GLog.p( Messages.get( this, "secret_hint" ) );
    }
}
```

이 방식은 임시 generator를 push하고 `finally`에서 pop하는 것과 같은 의미지만, RNG stack을 실수로 망가뜨리기 어렵습니다.

## 정책 보류 영역

아래 영역은 정리하기 전에 명시적인 설계가 필요합니다.

- Rift RNG
- subclass/armor ability/talent 랜덤 버튼
- attack indicator target choice
- Ring of Wealth, Lucky enchantment, Soul Eater bonus reward stream

결과가 shared인지, local인지, 순수 client-side인지 결정하지 않고 조용히 다른 stream으로 옮기지 마세요.

## 새 랜덤 추가 체크리스트

랜덤 호출을 추가하기 전에 답하세요.

1. 결과가 game state 또는 플레이어 결정에 영향을 주는가?
2. 모든 플레이어가 같은 결과를 얻어야 하는가?
3. 이 랜덤 호출이 main RNG 소비량을 바꿀 수 있는가?
4. save/load 이후에도 독립적으로 상태가 유지되어야 하는가?
5. 순수 visual/audio/dialogue flavor인가?
6. persistent 또는 named stream을 쓴다면 어디에서 reset, store, restore되는가?

답에 따라 stream을 고릅니다.

- gameplay outcome: named stream이 필요한 경우가 아니면 main RNG
- cosmetic only: visual RNG
- mob drop stability: drop RNG
- local vanilla spawn behavior: spawn RNG
- bones/remains placement: bones RNG
- multiplayer policy가 애매함: 먼저 문서화하고 그 다음 구현
