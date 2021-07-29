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

import java.io.FileInputStream;
import java.io.IOException;

public class EmulatorCore implements Runnable {

    private byte[] v = new byte[16]; // general purpose registers
    private int i; // addressing register
    private int pc = 0; // program counter
    private int sp = 0; // stack pointer
    
    private int[] stack = new int[16];
    short memory[] = new short[0xfff];
    
    short reserved[] = new short[80]; // temp store for bcd fonts 0x000 to 0x200
    
    private short opcode = 0;
    private byte delay = 0;
    private byte sound = 0;

    VideoAdapter video = new VideoAdapter();
    Keyboard keyboard = new Keyboard();

    public EmulatorCore() {
        loadFonts();
    }

    public VideoAdapter getVideoAdapter() {
        return this.video;
    }

    public void loadRom(String romName) throws IOException {
        FileInputStream f = new FileInputStream(romName);
        
        // reset the machine
        this.reset();
        
        int a = 0;
        short data;
        
        // now reverse store bcd fonts to avoid address rewriting
        // programs found on web usualy refer to 0x000 as start
        // and it conflicts with reserved addresses
        for (int b = 0xfff - 80; b < 0xfff; b++) {
            memory[b] = reserved[a++];
        }

        a = 0;
        while ((data = (short) f.read()) != -1) {
            memory[a++] = data;
        }
        
    }

    public void reset() {
        // cleanup special registers
        pc = 0;
        sp = 0;
        i = 0;

        opcode = 0;
        delay = 0;
        sound = 0;
        
        // cleanup general purpose registers
        for (int a = 0; a < 16; a++) {
            v[a] = 0;
        }
        
        // cleanup RAM
        for (int a = 0; a < 0xfff - 80; a++) {
            memory[a] = 0x00ff;
        }
        
        video.reset();
    }
    
    boolean keyPressed(int keyCode) {
        return keyboard.getPressedKey() == keyCode;
    }

