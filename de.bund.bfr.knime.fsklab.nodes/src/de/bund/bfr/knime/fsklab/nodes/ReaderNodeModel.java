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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Text;
import org.jlibsedml.ChangeAttribute;
import org.jlibsedml.Libsedml;
import org.jlibsedml.SEDMLTags;
import org.jlibsedml.SedML;
import org.jlibsedml.XMLException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NoInternalsModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.ReplacedBy;
import org.sbml.jsbml.xml.XMLNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.bfr.fskml.FSKML;
import de.bund.bfr.fskml.FskMetaDataObject;
import de.bund.bfr.fskml.FskMetaDataObject.ResourceType;
import de.bund.bfr.fskml.RScript;
import de.bund.bfr.knime.fsklab.CombinedFskPortObject;
import de.bund.bfr.knime.fsklab.FskPlugin;
import de.bund.bfr.knime.fsklab.FskPortObject;
import de.bund.bfr.knime.fsklab.FskPortObjectSpec;
import de.bund.bfr.knime.fsklab.FskSimulation;
import de.bund.bfr.knime.fsklab.JoinRelation;
import de.bund.bfr.knime.fsklab.rakip.RakipUtil;
import de.bund.bfr.metadata.swagger.GenericModel;
import de.bund.bfr.metadata.swagger.GenericModelDataBackground;
import de.bund.bfr.metadata.swagger.GenericModelGeneralInformation;
import de.bund.bfr.metadata.swagger.GenericModelModelMath;
import de.bund.bfr.metadata.swagger.GenericModelScope;
import de.bund.bfr.metadata.swagger.Model;
import de.bund.bfr.metadata.swagger.Parameter;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import metadata.SwaggerUtil;


class ReaderNodeModel extends NoInternalsModel {

  private static final PortType[] IN_TYPES = {};
  private static final PortType[] OUT_TYPES = {FskPortObject.TYPE};

  private final ReaderNodeSettings nodeSettings = new ReaderNodeSettings();

  public ReaderNodeModel() {
    super(IN_TYPES, OUT_TYPES);
  }

  @Override
  protected void saveSettingsTo(NodeSettingsWO settings) {
    nodeSettings.save(settings);
  }

  @Override
  protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
      throws InvalidSettingsException {
    nodeSettings.load(settings);
  }

