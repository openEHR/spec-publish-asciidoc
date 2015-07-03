package org.openehr.docs.magicdraw;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;

import javax.annotation.CheckForNull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;

/**
 * Action which displays its name.
 *
 * @author Bostjan Lah
 */
class OpenEHRExportAction extends MDAction {
    private static final long serialVersionUID = 1L;

    OpenEHRExportAction(@CheckForNull String id, String name) {
        super(id, name, null, null);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    @Override
    public void actionPerformed(ActionEvent e) {
        File outputFolder = chooseFolder();
        if (outputFolder != null) {
            try {
                OpenEHRProjectExporter exporter = new OpenEHRProjectExporter(3);
                exporter.exportProject(outputFolder, Application.getInstance().getProject());

                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Export complete.", "Export",
                                              JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Unable to export data: " + ex.getMessage());
            }
        }
    }

    private File chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setDialogTitle("Select Export Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        File chosen;
        if (chooser.showDialog(MDDialogParentProvider.getProvider().getDialogParent(), "OK") == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            chosen = !Files.exists(selectedFile.toPath()) && Files.exists(selectedFile.toPath().getParent())
                    ? selectedFile.toPath().getParent().toFile()
                    : selectedFile.toPath().toFile();
        } else {
            chosen = null;
        }

        return chosen;
    }
}