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
package com.classicomp.chip8.app;

import java.awt.Event;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Toolkit;
import javax.swing.SwingWorker;

import com.classicomp.chip8.emu.EmulatorCore;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;

public class Chip8App extends Frame {

    EmulatorCore emu;
    Image display;

    public Chip8App() {
        super("Classicomp Chip-8");

        initMenu();

        // @TODO: Resize/Reposition window
        setSize(400, 400);
        setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width - 400) / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height - 400) / 2
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
              System.exit(0);
            }
        });

        setVisible(true);

        try {
            emu = new EmulatorCore();
            addKeyListener(emu.getKeyboard());

            display = createImage(emu.getVideoAdapter().getScreenWidth() * emu.getVideoAdapter().getScale() + 5,
                    emu.getVideoAdapter().getScreenHeiht() * emu.getVideoAdapter().getScale() + 4 + 50);
            
            emu.setDisplayContainer(this);
            emu.setDisplay(display.getGraphics());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new Chip8App();
                    }
                });

    }

    public void update(Graphics g) {
        g = getGraphics();
        g.drawImage(display, 0, 20, display.getWidth(this), display.getHeight(this), null);
        paint(g);
    }

    // @TODO: consider turn menu variables into class attributes
    private void initMenu() {
        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu("File");
        MenuItem miLoadRom = new MenuItem("Load Rom");
        MenuItem miExit = new MenuItem("Exit");

        menuFile.add(miLoadRom);
        menuFile.add(miExit);

        menuBar.add(menuFile);

        Menu menuHelp = new Menu("Help");
        MenuItem miAbout = new MenuItem("About");

        menuHelp.add(miAbout);

        menuBar.add(menuHelp);

        this.setMenuBar(menuBar);
    }

    public boolean action(Event e, Object o) {
        if (e.target instanceof MenuItem) {
            MenuItem miTarget = (MenuItem) e.target;
            if (miTarget.getLabel().equals("Load Rom")) {
                if (showRomDialog())
                    miTarget.setEnabled(false);
                return true;
            }
            if (miTarget.getLabel().equals("Exit")) {
                System.exit(0);
                return true;
            }
            if (miTarget.getLabel().equals("About")) {
                JOptionPane.showMessageDialog(this, "Developed by Alessandro Ramos da Silva");
            }
        }
        return false;
    }
    
    public boolean showRomDialog() {
        FileDialog fileDialog = 
                new FileDialog(
                        this,
                        "Load ROM",
                        FileDialog.LOAD
                );
        fileDialog.show();
        
        if (fileDialog.getFile() != null) {
            String romPath = fileDialog.getDirectory() + fileDialog.getFile();
            try {
                this.emu.loadRom(romPath);

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Chip8App.this.emu.run();
                        return null;
                    }
                };
                worker.execute();
                
                return true;
            } catch(Exception e) {
                System.out.println(e.toString());
            }
        }
        
        return false;
    }

}
