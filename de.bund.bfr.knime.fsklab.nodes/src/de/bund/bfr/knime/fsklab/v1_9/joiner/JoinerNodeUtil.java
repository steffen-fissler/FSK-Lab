package de.bund.bfr.knime.fsklab.v1_9.joiner;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knime.core.node.ExecutionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.bfr.knime.fsklab.nodes.NodeUtils;
import de.bund.bfr.knime.fsklab.nodes.ScriptHandler;
import de.bund.bfr.knime.fsklab.v1_9.CombinedFskPortObject;
import de.bund.bfr.knime.fsklab.v1_9.FskPortObject;
import de.bund.bfr.knime.fsklab.v1_9.FskSimulation;
import de.bund.bfr.knime.fsklab.v1_9.JoinRelation;
import de.bund.bfr.metadata.swagger.Parameter;
import metadata.SwaggerUtil;

public class JoinerNodeUtil {



  /**
   * 
   * @param originalOutputParameters map that contains the globally unique parameter names and links
   *        them to the local names of the parameters in the actual scripts (e.g. var12 = var)
   * @param suffix Can be JoinerNodeModel.SUFFIX_FIRST ("1") or SUFFIX_SECOND ("2")
   * @return mapping of original parameter names for one of the two models in a combined FSK object
   */
  public static LinkedHashMap<String, String> getOriginalParameterNames(
      LinkedHashMap<String, String> originalOutputParameters, String suffix, int depth) {

    LinkedHashMap<String, String> originalNamesMap = new LinkedHashMap<String, String>();

    // create a mapping for the output parameters so that the true (original) parameter name is
    // preserved
    // original parameter name maps to the current (local) parameter (from the script)
    for (Map.Entry<String, String> pair : originalOutputParameters.entrySet()) {
      if (pair.getValue().endsWith(suffix)) {
        originalNamesMap.put(pair.getKey(),
            pair.getValue().substring(0, pair.getValue().length() - depth));
      }
    }

    return originalNamesMap;
  }

  /**
   * 
   * @param originalOutputParameters map that contains the globally unique parameter names and links
   *        them to the local names of the parameters in the actual scripts (e.g. var12 = var)
   * @param depth represents the index of suffix, this helps to extract the parameters that belong
   *        to the join level.
   * @return mapping of original parameter names for one of the two models in a combined FSK object
   */
  public static LinkedHashMap<String, String> getOriginalParameterNameswithoutManipulation(
      LinkedHashMap<String, String> originalOutputParameters, int depth) {

    LinkedHashMap<String, String> originalNamesMap = new LinkedHashMap<String, String>();

    // create a mapping for the output parameters so that the true (original) parameter name is
    // preserved
    // original parameter name maps to the current (local) parameter (from the script)
    for (Map.Entry<String, String> pair : originalOutputParameters.entrySet()) {
      if (pair.getValue().length() > depth) {
        originalNamesMap.put(pair.getKey(), pair.getValue());
      }
    }

    return originalNamesMap;
  }

  /**
   * 
   * Create a simulation only for one model (first or second) in a combined object
   * 
   * 
   * @param combinedSim Simulation containing parameters from the first AND the second model.
   * @param suffix Can be JoinerNodeModel.SUFFIX_FIRST ("1") or SUFFIX_SECOND ("2")
   * @return simulation containing only parameters from the first or second model in a combined FSK
   *         object
   */
  public static FskSimulation makeIndividualSimulation(FskSimulation combinedSim, String suffix) {
    FskSimulation fskSimulation = new FskSimulation(combinedSim.getName());

    combinedSim.getParameters().forEach((pId, pValue) -> {
      if (pId.endsWith(suffix))
        fskSimulation.getParameters().put(pId.substring(0, pId.length() - 1), pValue);
    });

    return fskSimulation;
  }

