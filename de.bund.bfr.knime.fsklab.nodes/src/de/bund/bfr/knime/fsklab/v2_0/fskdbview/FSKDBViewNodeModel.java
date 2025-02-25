/*
 ***************************************************************************************************
 * Copyright (c) 2020 Federal Institute for Risk Assessment (BfR), Germany
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
package de.bund.bfr.knime.fsklab.v2_0.fskdbview;

import de.bund.bfr.knime.fsklab.v2_0.editor.FSKEditorJSNodeFactory;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.web.ValidationError;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.JSONDataTable.JSONDataTableRow;
import org.knime.js.core.node.AbstractWizardNodeModel;


/**
 * This is an implementation of the node model of the "FSKDBView" node.
 * 
 * This node visualizes FSK models as a HTML table
 */
public class FSKDBViewNodeModel
    extends AbstractWizardNodeModel<FSKDBViewRepresentation, FSKDBViewValue> {
  // Input and output port types
  private static final PortType[] IN_TYPES = {BufferedDataTable.TYPE_OPTIONAL};
  private static final PortType[] OUT_TYPES = {BufferedDataTable.TYPE};
  private static JSONDataTable emptyJSONTable;
  private static final String VIEW_NAME = new FSKEditorJSNodeFactory().getInteractiveViewName();

  protected FSKDBViewNodeModel() {
    super(IN_TYPES, OUT_TYPES, VIEW_NAME);
  }

  /**
   * The logger is used to print info/warning/error messages to the KNIME console and to the KNIME
   * log file. Retrieve it via 'NodeLogger.getLogger' providing the class of this node model.
   */
  private static final NodeLogger LOGGER = NodeLogger.getLogger(FSKDBViewNodeModel.class);

  /**
   * The settings key to retrieve and store settings shared between node dialog and node model. In
   * this case, the key for the online repository URL String that should be entered by the user in
   * the dialog.
   */
  static final String KEY_REPOSITORY_LOCATION = "repository_location";
  static final String KEY_SHOW_DOWNLOAD_BUTTON = "download_button";
  static final String KEY_SHOW_DETAILS_BUTTON = "details_button";
  static final String KEY_SHOW_EXECUTE_BUTTON = "execute_button";
  static final String KEY_SHOW_HEADER = "header";
  static final String KEY_SANDWICH_LIST = "sandwichList";
  static final String KEY_TITLE = "tilte";
  static final String KEY_TOKEN = "token";

  /**
   * The default online repository URL String.
   */
  private static final String DEFAULT_REPOSITORY_LOCATION = "https://knime.bfr.berlin/landingpage/DB/";

  /**
   * The settings model to manage the shared settings. This model will hold the value entered by the
   * user in the dialog and will update once the user changes the value. Furthermore, it provides
   * methods to easily load and save the value to and from the shared settings (see: <br>
   * {@link #loadValidatedSettingsFrom(NodeSettingsRO)}, {@link #saveSettingsTo(NodeSettingsWO)}).
   * <br>
   * Here, we use a SettingsModelString as the online repository URL is a String. Also have a look
   * at the comments in the constructor of the {@link FSKDBViewNodeDialog} as the settings models
   * are also used to create simple dialogs.
   */
  SettingsModelString m_repositoryLocationSettings = createRepositoryLocationSettingsModel();
  SettingsModelString m_TitleSettings = createTitleSettingsModel();
  SettingsModelString m_SandwichListSettings = createSandwichListnSettingsModel();


  /**
   * A convenience method to create a new settings model used for the online repository URL String.
   * This method will also be used in the {@link FSKDBViewNodeDialog}. The settings model will sync
   * via the above defined key.
   * 
   * @return a new SettingsModelString with the key for the online repository URL String
   */
  static SettingsModelString createRepositoryLocationSettingsModel() {
    return new SettingsModelString(KEY_REPOSITORY_LOCATION, DEFAULT_REPOSITORY_LOCATION);
  }

  /**
   * The settings key to retrieve and store settings shared between node dialog and node model. In
   * this case, the key for the maximum number of models that are allowed to be selected that should
   * be entered by the user in the dialog.
   */
  static final String MAX_SELECTION_NUMBER = "max_selection_number";
  static final String SELECTION = "selection";

  /**
   * The default the maximum number of models that are allowed to be selected.
   */
  private static final int DEFAULT_MAX_SELECTION_NUMBER = 2;

  /**
   * The settings model to manage the shared settings. This model will hold the value entered by the
   * user in the dialog and will update once the user changes the value. Furthermore, it provides
   * methods to easily load and save the value to and from the shared settings (see: <br>
   * {@link #loadValidatedSettingsFrom(NodeSettingsRO)}, {@link #saveSettingsTo(NodeSettingsWO)}).
   * <br>
   * Here, we use a SettingsModelNumber as the the maximum number of models that are allowed to be
   * selected is a number. Also have a look at the comments in the constructor of the
   * {@link FSKDBViewNodeDialog} as the settings models are also used to create simple dialogs.
   */
  SettingsModelNumber m_maxSelectionNumberSettings = createMaxSelectionNumberSettingsModel();

  SettingsModelString m_showDownloadSettings = createShowDownloadButtonSettingsModel();
  SettingsModelString m_showDetailsSettings = createShowDetailsButtonSettingsModel();
  SettingsModelString m_showExecuteSettings = createShowExecuteButtonSettingsModel();
  SettingsModelString m_showHeaderSettings = createShowHeaderButtonSettingsModel();
  SettingsModelString m_TokenSettings = createTokenSettingsModel();

  final SettingsModelStringArray selectionSettings = createSelectionSettingsModel();


  /**
   * A convenience method to create a new settings model used for the maximum number of models
   * allowed to be selected number. Minimum 2 models and maximum is 4 are allowed to be selected in
   * the JS View HTML table This method will also be used in the {@link FSKDBViewNodeDialog}. The
   * settings model will sync via the above defined key.
   * 
   * @return a new SettingsModelNumber with the key for the the maximum number of models allowed to
   *         be selected number (between 2 and 4)
   */
  static SettingsModelNumber createMaxSelectionNumberSettingsModel() {
    return new SettingsModelIntegerBounded(MAX_SELECTION_NUMBER, DEFAULT_MAX_SELECTION_NUMBER, 2,
        4);
  }

  static SettingsModelString createShowDownloadButtonSettingsModel() {
    return new SettingsModelString(KEY_SHOW_DOWNLOAD_BUTTON, "false");
  }

  static SettingsModelString createShowExecuteButtonSettingsModel() {
    return new SettingsModelString(KEY_SHOW_EXECUTE_BUTTON, "false");
  }

  static SettingsModelString createShowDetailsButtonSettingsModel() {
    return new SettingsModelString(KEY_SHOW_DETAILS_BUTTON, "false");
  }

  static SettingsModelString createShowHeaderButtonSettingsModel() {
    return new SettingsModelString(KEY_SHOW_HEADER, "false");
  }

  static SettingsModelStringArray createSelectionSettingsModel() {
    return new SettingsModelStringArray(SELECTION, new String[0]);
  }

  static SettingsModelString createSandwichListnSettingsModel() {
    return new SettingsModelString(KEY_SANDWICH_LIST,
        "[{\"title\":\"RAKIP Model Repository (login)\",\"href\":\"https://knime.bfr.berlin/knime/#/RAKIP-Web/7._FSK_Repository_Model_Runner&run\"},{\"title\":\"Infos\",\"href\":\"#\"},{\"title\":\"Imprint\",\"href\":\"#\"},{\"title\":\"Privacy Policy\",\"href\":\"#\"}]");
  }

  static SettingsModelString createTitleSettingsModel() {
    return new SettingsModelString(KEY_TITLE, "FSK-Web Landing Page");
  }
  static SettingsModelString createTokenSettingsModel() {
    return new SettingsModelString(KEY_TOKEN, "?repository=FSK-Web&status=Curated");
  }
  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    m_repositoryLocationSettings.saveSettingsTo(settings);
    m_SandwichListSettings.saveSettingsTo(settings);
    m_TitleSettings.saveSettingsTo(settings);
    m_maxSelectionNumberSettings.saveSettingsTo(settings);
    m_showDownloadSettings.saveSettingsTo(settings);
    m_showDetailsSettings.saveSettingsTo(settings);
    m_showExecuteSettings.saveSettingsTo(settings);
    m_showHeaderSettings.saveSettingsTo(settings);
    m_TokenSettings.saveSettingsTo(settings);
    FSKDBViewValue viewValue = getViewValue();
    if (viewValue != null && viewValue.getSelection() != null
        && viewValue.getSelection().length > 0) {
      selectionSettings.setStringArrayValue(viewValue.getSelection());

    }
    selectionSettings.saveSettingsTo(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    m_repositoryLocationSettings.loadSettingsFrom(settings);
    m_maxSelectionNumberSettings.loadSettingsFrom(settings);
    if (settings.containsKey(KEY_SHOW_DOWNLOAD_BUTTON)) {
      m_showDownloadSettings.loadSettingsFrom(settings);
      m_showDetailsSettings.loadSettingsFrom(settings);
      m_showExecuteSettings.loadSettingsFrom(settings);
      m_showHeaderSettings.loadSettingsFrom(settings);
      m_SandwichListSettings.loadSettingsFrom(settings);
      m_TitleSettings.loadSettingsFrom(settings);
    }
    if (settings.containsKey(SELECTION))
      selectionSettings.loadSettingsFrom(settings);
    if (settings.containsKey(KEY_TOKEN))
      m_TokenSettings.loadSettingsFrom(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    m_repositoryLocationSettings.validateSettings(settings);
    m_maxSelectionNumberSettings.validateSettings(settings);
    if (settings.containsKey(KEY_SHOW_DOWNLOAD_BUTTON)) {
      CheckUtils.checkState(
          settings.getString(KEY_SHOW_DOWNLOAD_BUTTON).equalsIgnoreCase("true")
              || settings.getString(KEY_SHOW_DOWNLOAD_BUTTON).equalsIgnoreCase("false"),
          "Only Boolean Values are allowed for show Download Button");
      CheckUtils.checkState(
          settings.getString(KEY_SHOW_DETAILS_BUTTON).equalsIgnoreCase("true")
              || settings.getString(KEY_SHOW_DETAILS_BUTTON).equalsIgnoreCase("false"),
          "Only Boolean Values are allowed for show Details Button");
      CheckUtils.checkState(
          settings.getString(KEY_SHOW_EXECUTE_BUTTON).equalsIgnoreCase("true")
              || settings.getString(KEY_SHOW_EXECUTE_BUTTON).equalsIgnoreCase("false"),
          "Only Boolean Values are allowed for show Execute Button");
      CheckUtils.checkState(
          settings.getString(KEY_SHOW_HEADER).equalsIgnoreCase("true")
              || settings.getString(KEY_SHOW_HEADER).equalsIgnoreCase("false"),
          "Only Boolean Values are allowed for show Header Button");
      boolean isSandwichListValid = false;
      try {

        new JSONArray(settings.getString(KEY_SANDWICH_LIST));
      } catch (Exception ex) {
        isSandwichListValid = true;
      }
      CheckUtils.checkState(isSandwichListValid == false, "Sandich List is not a valid JSON Array");
      m_SandwichListSettings.validateSettings(settings);
      m_TitleSettings.validateSettings(settings);
    }
    if (settings.containsKey(SELECTION))
      selectionSettings.validateSettings(settings);
  }

  @Override
  public FSKDBViewRepresentation getViewRepresentation() {
    FSKDBViewRepresentation representation;
    synchronized (getLock()) {
      representation = super.getViewRepresentation();
      if (representation == null) {
        representation = createEmptyViewRepresentation();
      }
    }
    representation
        .setSelection(selectionSettings.getStringArrayValue());
    if (representation.getTable() == null) {
      representation.setTable(emptyJSONTable);
    }
    return representation;
  }

  @Override
  public FSKDBViewRepresentation createEmptyViewRepresentation() {
    return new FSKDBViewRepresentation();

  }

  @Override
  public FSKDBViewValue createEmptyViewValue() {
    return new FSKDBViewValue();
  }

  @Override
  public FSKDBViewValue getViewValue() {
    FSKDBViewValue val;
    synchronized (getLock()) {
      val = super.getViewValue();
      if (val == null) {
        val = createEmptyViewValue();
        String connectedNodeId = getTableId(0);
        val.setTableID(connectedNodeId);
      }
    }
    return val;
  }

  @Override
  public String getJavascriptObjectID() {
    return "de.bund.bfr.knime.fsklab.v2.0.fskdbview.component";
  }

  @Override
  public boolean isHideInWizard() {
    return false;
  }

  @Override
  public void setHideInWizard(boolean hide) {}

  @Override
  public ValidationError validateViewValue(FSKDBViewValue viewContent) {
    return null;
  }

  @Override
  public void saveCurrentValue(NodeSettingsWO content) {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
      throws InvalidSettingsException {
    return inSpecs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PortObject[] performExecute(PortObject[] inObjects, ExecutionContext exec)
      throws Exception {
    PortObject inPort = inObjects[0];
    PortObject outputPort = null;

    synchronized (getLock()) {
      FSKDBViewRepresentation representation = getViewRepresentation();
      representation.setRemoteRepositoryURL(m_repositoryLocationSettings.getStringValue());
      representation.setSandwichList(m_SandwichListSettings.getStringValue());
      representation.setTitle(m_TitleSettings.getStringValue());
      representation.setMaxSelectionNumber(
          ((SettingsModelIntegerBounded) m_maxSelectionNumberSettings).getIntValue());
      representation.setShowDownloadButtonChecked(
          m_showDownloadSettings.getStringValue());
      representation.setShowDetailsButtonChecked(
          m_showDetailsSettings.getStringValue());
      representation.setShowExecuteButtonChecked(
          m_showExecuteSettings.getStringValue());
      representation.setShowHeaderButtonChecked(
          m_showHeaderSettings.getStringValue());
      representation
          .setSelection(selectionSettings.getStringArrayValue());
      representation.setToken(m_TokenSettings.getStringValue());
      FSKDBViewValue fskdbViewValue = getViewValue();
      if (fskdbViewValue != null && fskdbViewValue.getSelection() != null
          && fskdbViewValue.getSelection().length > 0) {
        selectionSettings.setStringArrayValue(fskdbViewValue.getSelection());
      }
      if (fskdbViewValue.getTable() != null) {
        outputPort = convertJSONTableToBufferedDataTable(exec);
      } else if (inPort == null && fskdbViewValue.getTable() == null) {
        // if the optional input port is not provided then
        outputPort = createEmptyTable(exec);
        emptyJSONTable =
            JSONDataTable.newBuilder().setDataTable((DataTable) outputPort).build(exec);
        representation.setTable(emptyJSONTable);
      } else if (fskdbViewValue.getTable() == null) {

        // construct a BufferedDataTable from the input object.
        BufferedDataTable table = (BufferedDataTable) inPort;
        JSONDataTable jsonTable = JSONDataTable.newBuilder().setDataTable(table).build(exec);
        representation.setTable(jsonTable);

        // set the table ID which will be used in broadcasting and receiving event in the component.
        String connectedNodeId = getTableId(0);
        representation.setTableID(connectedNodeId);

        outputPort = inPort;
      }
    }
    return new PortObject[] {outputPort};
  }

  /**
   * A helper method for convert the received JSON Table from the JS View to a BufferedDataTable
   * adding a new column for selection.
   * 
   * @param ExecutionContext exec.
   * @return a BufferedDataTable instance.
   */
  private BufferedDataTable convertJSONTableToBufferedDataTable(final ExecutionContext exec) {

    String[] colNames = new String[] {"JSON", "Selected (Table View)"};
    DataType[] colTypes = new DataType[] {StringCell.TYPE, BooleanCell.TYPE};
    DataTableSpec spec = new DataTableSpec(colNames, colTypes);
    BufferedDataContainer container = exec.createDataContainer(spec);


    List<String> selectionList = null;
    FSKDBViewValue viewValue = getViewValue();
    if (viewValue != null && viewValue.getSelection() != null) {
      selectionList = Arrays.asList(viewValue.getSelection());
    }
    for (JSONDataTableRow row : viewValue.getTable().getRows()) {
      String jsonRow = (String) row.getData()[0];
      String rowKey = row.getRowKey();
      DataCell jsonCell = new StringCell(jsonRow);
      BooleanCell booleanCell = BooleanCell.FALSE;
      if (selectionList != null && selectionList.contains(rowKey.toString())) {
        booleanCell = BooleanCell.TRUE;
      }

      DataRow convertedRow = new DefaultRow(rowKey, new DataCell[] {jsonCell, booleanCell});
      container.addRowToTable(convertedRow);
    }

    container.close();
    return container.getTable();
  }

  /**
   * A helper method for creating an empty table in the case of empty input port.
   * 
   * @param ExecutionContext exec.
   * @return an empty BufferedDataTable instance.
   */
  private static BufferedDataTable createEmptyTable(final ExecutionContext exec) {
    BufferedDataContainer container =
        exec.createDataContainer(new DataTableSpecCreator().createSpec());
    container.close();
    return container.getTable();
  }

  @Override
  protected void performReset() {
    selectionSettings.setStringArrayValue(new String[0]);
  }

  @Override
  protected void useCurrentValueAsDefault() {
    // TODO required to preset implementation.

  }
}

