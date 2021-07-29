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

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class Keyboard implements KeyListener {

    int pressedKey;
    int typedKey;

    int[][] keys =
          {
            {0x31, 0x01}, {0x32, 0x02}, {0x33, 0x03}, {0x34, 0x0c},
            {0x51, 0x04}, {0x57, 0x05}, {0x45, 0x06}, {0x52, 0x0d},
            {0x41, 0x07}, {0x53, 0x08}, {0x44, 0x09}, {0x46, 0x0e},
            {0x5a, 0x0a}, {0x58, 0x00}, {0x43, 0x0b}, {0x56, 0x0f}
          };

    public Keyboard() {
        pressedKey = -1;
        typedKey = -1;
    }

    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        for (int i = 0; i < 12; i++) {
            if (keys[i][0] == keyCode) {
                pressedKey = keys[i][1];
            }
        }

        System.out.println("keyPressed");
        System.out.println(e.getKeyText(e.getKeyCode()) + " = " + Integer.toHexString(e.getKeyCode()));
    }

    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (pressedKey != -1) {
            for (int i = 0; i < 12; i++) {
                if ((keys[i][0] == keyCode) && (keys[i][1] == pressedKey)) {
                    typedKey = keys[i][1];
                    pressedKey = -1;
                }
            }
        }

    }

    public void keyTyped(KeyEvent e) {
    }

    public int getPressedKey() {
        return pressedKey;
    }

    public int getTypedKey() {
        int result;
        result = typedKey;
        typedKey = -1;
        return result;
    }
}
