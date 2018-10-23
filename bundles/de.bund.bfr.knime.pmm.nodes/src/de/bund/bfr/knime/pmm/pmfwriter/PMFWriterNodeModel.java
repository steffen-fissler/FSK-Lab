/*******************************************************************************
 * Copyright (c) 2015 Federal Institute for Risk Assessment (BfR), Germany
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
 *
 * Contributors:
 *     Department Biological Safety - BfR
 *******************************************************************************/
package de.bund.bfr.knime.pmm.pmfwriter;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import com.google.common.base.Strings;

import de.bund.bfr.knime.pmm.common.EstModelXml;
import de.bund.bfr.knime.pmm.common.PmmXmlDoc;
import de.bund.bfr.knime.pmm.common.generictablemodel.KnimeSchema;
import de.bund.bfr.knime.pmm.common.generictablemodel.KnimeTuple;
import de.bund.bfr.knime.pmm.common.pmmtablemodel.Model1Schema;
import de.bund.bfr.knime.pmm.common.pmmtablemodel.PmmUtilities;
import de.bund.bfr.knime.pmm.common.pmmtablemodel.SchemaFactory;
import de.bund.bfr.knime.pmm.common.pmmtablemodel.TimeSeriesSchema;
import de.bund.bfr.knime.pmm.common.writer.WriterUtils;
import de.bund.bfr.pmfml.ModelType;
import de.bund.bfr.pmfml.sbml.Metadata;
import de.bund.bfr.pmfml.sbml.SBMLFactory;

/**
 * Base model implementation of PMFWriter
 * 
 * @author Miguel Alba
 */
public class PMFWriterNodeModel extends NodeModel {

	private final PMFWriterNodeSettings settings = new PMFWriterNodeSettings();

	private final boolean isPmfx;

	public PMFWriterNodeModel(final boolean isPmfx) {
		super(1, 0);

		// Sets current date in the dialog components
		long currentDate = Calendar.getInstance().getTimeInMillis();
		this.settings.createdDate = currentDate;
		this.settings.modifiedDate = currentDate;

		this.isPmfx = isPmfx;
	}

	// TODO: execute
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		KnimeSchema schema = null;
		ModelType modelType = null;
		List<KnimeTuple> tuples;

		DataTableSpec spec = inData[0].getSpec();
		// Table has the structure Model1 + Model2 + Data
		if (SchemaFactory.conformsM12DataSchema(spec)) {
			schema = SchemaFactory.createM12DataSchema();
			tuples = PmmUtilities.getTuples(inData[0], schema);
			if (WriterUtils.hasData(tuples)) {
				boolean identical = identicalEstModels(tuples);
				if (settings.isSecondary) {
					modelType = identical ? ModelType.ONE_STEP_SECONDARY_MODEL : ModelType.TWO_STEP_SECONDARY_MODEL;
				} else {
					modelType = identical ? ModelType.ONE_STEP_TERTIARY_MODEL : ModelType.TWO_STEP_TERTIARY_MODEL;
				}
			} else {
				modelType = ModelType.MANUAL_TERTIARY_MODEL;
			}
		}

		// Table has Model1 + Data
		else if (SchemaFactory.conformsM1DataSchema(spec)) {
			schema = SchemaFactory.createM1DataSchema();
			tuples = PmmUtilities.getTuples(inData[0], schema);

			// Check every tuple. If any tuple has data (number of data points >
			// 0) then assigns PRIMARY_MODEL_WDATA. Otherwise it assigns
			// PRIMARY_MODEL_WODATA
			modelType = ModelType.PRIMARY_MODEL_WODATA;
			for (KnimeTuple tuple : tuples) {
				PmmXmlDoc mdData = tuple.getPmmXml(TimeSeriesSchema.ATT_TIMESERIES);
				if (mdData.size() > 0) {
					modelType = ModelType.PRIMARY_MODEL_WDATA;
					break;
				}
			}
		}

		// Table only has data
		else if (SchemaFactory.conformsDataSchema(spec)) {
			schema = SchemaFactory.createDataSchema();
			tuples = PmmUtilities.getTuples(inData[0], schema);
			modelType = ModelType.EXPERIMENTAL_DATA;
		}

		// Table only has secondary model cells
		else if (SchemaFactory.conformsM2Schema(spec)) {
			schema = SchemaFactory.createM2Schema();
			tuples = PmmUtilities.getTuples(inData[0], schema);
			modelType = ModelType.MANUAL_SECONDARY_MODEL;
		} else {
			throw new Exception();
		}

		// Retrieve info from dialog
		Metadata metadata = SBMLFactory.createMetadata();

		if (settings.creatorGivenName.isEmpty()) {
			setWarningMessage("Given name missing");
		} else {
			metadata.setGivenName(settings.creatorGivenName);
		}

		if (settings.creatorFamilyName.isEmpty()) {
			setWarningMessage("Creator family name missing");
		} else {
			metadata.setFamilyName(settings.creatorFamilyName);
		}

		if (settings.creatorContact.isEmpty()) {
			setWarningMessage("Creator contact missing");
		} else {
			metadata.setContact(settings.creatorContact);
		}

		metadata.setCreatedDate(new Date(settings.createdDate).toString());
		metadata.setModifiedDate(new Date(settings.modifiedDate).toString());

		metadata.setType(modelType);
		metadata.setRights(Strings.emptyToNull(settings.license));
		metadata.setReferenceLink(Strings.emptyToNull(settings.referenceDescriptionLink));

		String modelNotes = Strings.emptyToNull(settings.notes);

		String dir = settings.outPath;
		String mdName = settings.modelName;

		// Check for existing file -> shows warning if despite overwrite being
		// false the user still executes the nod
		String filepath = String.format("%s/%s.pmfx", dir, mdName);
		File f = new File(filepath);
		if (f.exists() && !f.isDirectory() && !settings.overwrite) {
			setWarningMessage(filepath + " was not overwritten");
			return new BufferedDataTable[] {};
		}

		WriterUtils.write(tuples, isPmfx, dir, mdName, metadata, settings.splitModels, modelNotes, exec, modelType);

		return new BufferedDataTable[] {};
	}

	@Override
	protected void reset() {
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (settings.outPath == null || settings.modelName == null) {
			throw new InvalidSettingsException("Node must be configured");
		}

		if (settings.outPath.isEmpty()) {
			throw new InvalidSettingsException("Missing outpath");
		}

		if (settings.modelName.isEmpty()) {
			throw new InvalidSettingsException("Missing model name");
		}
		return new DataTableSpec[] {};
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		this.settings.save(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.load(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		// does nothing
	}

	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	private static boolean identicalEstModels(List<KnimeTuple> tuples) {
		int id = ((EstModelXml) tuples.get(0).getPmmXml(Model1Schema.ATT_ESTMODEL).get(0)).id;
		for (KnimeTuple tuple : tuples.subList(1, tuples.size())) {
			EstModelXml estModel = (EstModelXml) tuple.getPmmXml(Model1Schema.ATT_ESTMODEL).get(0);
			if (id != estModel.id) {
				return false;
			}
		}
		return true;
	}
}