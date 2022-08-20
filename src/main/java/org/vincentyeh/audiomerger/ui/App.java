package org.vincentyeh.audiomerger.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        var frame = new JFrame("audio-merger");
        frame.setContentPane(new MainFrame().root);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                frame.dispose();
            }
        });
    }
}