  /**
   * This method saves the output of a model script using its globally unique parameter name so they
   * can't be overwritten by other models in the same session
   * 
   * @param originalOutputParameters map that contains the globally unique parameter names and links
   *        them to the local names of the parameters in the actual scripts (e.g. var12 = var)
   * @param handler takes care of script execution
   * @param exec KNIME execution context
   */
  public static void saveOutputVariable(Map<String, String> originalOutputParameters,
      ScriptHandler handler, ExecutionContext exec) {


    // save output to the official name (with all the suffixes) so it doesn't get overwritten by
    // subsequent model executions
    // saving is done on R evaluation level
    for (Map.Entry<String, String> pair : originalOutputParameters.entrySet()) {
      // key: output with all suffixes (globally unique name)
      // value: output name without all suffixes
      try {

        handler.runScript(pair.getKey() + "<-" + pair.getValue(), exec, false);

      } catch (Exception e) {
      }
    }
  }

  /**
   * this method prepares the model parameters and the suffix to be add to the parameters ID
   * 
   * @param portObject the combined model.
   * @param the suffix to be added.
   * @throws JsonProcessingException
   * @throws JsonMappingException
   */
  public static void addIdentifierToParametersForCombinedObject(FskPortObject portObject,
      String suffix) throws JsonMappingException, JsonProcessingException {
    if (portObject instanceof CombinedFskPortObject) {
      addIdentifierToParametersForCombinedObject(
          ((CombinedFskPortObject) portObject).getFirstFskPortObject(),
          suffix + JoinerNodeModel.SUFFIX_FIRST);
      addIdentifierToParametersForCombinedObject(
          ((CombinedFskPortObject) portObject).getSecondFskPortObject(),
          suffix + JoinerNodeModel.SUFFIX_SECOND);
      portObject.modelMetadata =
          ((CombinedFskPortObject) portObject).getSecondFskPortObject().modelMetadata;
    } else {
      addIdentifierToParameters(SwaggerUtil.getParameter(portObject.modelMetadata), suffix);
    }
  }


  /**
   * Methods adds an identifier suffix to each parameter so it can be identified after the joining.
   * The suffix is currently a string in {"1","2"}, "1" meaning the parameter is from the first
   * model, "2" meaning it is from the second model
   * 
   * 
   * @param modelParameters List of parameters from the model to be joined.
   * @param currentSuffix the suffix to be add to the parameter ID.
   */
  public static void addIdentifierToParameters(List<Parameter> modelParameters,
      String currentSuffix) {
    if (modelParameters != null)
      modelParameters.forEach(it -> it.setId(it.getId() + currentSuffix));
  }


  /**
   * This method sets the default values of a combined model. The values are taken from the
   * simulation settings of the individual models.
   * 
   * 
   * @param simulation parameters from the second model to be joined.
   * @param parameters Simulation parameters with changed values based on the first and second
   *        simulations.
   * @param index of the suffix to be cut out (length - index)
   */
  public static void createDefaultParameterValues(FskSimulation simulation,
      List<Parameter> parameters, int suffixIndex) {

    for (Parameter p : parameters) {

      String p_id = p.getId();
      String p_id_trim = p_id.substring(0, p_id.length() - suffixIndex);

      if (simulation.getParameters().containsKey(p_id_trim)) {
        p.setValue(simulation.getParameters().get(p_id_trim));
      }
    }
  }


