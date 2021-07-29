/*
 * Copyright (C) 2006-2021 Alessandro Ramos da Silva
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.classicomp.chip8.emu;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.Frame;

public class VideoAdapter {

    private byte memory[] = null;
    private Object display = null;
    private Object displayContainer = null;
    
    public static final int MEMORY_SIZE = 2048;

    private final byte SCALE = 8;
    private static final int SCREEN_WIDTH = 64;
    private static final int SCREEN_HEIGHT = 32;

    public VideoAdapter() {
        memory = new byte[SCREEN_WIDTH * SCREEN_HEIGHT];
        reset();
    }

    public void reset() {
        for (int i = 0; i < 2048; i++) {
            memory[i] = 0;
        }
    }
    
    public byte getScale() {
        return SCALE;
    }

    public int getScreenWidth() {
        return SCREEN_WIDTH;
    }

    public int getScreenHeiht() {
        return SCREEN_HEIGHT;
    }

    public void memoryWrite(int index, byte value)
            throws RuntimeException {
        try {
            memory[index] = value;
        } catch (ArrayIndexOutOfBoundsException e) {

        }
    }

    public byte memoryRead(int index) throws RuntimeException {
        try {
            return memory[index];
        } catch (ArrayIndexOutOfBoundsException e) {

        }
        return 0;
    }

    public void setDisplay(Object display) {
        this.display = display;
    }

    public void setDisplayContainer(Object displayContainer) {
        this.displayContainer = displayContainer;

        ((Frame) displayContainer).setResizable(false);

        ((Frame) displayContainer).setSize(64 * this.SCALE + 8 + 2,
                32 * this.SCALE + 50 + 24);
    }

    public void updateDisplay() {
        Graphics g = (Graphics) display;
        int k = 0;

        int startX = 8;
        int startY = 50;

        g.setColor(new Color(170, 170, 0));
        g.fillRect(startX - 4,
                startY - 4,
                (64 * this.SCALE) + 4,
                (32 * this.SCALE) + 4);

        g.setColor(Color.BLACK);
        g.drawRect(startX - 2,
                startY - 2,
                (64 * this.SCALE) - 2,
                (32 * this.SCALE) + 0);

        for (int y = startY;
                y < (this.SCALE * 32 + startY);
                y += this.SCALE) {
            for (int x = startX;
                    x < (this.SCALE * 64 + startX);
                    x += this.SCALE, k++) {
                if (memory[k] != 0) {
                    g.fillRect(x, y, this.SCALE, this.SCALE);
                }
            }
        }
        ((Frame) displayContainer).update(g);
    }
}
