/*
 ***************************************************************************************************
 * Copyright (c) 2017 Federal Institute for Risk Assessment (BfR), Germany
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors: Department Biological Safety - BfR
 *************************************************************************************************
 */
package de.bund.bfr.knime.fsklab.nodes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.util.FileUtil;
import de.bund.bfr.knime.fsklab.nodes.ui.FBrowseButton;
import de.bund.bfr.knime.fsklab.nodes.ui.FLabel;
import de.bund.bfr.knime.fsklab.nodes.ui.FPanel;
import de.bund.bfr.knime.fsklab.nodes.ui.FTextField;
import de.bund.bfr.knime.fsklab.nodes.ui.UIUtils;
import de.bund.bfr.knime.fsklab.util.UTF8Control;
import de.bund.bfr.swing.UI;

public class CreatorNodeDialog extends NodeDialogPane {

  private final CreatorNodeSettings settings;

  private final JTextField modelScriptField;
  private final JTextField parametersScriptField;
  private final JTextField visualizationScriptField;
  private final JTextField workingDirectoryField;
  
  private final JTextField spreadsheetField;
  private final DefaultComboBoxModel<String> sheetModel;

  public CreatorNodeDialog() {

    settings = new CreatorNodeSettings();

    modelScriptField = new FTextField();
    parametersScriptField = new FTextField();
    visualizationScriptField = new FTextField();
    workingDirectoryField = new FTextField();
    
    spreadsheetField = new FTextField();
    sheetModel = new DefaultComboBoxModel<>();

    createUI();
  }

  @Override
  protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
      throws NotConfigurableException {
    try {
      this.settings.load(settings);

      modelScriptField.setText(this.settings.modelScript);
      parametersScriptField.setText(this.settings.parameterScript);
      visualizationScriptField.setText(this.settings.visualizationScript);
      workingDirectoryField.setText(this.settings.getWorkingDirectory());

      spreadsheetField.setText(this.settings.spreadsheet);

      // Populate sheetField with sheet names from the spreadsheet in settings
      sheetModel.removeAllElements();
      new SheetFieldTask(this.settings.spreadsheet).execute();
      // Set selected sheet from settings
      sheetModel.setSelectedItem(this.settings.sheet);

    } catch (InvalidSettingsException exception) {
      throw new NotConfigurableException(exception.getMessage(), exception);
    }
  }

  @Override
  protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {

    this.settings.modelScript = modelScriptField.getText();
    this.settings.parameterScript = parametersScriptField.getText();
    this.settings.visualizationScript = visualizationScriptField.getText();
    this.settings.setWorkingDirectory(workingDirectoryField.getText());
    
    this.settings.spreadsheet = spreadsheetField.getText();
    // selected sheet may be null if there is no selection
    this.settings.sheet = (String) sheetModel.getSelectedItem();

    this.settings.save(settings);
  }

  private void createUI() {

    spreadsheetField.getDocument().addDocumentListener(new SpreadsheetFieldListener());

    FileNameExtensionFilter rFilter = new FileNameExtensionFilter("R script", "r");
    FileNameExtensionFilter spreadsheetFilter =
        new FileNameExtensionFilter("Excel spreadsheet", "xlsx");

    // Build locale with the selected language in the preferences
    ResourceBundle bundle =
        ResourceBundle.getBundle("CreatorNodeBundle", NodeUtils.getLocale(), new UTF8Control());

    // buttons
    String buttonText = bundle.getString("browse_button");

    String modelScriptToolTip = bundle.getString("modelscript_tooltip");
    String parameterScriptToolTip = bundle.getString("parameterscript_tooltip");
    String visualizationScriptToolTip = bundle.getString("visualizationscript_tooltip");

    JButton modelScriptButton =
        UIUtils.createBrowseButton(buttonText, modelScriptField, JFileChooser.OPEN_DIALOG, rFilter);
    JButton parametersScriptButton = UIUtils.createBrowseButton(buttonText, parametersScriptField,
        JFileChooser.OPEN_DIALOG, rFilter);
    JButton visualizationScriptButton = UIUtils.createBrowseButton(buttonText,
        visualizationScriptField, JFileChooser.OPEN_DIALOG, rFilter);
    
    FBrowseButton workingDirectoryButton = new FBrowseButton("Browse");
    workingDirectoryButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
        JFileChooser fc;
        try {
          File file = FileUtil.getFileFromURL(FileUtil.toURL(workingDirectoryField.getText()));
          fc = new JFileChooser(file);
        } catch (Exception ex) {
          fc = new JFileChooser();
        }
        
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);
        
