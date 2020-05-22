package de.bund.bfr.knime.fsklab.vocabularies.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.bund.bfr.knime.fsklab.vocabularies.data.ProductMatrixRepository;
import de.bund.bfr.knime.fsklab.vocabularies.domain.ProductMatrix;

public class ProductMatrixRepositoryTest {

	private static Connection connection;

	@BeforeClass
	public static void setUp() throws SQLException {
		DriverManager.registerDriver(new org.h2.Driver());
		connection = DriverManager.getConnection("jdbc:h2:mem:AvailabilityRepositoryTest");
		
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE product_matrix ("
				+ "id INTEGER not NULL,"
				+ "ssd CHAR(20) not NULL,"
				+ "name VARCHAR(250),"
				+ "comment VARCHAR(255),"
				+ "PRIMARY KEY(id))");
		
		statement.execute("INSERT INTO product_matrix VALUES(0, 'ssd', 'name', 'comment')");
	}
	
	@AfterClass
	public static void tearDown() throws SQLException {
		connection.close();
	}
	
	@Test
	public void testGetById() throws Exception {
		ProductMatrixRepository repository = new ProductMatrixRepository(connection);
		ProductMatrix matrix = repository.getById(0);
		
		assertEquals(0, matrix.getId());
		assertEquals("ssd", matrix.getSsd());
		assertEquals("name", matrix.getName());
		assertEquals("comment", matrix.getComment());
	}
	
	@Test
	public void testGetAll() throws Exception {
		ProductMatrixRepository repository = new ProductMatrixRepository(connection);
		assertTrue(repository.getAll().length > 0);
	}
}
