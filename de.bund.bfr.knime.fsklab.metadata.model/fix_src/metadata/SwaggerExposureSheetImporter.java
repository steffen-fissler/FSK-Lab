package metadata;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import de.bund.bfr.metadata.swagger.ExposureModelScope;
import de.bund.bfr.metadata.swagger.GenericModelDataBackground;
import de.bund.bfr.metadata.swagger.GenericModelModelMath;
import de.bund.bfr.metadata.swagger.PredictiveModelGeneralInformation;

public class SwaggerExposureSheetImporter extends SwaggerSheetImporter {
	public GenericModelDataBackground retrieveBackground(Sheet sheet) {
		SwaggerGenericSheetImporter importer = new SwaggerGenericSheetImporter();
		return importer.retrieveBackground(sheet);	
	}
	public GenericModelModelMath retrieveModelMath(Sheet sheet) {
		
		SwaggerGenericSheetImporter importer = new SwaggerGenericSheetImporter();
		return importer.retrieveModelMath(sheet);
		
	}

	public PredictiveModelGeneralInformation retrieveGeneralInformation(Sheet sheet) {
		
		SwaggerPredictiveModelSheetImporter importer = new SwaggerPredictiveModelSheetImporter();
		return importer.retrieveGeneralInformation(sheet);	
	
	}
	public ExposureModelScope retrieveScope(Sheet sheet) {
		
		SwaggerGenericSheetImporter importer = new SwaggerGenericSheetImporter();
		ExposureModelScope scope = new ExposureModelScope();

		for (int numrow = 38; numrow <= 49; numrow++) {

			Row row = sheet.getRow(numrow);

			try {
				scope.addProductItem(importer.retrieveProduct(row));
			} catch (IllegalArgumentException exception) {
				// ignore exception since products are optional (*)
			}

			try {
				scope.addHazardItem(retrieveHazard(row));
			} catch (IllegalArgumentException exception) {
				// ignore exception since products are optional (*)
			}

			try {
				scope.addPopulationGroupItem(retrievePopulationGroup(row));
			} catch (IllegalArgumentException exception) {
				// ignore exception since population groups are optional (*)
			}
		}

		Cell generalCommentCell = sheet.getRow(SCOPE__GENERAL_COMMENT).getCell(I);
		if (generalCommentCell.getCellType() == Cell.CELL_TYPE_STRING) {
			scope.setGeneralComment(generalCommentCell.getStringCellValue());
		}

		Cell temporalInformationCell = sheet.getRow(SCOPE__TEMPORAL_INFORMATION).getCell(I);
		if (temporalInformationCell.getCellType() == Cell.CELL_TYPE_STRING) {
			scope.setTemporalInformation(temporalInformationCell.getStringCellValue());
		}

		// TODO: Spatial information

		return scope;
	}

}