        int response = fc.showOpenDialog(workingDirectoryButton);
        if (response == JFileChooser.APPROVE_OPTION) {
          
          String selectedDirectory = fc.getSelectedFile().getAbsolutePath();
          workingDirectoryField.setText(selectedDirectory);
        }
      }
    });

    modelScriptButton.setToolTipText(modelScriptToolTip);
    parametersScriptButton.setToolTipText(parameterScriptToolTip);
    visualizationScriptButton.setToolTipText(visualizationScriptToolTip);

    // labels
    String modelScriptLabelText = bundle.getString("modelscript_label");
    String paramScriptLabelText = bundle.getString("parameterscript_label");
    String visualizationScriptLabelText = bundle.getString("visualizationscript_label");

    FLabel modelScriptLabel = new FLabel(modelScriptLabelText);
    FLabel parametersScriptLabel = new FLabel(paramScriptLabelText);
    FLabel visualizationScriptLabel = new FLabel(visualizationScriptLabelText);
    FLabel workingDirectoryLabel = new FLabel("Working directory");  // TODO: resource bundle

    // formPanel
    List<FLabel> labels =
        Arrays.asList(modelScriptLabel, parametersScriptLabel, visualizationScriptLabel, workingDirectoryLabel);
    List<JTextField> fields =
        Arrays.asList(modelScriptField, parametersScriptField, visualizationScriptField, workingDirectoryField);
    List<JButton> buttons =
        Arrays.asList(modelScriptButton, parametersScriptButton, visualizationScriptButton, workingDirectoryButton);

    FPanel formPanel = UIUtils.createFormPanel(labels, fields, buttons);
    JPanel northPanel = UI.createNorthPanel(formPanel);
    northPanel.setBackground(UIUtils.WHITE);

    // Metadata panel
    {
      String spreadsheetLabelText = bundle.getString("spreadsheet_label");
      String spreadsheetScriptToolTip = bundle.getString("spreadsheet_tooltip");

      FLabel spreadsheetLabel = new FLabel(spreadsheetLabelText);
      JButton spreadsheetButton = UIUtils.createBrowseButton(buttonText, spreadsheetField,
          JFileChooser.OPEN_DIALOG, spreadsheetFilter);
      spreadsheetButton.setToolTipText(spreadsheetScriptToolTip);

      FLabel sheetLabel = new FLabel(bundle.getString("sheet_label"));
      JComboBox<String> sheetField = new JComboBox<>(sheetModel);
      sheetField.setToolTipText(bundle.getString("sheet_tooltip"));

      List<FLabel> labels2 = Arrays.asList(spreadsheetLabel, sheetLabel);
      List<JComponent> fields2 = Arrays.asList(spreadsheetField, sheetField);
      List<JComponent> buttons2 = Arrays.asList(spreadsheetButton, new JLabel(""));

      FPanel formPanel2 = UIUtils.createFormPanel(labels2, fields2, buttons2);

      FPanel metadataPanel =
          UIUtils.createTitledPanel(formPanel2, bundle.getString("metadata_title"));
      northPanel.add(UIUtils.createNorthPanel(metadataPanel));
    }

    addTab("Options", northPanel);
  }

  private class SpreadsheetFieldListener implements DocumentListener {

    @Override
    public void changedUpdate(DocumentEvent e) {
      sheetModel.removeAllElements();
      new SheetFieldTask(spreadsheetField.getText()).execute();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      sheetModel.removeAllElements();
      new SheetFieldTask(spreadsheetField.getText()).execute();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      sheetModel.removeAllElements();
      new SheetFieldTask(spreadsheetField.getText()).execute();
    }
  }

  private final Object LOCK = new Object();

  private class SheetFieldTask extends SwingWorker<List<String>, Void> {

    private final String path;

    public SheetFieldTask(String path) {
      this.path = path;
    }

    @Override
    protected List<String> doInBackground() throws Exception {

      List<String> names = new ArrayList<>();

      synchronized (LOCK) {

        if (!path.isEmpty()) {
          try {
            File file = FileUtil.getFileFromURL(FileUtil.toURL(path));
            if (file.exists()) {
              try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                  names.add(workbook.getSheetName(i));
                }
              }
            }
          } catch (InvalidPathException | InvalidFormatException | IOException exception) {
            exception.printStackTrace();
          }
        }
      }

      return names;
    }

    @Override
    protected void done() {
      try {
        List<String> names = get();
        names.forEach(sheetModel::addElement);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
  }
}
