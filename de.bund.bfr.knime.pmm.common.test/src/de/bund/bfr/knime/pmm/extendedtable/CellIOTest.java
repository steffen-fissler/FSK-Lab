package de.bund.bfr.knime.pmm.extendedtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCellFactory;

import de.bund.bfr.knime.pmm.common.PmmXmlDoc;

public class CellIOTest {
	
	@Test
	public void testGetString() {
		assertNull(CellIO.getString(DataType.getMissingCell()));
		assertNull(CellIO.getString(new StringCell("")));
		assertEquals("Hello world", CellIO.getString(new StringCell("Hello world")));
	}
	
	@Test
	public void testGetInt() {
		assertNull(CellIO.getInt(DataType.getMissingCell()));
		assertEquals(7, CellIO.getInt(new IntCell(7)).intValue());
	}
	
	@Test
	public void testGetPmmXml() throws Exception {
		PmmXmlDoc doc0 = CellIO.getPmmXml(DataType.getMissingCell());
		assertTrue(doc0 instanceof PmmXmlDoc && doc0.size() == 0);
		
		DataCell cell = XMLCellFactory.create("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<note>\r\n" + 
				"  <to>Tove</to>\r\n" + 
				"  <from>Jani</from>\r\n" + 
				"  <heading>Reminder</heading>\r\n" + 
				"  <body>Don't forget me this weekend!</body>\r\n" + 
				"</note>");
		PmmXmlDoc doc1 = CellIO.getPmmXml(cell);
		assertTrue(doc1 instanceof PmmXmlDoc);
	}
	
	@Test
	public void testGetDouble() {
		assertNull(CellIO.getDouble(DataType.getMissingCell()));
		assertEquals(5.0, CellIO.getDouble(new DoubleCell(5.0)).doubleValue(), .0);
	}
}
