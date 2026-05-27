# 새로운 랜덤 사용법

새 랜덤 코드를 추가할 때는 먼저 "이 랜덤 결과가 무엇을 바꾸는가"를 정합니다.

자세한 정책과 기존 패턴 설명은 `docs/rng-ko.md`를 참고하세요. 이 문서는 실전에서 바로 고르는 용도입니다.

## 1. 일반 게임플레이 결과

HP, 피해량, 명중, 버프, 아이템, 맵 상태, 몹 행동처럼 게임 상태에 영향을 주면 기본 `Random.*`을 씁니다.

```java
if (Random.Float() < procChance) {
    Buff.affect( enemy, Poison.class );
}
```

기본 원칙은 간단합니다. 별도 stream이 필요한 이유가 없다면 기존 main gameplay RNG를 씁니다.

## 2. 순수 표현 효과

파티클, 소리 pitch, 애니메이션, 장식, 순수 cosmetic 대사처럼 게임 상태를 바꾸지 않으면 `Random.*Visual`을 씁니다.

```java
float angle = Random.FloatVisual( 360f );
int particles = Random.IntRangeVisual( 3, 6 );
```

Visual RNG로 전투, 드랍, 위치, 아이템 선택, 타겟 선택을 결정하면 안 됩니다.

## 3. 짧은 deterministic block

특정 seed로 짧은 구간만 재현하고 싶으면 `Random.Scope`를 씁니다.

```java
try (Random.Scope ignored = Random.useGenerator( Dungeon.seedCurDepth() + 1 )) {
    int roll = Random.Int( 4 );
    // 이 block 안의 Random.* 호출은 위 seed에서 나온 generator를 사용합니다.
}
```

`try` block이 끝나면 Java가 자동으로 `close()`를 호출하고, `Random.Scope.close()`가 generator를 pop합니다.

## 4. 몹 드랍

몹 드랍은 `Mob` subclass 안에서 `useDropRNG(...)`를 씁니다.

```java
try (Random.Scope ignored = useDropRNG( "loot" )) {
    loot = createLoot();
}
```

자주 쓰는 stream 이름:

- `check`: 드랍 발생 여부
- `loot`: 드랍 아이템 종류
- `drop_pos`: 추가 드랍 위치
- `extra_loot`: 특수 몹의 추가 아이템
- `extra_count`: 특수 몹의 추가 개수

`DropRNGManager.get(...)`을 직접 push하지 말고, `useDropRNG(...)`를 통해 key 규칙을 타세요. `Swarm`처럼 key를 override하는 몹이 있기 때문입니다.

## 5. 새 persistent/named stream

호출 사이에 RNG 상태가 이어져야 하거나 save/load 후에도 상태가 유지되어야 한다면 persistent stream입니다.

최소 형태:

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

사용할 때:

```java
try (Random.Scope ignored = Random.useGenerator( myRNG )) {
    int index = Random.Int( candidates.size() );
}
```

`new Random.LCG(seed)`는 새 stream을 시작할 때 사용합니다. Save/load 복원에서는 저장해둔 `myRNG.seed` 값을 다시 넣어야 이전에 소비한 상태가 이어집니다.

전역 manager가 여러 RNG stream을 소유한다면 manager 전용 child `Bundle`에 저장합니다.

```java
Bundle myRNG = new Bundle();
MyRNGManager.storeInBundle( myRNG );
bundle.put( MY_RNG, myRNG );
```

복원할 때:

```java
MyRNGManager.restoreFromBundle( bundle.getBundle( MY_RNG ), seed );
```

실제 파일 저장은 save system이 처리합니다. 이 child bundle은 parent bundle이 이후 파일로 저장될 때만 의미가 있습니다. save/load 이후 RNG 상태를 이어갈 필요가 없다면 bundle에 저장하지 않아도 됩니다.

새 persistent stream을 만들기 전에 반드시 정하세요.

- 새 run에서 어디서 reset/seed되는가
- save/load 때 어디서 store/restore되는가
- run reset 때 어디서 clear되는가
- key 기반이라면 key가 안정적인가

## 피해야 할 것

- 게임 로직에서 `Math.random()` 사용
- 새 shared gameplay/levelgen 코드에서 seed 없는 `Random.pushGenerator()` 사용
- `Random.*Visual`로 게임 결과 결정
- `pushGenerator(...)` 후 `popGenerator()`를 빠뜨릴 수 있는 구조
- 정책이 애매한 랜덤을 조용히 main/drop/visual 중 하나에 섞기

애매하면 먼저 결정하세요. 결과가 shared인지, local인지, cosmetic인지가 RNG 선택의 기준입니다.
