package org.vincentyeh.audiomerger.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.vincentyeh.audiomerger.merger.concrete.DefaultAudioMerger;
import org.vincentyeh.audiomerger.merger.framework.AudioFileType;
import org.vincentyeh.audiomerger.merger.framework.AudioMerger;
import org.vincentyeh.audiomerger.recorder.concrete.SrtRecorder;
import org.vincentyeh.audiomerger.recorder.framework.Recorder;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
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
            File[] files = selectFilesPathToOpen();
            if (files != null) {
                sources.addAll(Arrays.asList(files));
                updateSourceList();
            }
        });

        mergeButton.addActionListener(e -> {
            AudioFileType type = (AudioFileType) comboBox_file_format.getSelectedItem();
            File destination = selectFilePathToSave();

            if (destination == null)
                return;

            new Thread(() -> {
                AudioMerger merger = new DefaultAudioMerger(type);
                try {
                    panel_progress.setVisible(true);
                    merger.merge(sources, destination,
                            (index, total) -> {
                                progressBar_progress.setMaximum(total);
                                progressBar_progress.setValue(index + 1);
                                label_progress.setText(format("%d/%d", index + 1, total));
                            }, getRecorder(new File(destination.getParent(), destination.getName() + ".srt")));
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
        list_sources.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedIndex = list_sources.getSelectedIndex();
                if (selectedIndex == -1)
                    return;
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    sources.remove(selectedIndex);
                    EventQueue.invokeLater(MainFrame.this::updateSourceList);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (selectedIndex >= 1) {
                        sources.add(selectedIndex - 1, sources.get(selectedIndex));
                        sources.remove(selectedIndex + 1);
                        EventQueue.invokeLater(MainFrame.this::updateSourceList);
                        EventQueue.invokeLater(() -> list_sources.setSelectedIndex(selectedIndex - 1));
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (selectedIndex < sources.size() - 1) {
                        sources.add(selectedIndex + 2, sources.get(selectedIndex));
                        sources.remove(selectedIndex);
                        EventQueue.invokeLater(MainFrame.this::updateSourceList);
                        EventQueue.invokeLater(() -> list_sources.setSelectedIndex(selectedIndex + 1));
                    }
                }
            }
        });
    }

    private Recorder getRecorder(File recordDestination) throws FileNotFoundException {
        return new SrtRecorder(recordDestination) {
            @Override
            protected String getSubtitle(File file) {
                return file.getName();
            }
        };
    }

    private File selectFilePathToSave() {
        JFileChooser chooser = getReadOnlyChooser();
        chooser.setCurrentDirectory(new File("").getAbsoluteFile());
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.putClientProperty("FileChooser.readOnly", Boolean.TRUE);
        int option = chooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private File[] selectFilesPathToOpen() {
        JFileChooser chooser = getReadOnlyChooser();
        chooser.setCurrentDirectory(new File("").getAbsoluteFile());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Audio file(*.wav *.au *.aiff)", "wav", "au", "aiff"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Select audio files");
        int option = chooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFiles();
        }


        return null;
    }

    private JFileChooser getReadOnlyChooser() {
        Boolean old = UIManager.getBoolean("FileChooser.readOnly");
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        JFileChooser chooser = new JFileChooser();
        UIManager.put("FileChooser.readOnly", old);
        return chooser;
    }

    private void updateSourceList() {
        int index = 0;
        DefaultListModel<String> model = new DefaultListModel<>();
        for (File file : sources) {
            model.add(index++, file.getName());
        }
        list_sources.setModel(model);
        list_sources.updateUI();
        mergeButton.setEnabled(sources.size() != 0);
        clearAllButton.setEnabled(sources.size() != 0);
    }

//    private static JFileChooser getFixedFileChooser() {
//        JFileChooser chooser = new JFileChooser();
//        stream(chooser)
//                .filter(JList.class::isInstance)
//                .map(JList.class::cast)
//                .findFirst()
//                .ifPresent(MainFrame::addHierarchyListener);
//        return chooser;
//    }
//
//    // @see https://github.com/aterai/java-swing-tips/blob/master/GetComponentsRecursively/src/java/example/MainPanel.java
//    private static Stream<Component> stream(Container parent) {
//        return Arrays.stream(parent.getComponents())
//                .filter(Container.class::isInstance)
//                .map(c -> stream(Container.class.cast(c)))
//                .reduce(Stream.of(parent), Stream::concat);
//    }
//
//    private static void addHierarchyListener(JList<?> list) {
//        list.addHierarchyListener(e -> {
//            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
//                    && e.getComponent().isShowing()) {
//                list.putClientProperty("List.isFileList", Boolean.FALSE);
//                list.setLayoutOrientation(JList.VERTICAL);
//            }
//        });
//    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JPanel();
        root.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        root.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("text", "ui.output_format"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        comboBox_file_format = new JComboBox();
        panel1.add(comboBox_file_format, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        root.add(spacer2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        root.add(panel2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addButton = new JButton();
        this.$$$loadButtonText$$$(addButton, this.$$$getMessageFromBundle$$$("text", "ui.add"));
        panel2.add(addButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mergeButton = new JButton();
        mergeButton.setEnabled(false);
        this.$$$loadButtonText$$$(mergeButton, this.$$$getMessageFromBundle$$$("text", "ui.merge"));
        panel2.add(mergeButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearAllButton = new JButton();
        clearAllButton.setEnabled(false);
        this.$$$loadButtonText$$$(clearAllButton, this.$$$getMessageFromBundle$$$("text", "ui.clearall"));
        panel2.add(clearAllButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        list_sources = new JList();
        scrollPane1.setViewportView(list_sources);
        panel_progress = new JPanel();
        panel_progress.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        root.add(panel_progress, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("text", "ui.progress"));
        panel_progress.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar_progress = new JProgressBar();
        panel_progress.add(progressBar_progress, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label_progress = new JLabel();
        label_progress.setText("");
        panel_progress.add(label_progress, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("text", "ui.list.hint"));
        root.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }
}
