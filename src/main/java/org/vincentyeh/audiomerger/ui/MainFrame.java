package org.vincentyeh.audiomerger.ui;

import org.vincentyeh.audiomerger.merger.concrete.DefaultAudioMerger;
import org.vincentyeh.audiomerger.merger.framework.AudioFileType;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

public class MainFrame {
    public JPanel root;
    private JList<String> list_sources;
    private JButton addButton;
    private JComboBox<AudioFileType> comboBox_file_format;
    private JButton mergeButton;
    private JButton clearAllButton;
    private JProgressBar progressBar_progress;
    private JLabel label_progress;
    private JPanel panel_progress;

    private final List<File> sources = new LinkedList<>();

    public MainFrame() {
        panel_progress.setVisible(false);
        comboBox_file_format.setModel(new DefaultComboBoxModel<>(AudioFileType.values()));
        comboBox_file_format.updateUI();
        addButton.addActionListener(e -> {
            var files = selectFilesPathToOpen();
            if (files != null) {
                sources.addAll(Arrays.asList(files));
                updateSourceList();
            }
        });

        mergeButton.addActionListener(e -> {
            var type = (AudioFileType) comboBox_file_format.getSelectedItem();
            var destination = selectFilePathToSave();

            if (destination == null)
                return;

            new Thread(() -> {
                var merger = new DefaultAudioMerger(type);
                try {
                    panel_progress.setVisible(true);
                    merger.merge(sources, destination,
                            (index, total) -> {
                                progressBar_progress.setMaximum(total);
                                progressBar_progress.setValue(index + 1);
                                label_progress.setText(format("%d/%d",index+1,total));
                            });
                    panel_progress.setVisible(false);
                } catch (IOException | UnsupportedAudioFileException ex) {
                    throw new RuntimeException(ex);
                }
            }).start();
        });
        clearAllButton.addActionListener(e -> {
            sources.clear();
            updateSourceList();
        });
    }

    private File selectFilePathToSave() {
        var chooser = getFixedFileChooser();
        chooser.setCurrentDirectory(new File("").getAbsoluteFile());
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        var option = chooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private File[] selectFilesPathToOpen() {
        var chooser = getFixedFileChooser();
        chooser.setCurrentDirectory(new File("").getAbsoluteFile());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Audio file(*.wav *.au *.aiff)", "wav", "au", "aiff"));

        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Select audio files");
        var option = chooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFiles();
        }
        return null;
    }

    private void updateSourceList() {
        int index = 0;
        var model = new DefaultListModel<String>();
        for (var file : sources) {
            model.add(index++, file.getName());
        }
        list_sources.setModel(model);
        list_sources.updateUI();
        mergeButton.setEnabled(sources.size() != 0);
        clearAllButton.setEnabled(sources.size() != 0);
    }

    private static JFileChooser getFixedFileChooser() {
        var chooser = new JFileChooser();
        stream(chooser)
                .filter(JList.class::isInstance)
                .map(JList.class::cast)
                .findFirst()
                .ifPresent(MainFrame::addHierarchyListener);
        return chooser;
    }

    // @see https://github.com/aterai/java-swing-tips/blob/master/GetComponentsRecursively/src/java/example/MainPanel.java
    private static Stream<Component> stream(Container parent) {
        return Arrays.stream(parent.getComponents())
                .filter(Container.class::isInstance)
                .map(c -> stream(Container.class.cast(c)))
                .reduce(Stream.of(parent), Stream::concat);
    }

    private static void addHierarchyListener(JList<?> list) {
        list.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        && e.getComponent().isShowing()) {
                    list.putClientProperty("List.isFileList", Boolean.FALSE);
                    list.setLayoutOrientation(JList.VERTICAL);
                }
            }
        });
    }


}
