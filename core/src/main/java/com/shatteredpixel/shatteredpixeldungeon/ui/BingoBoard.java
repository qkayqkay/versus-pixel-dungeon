package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.networking.BingoTask;
import com.shatteredpixel.shatteredpixeldungeon.networking.Gamemode;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import javax.swing.*;

public class BingoBoard extends Component {

    BingoTask[][] bingoTasks;
    NinePatch bg;

    static final int BASE_CELL_SIZE = 16;
    static final int BAR_THICKNESS  = 2;
    static final int MARGIN         = 6;

    int scale = 2;

    BingoBtn[][] buttons;

    public BingoBoard(BingoTask[][] conditions) {
        this.bingoTasks = conditions;
    }

    @Override
    protected void layout() {
        super.layout();

        int cols       = bingoTasks.length;
        int rows       = bingoTasks[0].length;
        int cellSize   = BASE_CELL_SIZE * scale;

        // total panel size
        int totalW = MARGIN + cols * cellSize + (cols - 1) * BAR_THICKNESS + MARGIN;
        int totalH = MARGIN + rows * cellSize + (rows - 1) * BAR_THICKNESS + MARGIN;

        if (bg != null) bg.killAndErase();
        bg = Chrome.get(Chrome.Type.WINDOW);
        bg.x = x;
        bg.y = y;
        bg.size(totalW, totalH);
        addToBack(bg);

        // vertical bars between pairs
        for (int i = 0; i < cols - 1; i++) {
            float barX = x + MARGIN + (i + 1) * cellSize + i * BAR_THICKNESS;
            float barY = y + MARGIN;
            ColorBlock vBar = new ColorBlock(BAR_THICKNESS, totalH - MARGIN * 2, 0xFF2B2B2B);
            vBar.x = barX;
            vBar.y = barY;
            add(vBar);
        }

        // horizontal bars between pairs
        for (int i = 0; i < rows - 1; i++) {
            float barX = x + MARGIN;
            float barY = y + MARGIN + (i + 1) * cellSize + i * BAR_THICKNESS;
            ColorBlock hBar = new ColorBlock(totalW - MARGIN * 2, BAR_THICKNESS, 0xFF2B2B2B);
            hBar.x = barX;
            hBar.y = barY;
            add(hBar);
        }

        //bingo buttons
        buttons = new BingoBtn[cols][rows];
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                float cellX = x + MARGIN + col * (cellSize + BAR_THICKNESS);
                float cellY = y + MARGIN + row * (cellSize + BAR_THICKNESS);

                BingoBtn btn = new BingoBtn();
                btn.setCondition(bingoTasks[row][col]);
                btn.setRect(cellX, cellY, cellSize, cellSize);
                buttons[row][col] = btn;
                add(btn);
            }
        }

        width  = totalW;
        height = totalH;
    }

    public void updateButtons(){
        for(int i = 0; i < buttons.length; i++){
            for(int j = 0; j < buttons[0].length; j++){
                buttons[i][j].updateButtonStatus();
            }
        }
    }

    public class BingoBtn extends IconButton {
        private BingoTask task;
        boolean completed = false;
        boolean hasOverlay = false;

        public void setCondition(BingoTask t) {
            this.task = t;
            if(t!=null) {
                this.icon(t.createIcon());
            }
        }

        @Override
        protected String hoverText() {
            return task.getLabel();
        }


        public void updateButtonStatus() {
            if (Gamemode.current.bingoReady) {
                this.completed = task.isCompleted();
            }
            if (completed && !hasOverlay) {
                int color = 0x66FFFFFF; //fallback case, translucent white. Im pretty sure my python library never generates a white color, so white = bad
                if (task.owner != null) {
                    color = task.owner.color;
                }
                ColorBlock overlay = new ColorBlock(this.width(), this.height(), color);
                overlay.x = this.x;
                overlay.y = this.y;
                addToFront(overlay);
                hasOverlay = true;
            }
        }
    }
}