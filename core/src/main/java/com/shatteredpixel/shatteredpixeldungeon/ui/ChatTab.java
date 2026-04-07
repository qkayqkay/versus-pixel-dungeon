package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.TextInput;

public class ChatTab extends TextInput {

    public ChatTab(int txtSize, float zoom) {
        super(Chrome.get(Chrome.Type.TOAST_WHITE), false, txtSize, zoom);
        bg.alpha(0.5f);
        //bg.hardlight(0x000000);
    }



    @Override
    public void onKeyTyped(char c) {
        if (c == '\r' || c == '\n') {
            enterPressed();

        }
    }

    @Override
    public void enterPressed() {
        String msg = getText();
        if (!msg.isEmpty()) {
            NetworkManager.INSTANCE.sendMessage(msg);
            clearText();
        }
        GameScene.chatOpen = false;
        GameScene.destroyChatTab();
    }
}