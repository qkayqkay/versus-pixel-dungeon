package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Image;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
import static java.lang.Math.floor;
import static java.lang.Math.round;


public enum BingoCondition {
    UPGRADE_CLOTH("cloth_upgrade", Icons.CLOSE) {
        @Override
        public boolean check(Hero hero, float random) {
            ClothArmor cloth = hero.belongings.getItem(ClothArmor.class);
            if (cloth == null) return false;
            int count = (int) floor(random*2)+1;
            if (cloth.level() >= count) {
                return true;
            }
            else{
                return false;
            }
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "cloth_upgrade", (int) floor(random*2)+1);
        }
    },

    REACH_FLOOR("reach_floor", Icons.STAIRS) {
        @Override
        public boolean check(Hero hero, float random) {
            int targetDepth = (int) floor(random*6) + 4;
            return Dungeon.depth >= targetDepth;
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "reach_floor", (int) floor(random*6) + 4);
        }
    },

    KILL_PIRANHA("kill_piranha", Icons.CLOSE) {
        @Override
        public boolean check(Hero hero, float random) {
            int required = (int) floor(random * 3) + 1;
            return Statistics.piranhasKilled >= required;
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "kill_piranha", ((int) floor(random * 3) + 1));
        }
    },

    KILL_TRAP("kill_trap", Icons.CLOSE) {
        @Override
        public boolean check(Hero hero, float random) {
            int required = (int) floor(random * 4) + 2;
            return Statistics.hazardAssistedKills >= required;
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "kill_trap", ((int) floor(random * 4) + 2));
        }
    },

    KILL_ENEMIES("kill_enemies", Icons.CLOSE) {
        @Override
        public boolean check(Hero hero, float random) {
            int required = (int) floor(random * 15) + 6;
            return Statistics.hazardAssistedKills >= required;
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "kill_enemies", ((int) floor(random * 15) + 6));
        }
    },

    SHOPKEEPER_FLED("shopkeeper_fled", Icons.CLOSE) {
        @Override
        public boolean check(Hero hero, float random) {
            return Statistics.shopkeepersFled >= 1;
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "shopkeeper_fled");
        }
    },

    CHASMS_FALLEN("chasms_fallen", Icons.DEPTH_CHASM) {
        @Override
        public boolean check(Hero hero, float random) {
            return Statistics.chasmsFallen >= (int) floor(random*2+1);
        }

        @Override
        public String getLabel(float random) {
            return Messages.get(BingoCondition.class, "chasms_fallen", (int) floor(random*2+1));
        }
    },

    CRAFT_EXOTIC_POTION("craft_exotic_potion", Icons.POTION_BANDOLIER) {
        Class<? extends Potion>[] potions = (Class<? extends Potion>[]) Generator.Category.POTION.classes;

        @Override
        public boolean check(Hero hero, float random) {
            int index = (int) floor(potions.length*random);
            Class<? extends Potion> basePotion = potions[index];
            Class<? extends ExoticPotion> targetPotion = ExoticPotion.regToExo.get(basePotion);
            if (targetPotion == null){
                System.out.println("no exotic pot wtf");
                return false;
            }
            return hero.belongings.getItem(targetPotion) != null;
        }

        @Override
        public String getLabel(float random) {
            String potionName = Messages.get(ExoticPotion.regToExo.get(potions[(int) floor(potions.length*random)]), "name");
            return Messages.get(BingoCondition.class, "craft_exotic_potion", potionName);
        }
    },

    CRAFT_EXOTIC_SCROLL("craft_exotic_scroll", Icons.SCROLL_HOLDER) {
        Class<? extends Scroll>[] scrolls = (Class<? extends Scroll>[]) Generator.Category.SCROLL.classes;

        @Override
        public boolean check(Hero hero, float random) {
            int index = (int) floor(scrolls.length*random);
            Class<? extends Scroll> baseScroll = scrolls[index];
            Class<? extends ExoticScroll> targetScroll = ExoticScroll.regToExo.get(baseScroll);
            if (targetScroll == null){
                System.out.println("no exotic pot wtf");
                return false;
            }
            return hero.belongings.getItem(targetScroll) != null;
        }

        @Override
        public String getLabel(float random) {
            String scrollName = Messages.get(ExoticScroll.regToExo.get(scrolls[(int) floor(scrolls.length*random)]), "name");
            return Messages.get(BingoCondition.class, "craft_exotic_scroll", scrollName);
        }
    },

    RANDOM_POTION("random_potion", Icons.POTION_BANDOLIER) {
        Class<? extends Potion>[] potions = (Class<? extends Potion>[]) Generator.Category.POTION.classes;

        @Override
        public boolean check(Hero hero, float random) {
            int index = (int) floor(potions.length*random);
            if(Dungeon.hero.lastPotionDrunk != null && Dungeon.hero.lastPotionDrunk.equals(potions[index])){
                return true;
            }
            else{
                return false;
            }
        }

        @Override
        public String getLabel(float random) {
            String potionName = Messages.get(potions[(int) floor(potions.length*random)], "name");
            return Messages.get(BingoCondition.class, "random_potion", potionName);
        }
    },
    RANDOM_SCROLL("random_scroll", Icons.SCROLL_HOLDER) {
        Class<? extends Scroll>[] scrolls = (Class<? extends Scroll>[]) Generator.Category.SCROLL.classes;

        @Override
        public boolean check(Hero hero, float random) {
            int index = (int) floor(scrolls.length*random);
            if(Dungeon.hero.lastScrollRead != null && Dungeon.hero.lastScrollRead.equals(scrolls[index])){
                return true;
            }
            else{
                return false;
            }
        }

        @Override
        public String getLabel(float random) {
            String scrollName = Messages.get(scrolls[(int) floor(scrolls.length*random)], "name");
            return Messages.get(BingoCondition.class, "random_scroll", scrollName);
        }
    };

    public final String id;
    public final Icons icon;

    BingoCondition(String id, Icons icon) {
        this.id = id;
        this.icon = icon;
    }

    public abstract boolean check(Hero hero, float random);

    public abstract String getLabel(float random);

    public static BingoCondition[] getConditions(){
        return BingoCondition.values();
    }
}