  @Override
  protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
    CheckUtils.checkDestinationFile(settings.getString("filename"), true);
  }

  @Override
  protected void reset() {
    NodeContext nodeContext = NodeContext.getContext();
    WorkflowManager wfm = nodeContext.getWorkflowManager();
    WorkflowContext workflowContext = wfm.getContext();
    /*
     * find and delete only the working directory folder related to current reader node in the mean
     * that, we are not deleting folders which are representing the working directory of other
     * reader nodes which maybe exist in the same workflow
     */

    try {
      Files.walk(workflowContext.getCurrentLocation().toPath())
          .filter(path -> path.toString()
              .contains(nodeContext.getNodeContainer().getNameWithID().toString()
                  .replaceAll("\\W", "").replace(" ", "") + "_" + "workingDirectory"))
          .sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
            try {
              file.delete();
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    return new PortObjectSpec[] {FskPortObjectSpec.INSTANCE};
  }

  @Override
  protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {

    URL url = FileUtil.toURL(nodeSettings.filePath);
    Path localPath = FileUtil.resolveToPath(url);

    FskPortObject inObject;

    if (localPath != null) {
      inObject = readArchive(localPath.toFile());
    }
    // if path is an external URL the archive is downloaded to a temporary file
    else {
      File temporaryFile = FileUtil.createTempFile("model", "fskx");
      temporaryFile.delete();

      try (
          InputStream inStream =
              FileUtil.openStreamWithTimeout(new URL(nodeSettings.filePath), 10000);
          OutputStream outStream = new FileOutputStream(temporaryFile)) {
        IOUtils.copy(inStream, outStream);
      }

      inObject = readArchive(temporaryFile);

      temporaryFile.delete();
    }

    return new PortObject[] {inObject};
  }

  private FskPortObject readArchive(File in) throws Exception {
    FskPortObject fskObj = null;

    try (final CombineArchive archive = new CombineArchive(in)) {

      // 1. Get SBML URI
      URI sbmlURI = FSKML.getURIS(1, 0, 12).get("sbml");

      // 2. Get number of SBML documents
      final int numberOfSBML = archive.getNumEntriesWithFormat(sbmlURI);

      // 3. Get the paths or model folders. A model folder contains a model and is the root
      // folder in case of a single model or can be nested folders in case of joined models.
      // If there is only one SBML document the archive contains a single model and several
      // SBML documents means a joined model.
      List<String> modelFolders;

      if (numberOfSBML > 1) {
        // Get directories inside the archive without duplication. The directories are
        // sorted to have the related directories after each other.
        // E.g.:
        // /SimpleModel2/SimpleModel2 <- Joined model
        // /SimpleModel2/SimpleModel2/SimpleModel1 <- Child model 1
        // /SimpleModel2/SimpleModel1/SimpleModel2 <- Child model 2
        TreeSet<String> entries = archive.getEntries().parallelStream()
            .map(ArchiveEntry::getFilePath)
            .map(fullPath -> fullPath.substring(0, fullPath.lastIndexOf("/") + 1))
            .filter(
                path -> StringUtils.countMatches(path, "/") > 2 && !path.endsWith("simulations/"))
            .collect(Collectors.toCollection(TreeSet::new));

        modelFolders = new ArrayList<>(entries);
      } else {
        modelFolders = Arrays.asList("/");
      }

      // 4. Create working directory for the model read inside the workflow folder.
      // E.g.: ..\InitializeParentsAnimals\FSKXReader021_workingDirectory
      // where ..\InitializeParentsAnimals is the relative path to the workflow.
      // FSKXReader021 is the name with id of the node container after removing whitespaces.
      NodeContext nodeContext = NodeContext.getContext();
      WorkflowContext workflowContext = nodeContext.getWorkflowManager().getContext();
      File workingDirectory = new File(workflowContext.getCurrentLocation(),
          nodeContext.getNodeContainer().getNameWithID().toString().replaceAll("\\W", "")
              .replace(" ", "") + "_workingDirectory");

      fskObj = readFskPortObject(archive, modelFolders, 0, workingDirectory);
    }

    return fskObj;
  }

  private FskPortObject getEmbedSecondFSKObject(CombinedFskPortObject comFskObj) {
    FskPortObject embedFSKObject = comFskObj.getSecondFskPortObject();
    if (embedFSKObject instanceof CombinedFskPortObject) {
      embedFSKObject = getEmbedSecondFSKObject((CombinedFskPortObject) embedFSKObject);
    }
    return embedFSKObject;
  }

  private FskPortObject readFskPortObject(CombineArchive archive, List<String> ListOfPaths,
      int readLevel, File currentWorkingDirectory) throws Exception {
    Map<String, URI> URIS = FSKML.getURIS(1, 0, 12);
    // each sub Model has it's own working directory to avoid resource conflict.
    // get current node's and workflow's context


    // get the location of the current Workflow to create the working directory in it and use the
    // name with current reader node id for the prefix of the working directory name

    if (!currentWorkingDirectory.exists()) {
      currentWorkingDirectory.mkdir();
    }

    final Path workingDirectory = currentWorkingDirectory.toPath();

    Model model = new Model();


    // more one than one element means this model is joined one
    if (ListOfPaths != null && ListOfPaths.size() > 1) {
      String firstelement = ListOfPaths.get(ListOfPaths.size() % 2);
      // classify the pathes into two groups, each belongs to sub model
      List<String> firstGroup = ListOfPaths.stream().filter(line -> line.startsWith(firstelement))
          .collect(Collectors.toList());
      List<String> secondGroup = ListOfPaths.stream().filter(line -> !firstGroup.contains(line))
          .collect(Collectors.toList());
      if (secondGroup.size() == 2) {
        secondGroup.remove(0);
      }

      // invoke this mothod recursively to get the sub model using the corresponding path group
      FskPortObject firstFskPortObject = readFskPortObject(archive, firstGroup, ++readLevel,
          new File(currentWorkingDirectory, "sub" + readLevel));
      FskPortObject secondFskPortObject = readFskPortObject(archive, secondGroup, ++readLevel,
          new File(currentWorkingDirectory, "sub" + readLevel));
      String tempString = firstelement.substring(0, firstelement.length() - 2);
      String parentPath = tempString.substring(0, tempString.lastIndexOf('/'));

      // Gets metadata
      {
        // Find metadata entry
        Optional<ArchiveEntry> metadataEntry = archive.getEntriesWithFormat(URIS.get("json"))
            .stream().filter(entry -> entry.getEntityPath().startsWith(parentPath))
            .filter(entry -> StringUtils.countMatches(entry.getEntityPath(),
                "/") == StringUtils.countMatches(parentPath, "/") + 1)
            .filter(entry -> entry.getEntityPath().endsWith("metaData.json")).findAny();

        if (metadataEntry.isPresent()) {
          model = readMetadata(metadataEntry.get());
        }
      }

      List<JoinRelation> connectionList = new ArrayList<>();

      // Find SBML entry in archive
      Optional<ArchiveEntry> sbmlEntry =
          archive.getEntriesWithFormat(URIS.get("sbml")).stream().filter(entry -> {
            final String path = entry.getEntityPath();
            return path.startsWith(parentPath) && StringUtils.countMatches(path,
                "/") == StringUtils.countMatches(parentPath, "/") + 1;
          }).findAny();

      String firstModelId = "";
      if (sbmlEntry.isPresent()) {

        // Extract entry to temporary file
        File temporaryFile = File.createTempFile("model", ".sbml");
        sbmlEntry.get().extractFile(temporaryFile);
        SBMLDocument sbmlDocument = SBMLReader.read(temporaryFile);
        temporaryFile.delete();

        // Get first model's id
        CompModelPlugin compModelPlugin =
            (CompModelPlugin) sbmlDocument.getModel().getExtension("comp");
        firstModelId =
            compModelPlugin.getNumSubmodels() > 0 ? compModelPlugin.getSubmodel(0).getModelRef()
                : "";

        for (org.sbml.jsbml.Parameter parameter : sbmlDocument.getModel().getListOfParameters()) {

          // Find metadata of target parameter. Connected parameter in 2nd model
          final String parameterId = parameter.getId().replaceAll(JoinerNodeModel.SUFFIX_SECOND, "");
          Optional<Parameter> targetParameter = SwaggerUtil
              .getParameter(secondFskPortObject.modelMetadata).stream().filter(currentParameter -> {
                final String currentParameterId =
                    currentParameter.getId().replaceAll(JoinerNodeModel.SUFFIX_SECOND, "");
                if (parameterId.equals(currentParameterId)) {
                  currentParameter.setId(parameter.getId());
                  return true;
                } else {
                  return false;
                }
              }).findAny();

          // If the metadata of targetParamter is not found, skip to next parameter
          if (!targetParameter.isPresent()) {
            continue;
          }

          // Find metadata of source parameter (connected parameter of 1st model)
          final ReplacedBy replacedBy =
              ((CompSBasePlugin) parameter.getExtension("comp")).getReplacedBy();
          final String replacement = replacedBy.getIdRef().replaceAll(JoinerNodeModel.SUFFIX_FIRST, "");
          Optional<Parameter> sourceParameter = SwaggerUtil
              .getParameter(firstFskPortObject.modelMetadata).stream().filter(currentParameter -> {
                final String currentParameterId =
                    currentParameter.getId().replaceAll(JoinerNodeModel.SUFFIX_FIRST, "");
                if (replacement.equals(currentParameterId)) {
                  currentParameter.setId(replacedBy.getIdRef());
                  return true;
                } else {
                  return false;
                }
              }).findAny();

          // If the metadata of sourceParmeter is not found, skip to next parameter
          if (!sourceParameter.isPresent()) {
            continue;
          }

          String command = null;
          if (parameter.getAnnotation() != null
              && parameter.getAnnotation().getNonRDFannotation() != null) {
            XMLNode nonRDFannotation = parameter.getAnnotation().getNonRDFannotation();
            XMLNode commandNode = nonRDFannotation.getChildElement("command", "");
            if (commandNode != null
                && commandNode.hasAttr(WriterNodeModel.METADATA_COMMAND_VALUE)) {
              command = commandNode.getAttrValue(WriterNodeModel.METADATA_COMMAND_VALUE);
            }
          }

          connectionList.add(new JoinRelation(sourceParameter.get().getId(),
              targetParameter.get().getId(), command, null));
        }
      }

      CombinedFskPortObject topfskObj;
      String currentModelID =
          SwaggerUtil.getModelName(firstFskPortObject.modelMetadata).replaceAll("\\W", "");
      if (currentModelID.equals(firstModelId)) {
        topfskObj = new CombinedFskPortObject("", "", model, workingDirectory.toString(),
            new ArrayList<>(), firstFskPortObject, secondFskPortObject);
      } else {
        topfskObj = new CombinedFskPortObject("", "", model, workingDirectory.toString(),
            new ArrayList<>(), secondFskPortObject, firstFskPortObject);
      }

      topfskObj.viz = getEmbedSecondFSKObject(topfskObj).viz;

      // Get select simulation index and simulations for joined model
      Optional<ArchiveEntry> simulationsEntry = archive.getEntriesWithFormat(URIS.get("sedml"))
          .stream().filter(entry -> entry.getEntityPath().startsWith(parentPath))
          .filter(entry -> StringUtils.countMatches(entry.getEntityPath(),
              "/") == StringUtils.countMatches(parentPath, "/") + 1)
          .findAny();
      if (simulationsEntry.isPresent()) {
        SimulationSettings simulationSettings = readSimulationSettings(simulationsEntry.get());
        topfskObj.selectedSimulationIndex = simulationSettings.selectedSimulationIndex;
        topfskObj.simulations.addAll(simulationSettings.simulations);
      }

      topfskObj.setJoinerRelation(connectionList.toArray(new JoinRelation[connectionList.size()]));
      
      return topfskObj;
    } else {
      String modelScript = "";
      String visualizationScript = "";
      File workspace = null; // null if missing
      String pathToResource = ListOfPaths.get(0);
      String readme = "";

      for (final ArchiveEntry entry : archive.getEntriesWithFormat(URIS.get("r"))) {
        String path = entry.getEntityPath();
        if (path.indexOf(pathToResource) == 0) {
          List<MetaDataObject> descriptions = entry.getDescriptions();

          if (descriptions.size() > 0) {
            final FskMetaDataObject fmdo = new FskMetaDataObject(descriptions.get(0));
            final ResourceType resourceType = fmdo.getResourceType();

            if (resourceType.equals(ResourceType.modelScript)) {
              modelScript = loadTextEntry(entry);
            } else if (resourceType.equals(ResourceType.visualizationScript)) {
              visualizationScript = loadTextEntry(entry);
            } else if (resourceType.equals(ResourceType.workspace)) {
              workspace = FileUtil.createTempFile("workspace", ".r");
              entry.extractFile(workspace);
            }
          }
        }
      }

      // Read readme
      URI textUri = URI.create("http://purl.org/NET/mediatypes/text-xplain");
      Optional<ArchiveEntry> readmeEntry = archive.getEntriesWithFormat(textUri).stream()
          .filter(entry -> entry.getDescriptions().size() > 0).findAny();
      if (readmeEntry.isPresent()) {
        readme = loadTextEntry(readmeEntry.get());
      }

      // Extract resources
      Set<ArchiveEntry> resourceEntries = new HashSet<>();
      archive.getEntriesWithFormat(textUri).stream()
          .filter(entry -> entry.getDescriptions().size() == 0).forEach(resourceEntries::add);
      resourceEntries.addAll(archive.getEntriesWithFormat(URIS.get("csv")));
      resourceEntries.addAll(archive.getEntriesWithFormat(URIS.get("rdata")));

      for (final ArchiveEntry entry : resourceEntries) {
        String path = entry.getEntityPath();
        if (path.indexOf(pathToResource) == 0) {
          Path targetPath = workingDirectory.resolve(entry.getFileName());

          try {
            Files.createFile(targetPath);
            entry.extractFile(targetPath.toFile());
          } catch (FileAlreadyExistsException e) {
            // Do nothing, the resource is already there
          }
        }
      }

      // Gets metadata
      {
        // Find metadata entry
        Optional<ArchiveEntry> metadataEntry = archive.getEntriesWithFormat(URIS.get("json"))
            .stream().filter(entry -> entry.getEntityPath().indexOf(pathToResource) == 0)
            .filter(entry -> entry.getEntityPath().endsWith("metaData.json")).findAny();

        if (metadataEntry.isPresent()) {
          model = readMetadata(metadataEntry.get());
        }
      }

      // Get metadata spreadsheet
      String spreadsheetPath = "";
      Optional<ArchiveEntry> excelEntry = archive.getEntriesWithFormat(URIS.get("xlsx")).stream()
          .filter(entry -> entry.getEntityPath().indexOf(pathToResource) == 0).findAny();
      if (excelEntry.isPresent()) {
        File tempFile = FileUtil.createTempFile("metadata", ".xlsx");
        excelEntry.get().extractFile(tempFile);
        spreadsheetPath = tempFile.getAbsolutePath();
      }

      // Retrieve missing libraries from CRAN
      HashSet<String> packagesSet = new HashSet<>();
      if (!modelScript.isEmpty()) {
        packagesSet.addAll(new RScript(modelScript).getLibraries());
      }
      if (!visualizationScript.isEmpty()) {
        packagesSet.addAll(new RScript(visualizationScript).getLibraries());
      }
      List<String> packagesList = new ArrayList<>(packagesSet);

      Path workspacePath = workspace == null ? null : workspace.toPath();

      // The reader node is not using currently the plot, if present. Therefore an
      // empty string is used.
      String plotPath = "";

      FskPortObject fskObj =
          new FskPortObject(modelScript, visualizationScript, model, workspacePath, packagesList,
              workingDirectory.toString(), plotPath, readme, spreadsheetPath);

      // Read selected simulation index and simulations
      Optional<ArchiveEntry> simulationsEntry = archive.getEntriesWithFormat(URIS.get("sedml"))
          .stream().filter(entry -> entry.getEntityPath().indexOf(pathToResource) == 0).findAny();
      if (simulationsEntry.isPresent()) {
        SimulationSettings simulationSettings = readSimulationSettings(simulationsEntry.get());
        fskObj.selectedSimulationIndex = simulationSettings.selectedSimulationIndex;
        fskObj.simulations.addAll(simulationSettings.simulations);
      }

      return fskObj;
    }
  }

  /** @return text content out of an {@link ArchiveEntry}. */
  private static String loadTextEntry(final ArchiveEntry entry) throws IOException {

    // Create temporary file with script
    File temp = File.createTempFile("temp", null);
    String contents;

    try {
      // extractFile throws IOException if the file does not exist (was deleted manually) or is
      // not writable.
      entry.extractFile(temp);

      // readFileToString throws IOException if the file was deleted manually
      contents = FileUtils.readFileToString(temp, "UTF-8");
    } catch (IOException exception) {
      throw exception;
    } finally {
      temp.delete();
    }

    return contents;
  }

  private Model readMetadata(ArchiveEntry metadataEntry)
      throws JsonProcessingException, IOException {

    // Create temporary file with metadata
    File temp = File.createTempFile("metadata", ".json");
    metadataEntry.extractFile(temp);

    // Load metadata from temporary file
    final ObjectMapper mapper = FskPlugin.getDefault().MAPPER104;
    JsonNode jsonNode = mapper.readTree(temp);

    temp.delete(); // Delete temporary file

    Model model;

    // New swagger models have the modelType property (1.0.4)
    if (jsonNode.has("modelType")) {
      String modelType = jsonNode.get("modelType").asText();
      Class<? extends Model> modelClass = FskPortObject.Serializer.modelClasses.get(modelType);
      model = mapper.treeToValue(jsonNode, modelClass);
    } else if (jsonNode.has("version")) {
      // 1.0.3 (with EMF)
      GenericModel gm = new GenericModel();
      gm.setModelType("genericModel");
      gm.setGeneralInformation(mapper.treeToValue(jsonNode.get("generalInformation"), GenericModelGeneralInformation.class));
      gm.setScope(mapper.treeToValue(jsonNode.get("scope"), GenericModelScope.class));
      gm.setDataBackground(mapper.treeToValue(jsonNode.get("dataBackground"), GenericModelDataBackground.class));
      gm.setModelMath(mapper.treeToValue(jsonNode.get("modelMath"), GenericModelModelMath.class));
      
      model = gm;
    } else {
      // Pre-RAKIP
      de.bund.bfr.knime.fsklab.rakip.GenericModel rakipModel =
          mapper.treeToValue(jsonNode, de.bund.bfr.knime.fsklab.rakip.GenericModel.class);

      GenericModel gm = new GenericModel();
      gm.setModelType("genericModel");
      gm.setGeneralInformation(RakipUtil.convert(rakipModel.generalInformation));
      gm.setScope(RakipUtil.convert(rakipModel.scope));
      gm.dataBackground(RakipUtil.convert(rakipModel.dataBackground));
      gm.modelMath(RakipUtil.convert(rakipModel.modelMath));
      model = gm;
    }

    return model;
  }

  private static class SimulationSettings {
    final int selectedSimulationIndex;
    final List<FskSimulation> simulations;

    SimulationSettings(final int selectedSimulationIndex, final List<FskSimulation> simulations) {
      this.selectedSimulationIndex = selectedSimulationIndex;
      this.simulations = simulations;
    }
  }

  /**
   * @return SimulationSettings with selected simulation index and list of simulations.
   * 
   *         <p>
   *         In SedML every simulation is encoded as a {@link org.jlibsedml.Model} with the
   *         parameter values defined as a {@link org.jlibsedml.ChangeAttribute}.
   *         </p>
   * 
   *         <pre>
   * {@code
   * <model id="simulation1">
   *   <listOfChanges>
   *     <changeAttribute newValue="1" target="a" />
   *     <changeAttribute newValue="2" target="b" />
   *   </listOfChanges>
   * </model>
   * <model id="simulation2">
   *   <listOfChanges>
   *     <changeAttribute newValue="3" target="c" />
   *     <changeAttribute newValue="4" target="d" />
   *   </listOfChanges>
   * </model> 
   * }
   *         </pre>
   */
  private SimulationSettings readSimulationSettings(ArchiveEntry simulationsEntry)
      throws IOException, XMLException {

    // Create temporary file for extracting SEDML and read it.
    File tempFile = File.createTempFile("simulations", ".sedml");
    simulationsEntry.extractFile(tempFile);

    // Read SEDML and delete temporary file
    SedML sedml = Libsedml.readDocument(tempFile).getSedMLModel();
    tempFile.delete();

    // Read selected simulation
    int selectedSimulationIndex = 0;
    final List<org.jlibsedml.Annotation> annotations = sedml.getAnnotation();
    if (annotations != null && annotations.size() > 0) {
      org.jlibsedml.Annotation indexAnnotation = annotations.get(0);
      Text indexAnnotationText = (Text) indexAnnotation.getAnnotationElement().getContent().get(0);
      selectedSimulationIndex = Integer.parseInt(indexAnnotationText.getText());
    }

    // Read simulations
    List<FskSimulation> simulations = new ArrayList<>(sedml.getModels().size());
    for (org.jlibsedml.Model model : sedml.getModels()) {

      Map<String, String> params = model.getListOfChanges().stream()
          .filter(change -> change.getChangeKind().equals(SEDMLTags.CHANGE_ATTRIBUTE_KIND))
          .map(change -> (ChangeAttribute) change).collect(Collectors
              .toMap(change -> change.getTargetXPath().toString(), ChangeAttribute::getNewValue));

      FskSimulation sim = new FskSimulation(model.getId());
      sim.getParameters().putAll(params);

      simulations.add(sim);
    }

    return new SimulationSettings(selectedSimulationIndex, simulations);
  }
}