    int readKey() {
        int key;
        //System.out.println("ReadKey!");
        try {
            while ((key = keyboard.getTypedKey()) == -1) {
                Thread.sleep(1);
            }
            return key;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public void setDisplay(Object o) {
        video.setDisplay(o);
    }

    public void setDisplayContainer(Object o) {
        video.setDisplayContainer(o);
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    // Interpreta os opcodes
    void execute() throws Exception {
        opcode = (short) ((((short) memory[pc] << 8) & 0x0000ff00) + ((short) memory[pc + 1] & 0xff));
        // Hex string versions for debugging
        String hxpc = Integer.toHexString(pc);
        String hxcode = Integer.toHexString(opcode);
        //System.out.println(hxpc + ":"
        //        + hxcode);

        switch ((opcode & 0xf000) >>> 12) {
            case 0x00:
                switch (y()) {
                    // 00CN Scroll down N lines (***)
                    case 0x0c:
                        // implementar
                        pc += 2;
                        break;
                    case 0x0e:
                        // 00E0 Erase the screen
                        if ((opcode & 0x000f) == 0x00) {
                            // implementar
                            for (int i = 0; i < VideoAdapter.MEMORY_SIZE; i++) {
                                video.memoryWrite(i, (byte) 0);
                            }
                            pc += 2;
                        } else { // 00EE Return from a CHIP-8 sub-routine
                            pc = stack[--sp];
                        }
                        break;
                    case 0x0f:
                        // 00FB Scroll 4 pixels right (***)
                        // 00FC Scroll 4 pixels left (***)

                        // 00FD Quit the emulator (***)
                        // 00FE Set CHIP-8 graphic mode (***)
                        // 00FF Set SCHIP graphic mode (***)
                        pc += 2;
                        break;
                }
                break;

            // 1NNN Jump to NNN
            case 0x01:
                pc = (opcode & 0x0fff) - 0x0200;
                break;

            // 2NNN Call CHIP-8 sub-routine at NNN (16 successive calls max)
            case 0x02:
                stack[sp++] = pc + 2;
                pc = (opcode & 0x0fff) - 0x0200;
                break;

            // 3XKK Skip next instruction if VX == KK
            case 0x03:
                pc += (v[x()] == kk()) ? 4 : 2;
                break;

            // 4XKK Skip next instruction if VX != KK
            case 0x04:
                pc += (v[x()] != kk()) ? 4 : 2;
                break;

            // 5XY0 Skip next instruction if VX == VY
            case 0x05:
                pc += (v[x()] == v[y()]) ? 4 : 2;
                break;

            // 6XKK VX = KK
            case 0x06:
                v[x()] = (byte) (kk());
                pc += 2;
                break;

            // 7XKK VX = VX + KK
            case 0x07:
                v[x()] += (kk());
                pc += 2;
                break;

            // 8XY* Muitas operações envolvendo VX e VY
            case 0x08:
                switch (opcode & 0x000f) {
                    // 8XY0 VX = VY
                    case 0x00:
                        v[x()] = v[y()];
                        break;

                    // 8XY1 VX = VX OR VY
                    case 0x01:
                        v[x()] = (byte) (v[x()] | v[y()]);
                        break;

                    // 8XY2 VX = VX AND VY
                    case 0x02:
                        v[x()] = (byte) (v[x()] & v[y()]);
                        break;

                    // 8XY3 VX = VX XOR VY (*)
                    case 0x03:
                        v[x()] = (byte) (v[x()] ^ v[y()]);
                        break;

                    // @TODO: review this instruction
                    // 8XY4 VX = VX + VY, VF = carry
                    case 0x04:
                        // v[0x0f] = (byte)((v[x()] + v[y()]) < 0x00ff ? 0 : 1);
                        v[0x0f] = (byte) ((v[x()] + v[y()]) - 0xff);
                        if (v[0x0f] < 0) {
                            v[0x0f] = 0;
                        }
                        v[x()] = (byte) (v[x()] + v[y()]);
                        break;

                    // 8XY5 VX = VX - VY, VF = not borrow (**)
                    case 0x05:
                        v[0x0f] = (byte) (v[x()] >= v[y()] ? 0x01 : 0x00);
                        v[x()] = (byte) (v[x()] - v[y()]);
                        break;

                    // 8XY6 VX = VX SHR 1 (VX=VX/2), VF = carry
                    case 0x06:
                        v[0x0f] = (byte) (v[x()] & 0x01);
                        v[x()] = (byte) (v[x()] >>> 1);
                        break;

                    // 8XY7 VX = VY - VX, VF = not borrow (*) (**)
                    case 0x07:
                        v[0x0f] = (byte) (v[y()] >= v[x()] ? 0x01 : 0x00);
                        v[x()] = (byte) (v[y()] - v[x()]);
                        break;

                    // 8XYE VX = VX SHL 1 (VX=VX*2), VF = carry
                    case 0x0e:
                        v[0x0f] = (byte) (v[x()] & 0x40);
                        v[x()] = (byte) (v[x()] << 1);
                        break;
                }
                pc += 2;
                break;

            // 9XY0 Skip next instruction if VX != VY
            case 0x09:
                pc += (v[x()] != v[y()]) ? 4 : 2;
                break;

            // ANNN I = NNN
            case 0x0a:
                i = (opcode & 0x0fff) - 0x0200;
                pc += 2;
                break;

            // BNNN Jump to NNN + V0
            case 0x0b:
                pc = (opcode & 0x0fff) + v[0x00];
                break;

            // CXKK VX = Random number AND KK
            case 0x0c:
                v[x()] = (byte) ((byte) ((1000 * Math.random()) % 256)
                        & (kk()));
                pc += 2;
                break;

            // DXYN Draws a sprite at (VX,VY) starting at M(I). VF = collision.
            // If N=0, draws the 16 x 16 sprite, else an 8 x N sprite.
            case 0x0d:
                int spriteW,
                 spriteH;
                int coordX,
                 coordY;

                coordX = x();
                coordY = y();

                v[0x0f] = 0; // Limpa colisões anteriores
                // @TODO: Substituir este tratamento para ao 
                // invés de utiliar height, usar a qtde de
                // bytes do opcode (terceiro parâmetro)
                if ((opcode & 0x000f) == 0) {
                    spriteW = 16;
                    spriteH = 16;
                } else {
                    spriteW = 8;
                    spriteH = opcode & 0x000f;
                }

                for (int yline = 0; yline < (spriteH); yline++) {
                    short data = memory[i + yline]; //this retreives the byte for a give line of pixels
                    for (int xpix = 0; xpix < spriteW; xpix++) {
                        if ((data & (0x80 >>> xpix)) != 0) {
                            if (video.memoryRead(v[coordX] + xpix + ((v[coordY] + yline) * 64)) == 1) {
                                v[0x0f] = 1; //there has been a collision
                            }
                            video.memoryWrite(v[coordX] + xpix + ((v[coordY] + yline) * 64),
                                    (byte) ((video.memoryRead(v[coordX] + xpix + ((v[coordY] + yline) * 64)) ^ 0x0001))); //note: coordinate registers from opcode
                        }
                    }
                }
                video.updateDisplay();
                pc += 2;
                break;

            case 0x0e:
                // EX9E Skip next instruction if key VX pressed
                if ((y()) == 0x09) {
                    pc += keyPressed(v[x()]) ? 4 : 2;
                } else // EXA1 Skip next instruction if key VX not pressed
                {
                    pc += !keyPressed(v[x()]) ? 4 : 2;
                }
                break;
            case 0x0f:
                switch (y()) {
                    case 0x00:
                        // FX0A Waits a keypress and stores it in VX
                        if ((opcode & 0x000f) == 0x0a) {
                            v[x()] = (byte) readKey();
                        } else // FX07 VX = Delay timer
                        {
                            v[x()] = delay;
                        }
                        pc += 2;
                        break;
                    case 0x01:
                        switch (opcode & 0x000f) {
                            // FX15 Delay timer = VX
                            case 0x05:
                                delay = v[x()];
                                break;
                            // FX18 Sound timer = VX
                            case 0x08:
                                sound = v[x()];
                                break;
                            // FX1E I = I + VX
                            case 0x0e:
                                i = i + v[x()];
                                break;
                        }
                        pc += 2;
                        break;
                    // FX29 I points to the 4 x 5 font sprite of hex char in VX
                    case 0x02:
                        i = (v[x()] * 5) + 0xfff - 80;
                        pc += 2;

                        break;

                    // FX33 Store BCD representation of VX in M(I)...M(I+2)
                    case 0x03:
                        byte vx = (byte) v[x()];

                        memory[i] = (short) ((vx / 100) % 10);
                        memory[i + 1] = (short) ((vx / 10) % 10);
                        memory[i + 2] = (short) (vx % 10);

                        pc += 2;
                        break;

                    // FX55 Save V0...VX in memory starting at M(I)
                    case 0x05:
                        for (int j = 0x00, k = i; j <= (x()); j++) {
                            // Registers store 2 bytes, but memory positions store just 1
                            if (j % 2 == 0) {
                                memory[k++] = (byte) ((v[j] & 0xff00) >>> 8);
                            } else {
                                memory[k++] = (byte) (v[j] & 0x00ff);
                            }
                        }
                        pc += 2;
                        break;

                    // FX65 Load V0...VX from memory starting at M(I)
                    case 0x06:
                        for (int j = 0x00, k = i; j <= (x()); j++) {
                            v[j] = (byte) memory[k++];
                        }
                        i = i + x() + 1;
                        pc += 2;
                        break;
                }
                break;
        }


        /*
FX75 Save V0...VX (X<8) in the HP48 flags (***)
FX85 Load V0...VX (X<8) from the HP48 flags (***)

(*): Used to be undocumented (but functional) in the original docs.
(**): When you do VX - VY, VF is set to the negation of the borrow.
      This means that if VX is superior or equal to VY, VF will be set to 01,
      as the borrow is 0. If VX is inferior to VY, VF is set to 00, as the borrow is 1.
(***): SCHIP Instruction. Can be used in CHIP8 graphic mode */
    }

    public void run() {
        int c;
        try {
            do {
                for (int k = 0; k < 1000; k++) {
                    execute();
                }
                Thread.sleep(17);
                c = 0;
                while (delay > 0) {
                    Thread.sleep(17);
                    delay--;
                }

                c = 0;
                while (sound > 0) {
                    Thread.sleep(17);
                    sound--;
                }

            } while (opcode != 0x0000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public int x() {
        return (opcode & 0x0f00) >>> 8;
    }

    public int y() {
        return (opcode & 0x00f0) >>> 4;
    }

    public int kk() {
        return opcode & 0x00ff;
    }

    private void loadFonts() {
        //"0"	Hex
        reserved[00] = 0xF0; reserved[01] = 0x90; reserved[02] = 0x90; reserved[03] = 0x90; reserved[04] = 0xF0;
        // "1"	Hex
        reserved[05] = 0x20; reserved[06] = 0x60; reserved[07] = 0x20; reserved[8 ] = 0x20; reserved[9 ] = 0x70;
        // "2"	Hex
        reserved[10] = 0xF0; reserved[11] = 0x10; reserved[12] = 0xF0; reserved[13] = 0x80; reserved[14] = 0xF0;
        // "3"	Hex
        reserved[15] = 0xF0; reserved[16] = 0x10; reserved[17] = 0xF0; reserved[18] = 0x10; reserved[19] = 0xF0;
        // "4"	Hex
        reserved[20] = 0x90; reserved[21] = 0x90; reserved[22] = 0xF0; reserved[23] = 0x10; reserved[24] = 0x10;
        // "5"	Hex
        reserved[25] = 0xF0; reserved[26] = 0x80; reserved[27] = 0xF0; reserved[28] = 0x10; reserved[29] = 0xF0;
        // "6"	Hex
        reserved[30] = 0xF0; reserved[31] = 0x80; reserved[32] = 0xF0; reserved[33] = 0x90; reserved[34] = 0xF0;
        // "7"	Hex
        reserved[35] = 0xF0; reserved[36] = 0x10; reserved[37] = 0x20; reserved[38] = 0x40; reserved[39] = 0x40;
        // "8"	Hex
        reserved[40] = 0xF0; reserved[41] = 0x90; reserved[42] = 0xF0; reserved[43] = 0x90; reserved[44] = 0xF0;
        // "9"	Hex
        reserved[45] = 0xF0; reserved[46] = 0x90; reserved[47] = 0xF0; reserved[48] = 0x10; reserved[49] = 0xF0;
        // "A"	Hex
        reserved[50] = 0xF0; reserved[51] = 0x90; reserved[52] = 0xF0; reserved[53] = 0x90; reserved[54] = 0x90;
        // "B"	Hex
        reserved[55] = 0xE0; reserved[56] = 0x90; reserved[57] = 0xE0; reserved[58] = 0x90; reserved[59] = 0xE0;
        // "C"	Hex
        reserved[60] = 0xF0; reserved[61] = 0x80; reserved[62] = 0x80; reserved[63] = 0x80; reserved[64] = 0xF0;
        // "D"	Hex
        reserved[65] = 0xE0; reserved[66] = 0x90; reserved[67] = 0x90; reserved[68] = 0x90; reserved[69] = 0xE0;
        // "E"	Hex
        reserved[70] = 0xF0; reserved[71] = 0x80; reserved[72] = 0xF0; reserved[73] = 0x80; reserved[74] = 0xF0;
        // "F"	Hex
        reserved[75] = 0xF0; reserved[76] = 0x80; reserved[77] = 0xF0; reserved[78] = 0x80; reserved[79] = 0x80;
    }
    
}