  /**
   * This method creates a cross product of all simulations from the first and second models and
   * combines them to create simulations for the joined model.
   * 
   * @param firstFskObj First FSK object with simulations
   * @param secondFskObj Second FSK object with simulations
   * @param outObj Combined FSK object with new list of simulations from both models
   */
  public static void createAllPossibleSimulations(FskPortObject firstFskObj,
      FskPortObject secondFskObj, CombinedFskPortObject outObj) {

    int indexFirst = 0;
    int indexSecond = 0;
    for (FskSimulation simFirst : firstFskObj.simulations) {

      for (FskSimulation simSecond : secondFskObj.simulations) {

        // these simulations are already used for the default simulation of the combined model. So
        // skip it.
        if (indexFirst == firstFskObj.selectedSimulationIndex
            && indexSecond == secondFskObj.selectedSimulationIndex) {
          indexSecond++;
          continue;
        }


        List<Parameter> combinedModelParameters = SwaggerUtil.getParameter(outObj.modelMetadata);

        FskSimulation simComb = new FskSimulation(simFirst.getName() + "_" + simSecond.getName());

        // unless the parameter is output, add it to new simulation
        for (Parameter p : combinedModelParameters) {
          if (p.getClassification().equals(Parameter.ClassificationEnum.OUTPUT))
            continue;


          String p_id = p.getId();
          // for convenience remove last suffix from the parameter to make comparison easier
          String p_trim = p.getId().substring(0, p.getId().length() - 1);
          // if simulation-parameter belongs to first model, get its value and add it to the
          // combined simulation
          if (p_id.endsWith(JoinerNodeModel.SUFFIX_FIRST)) {
            simComb.getParameters().put(p_id, simFirst.getParameters().get(p_trim));
          }
          if (p_id.endsWith(JoinerNodeModel.SUFFIX_SECOND)) {
            simComb.getParameters().put(p_id, simSecond.getParameters().get(p_trim));
          }
        }

        // add new simulation to combined model
        outObj.simulations.add(simComb);
        indexSecond++;
      }
      indexSecond = 0;
      indexFirst++;

    }

  }



  /**
   * Create a default simulation for an FSKPortObject
   * 
   * @param fskObj The FSK Port Object
   */
  public static void createDefaultSimulation(FskPortObject fskObj) {

    if (SwaggerUtil.getModelMath(fskObj.modelMetadata) != null) {

      List<Parameter> combinedModelParameters = SwaggerUtil.getParameter(fskObj.modelMetadata);

      FskSimulation defaultSimulation = NodeUtils.createDefaultSimulation(combinedModelParameters);

      fskObj.simulations.add(defaultSimulation);
      fskObj.selectedSimulationIndex = 0;
    }
  }

  /**
   * This method removes INPUT parameters that are the target for a join command.
   * 
   * @param relations The join relation between a source parameter (output of model 1) and target
   *        (input of model 2)
   * @param outfskPort The combined FSK PortObject with its List of parameters from the metadata
   */
  public static void removeJoinedParameters(JoinRelation[] relations, FskPortObject outfskPort) {
    if (outfskPort instanceof CombinedFskPortObject) {
      if (relations != null)
        for (JoinRelation relation : relations) {

          Iterator<Parameter> iter = SwaggerUtil.getParameter(outfskPort.modelMetadata).iterator();
          while (iter.hasNext()) {
            Parameter p = iter.next();

            // remove input from second model
            Boolean b2 = p.getId().equals(relation.getTargetParam());
            if (b2)
              iter.remove();

          } // while
        } // for
      removeJoinedParameters(relations,
          ((CombinedFskPortObject) outfskPort).getFirstFskPortObject());
      removeJoinedParameters(relations,
          ((CombinedFskPortObject) outfskPort).getSecondFskPortObject());
    }
  }// resolveParameters

  /**
   * 
   * @param firstParameterList List of parameters from model 1
   * @param secondParameterList List of parameters from model 2
   * @return List of parameters from both models
   */
  public static List<Parameter> combineParameters(List<Parameter> firstParameterList,
      List<Parameter> secondParameterList) {

    // parameters
    List<Parameter> combinedList = Stream
        .of(Optional.ofNullable(firstParameterList).orElse(Collections.emptyList()),
            Optional.ofNullable(secondParameterList).orElse(Collections.emptyList()))
        .flatMap(x -> x.stream()).collect(Collectors.toList());

    return combinedList;
  }



}// JoinerNodeUtil
