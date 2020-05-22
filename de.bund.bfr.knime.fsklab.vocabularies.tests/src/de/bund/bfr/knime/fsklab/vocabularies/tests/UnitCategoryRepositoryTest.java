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

import de.bund.bfr.knime.fsklab.vocabularies.data.UnitCategoryRepository;
import de.bund.bfr.knime.fsklab.vocabularies.domain.UnitCategory;

public class UnitCategoryRepositoryTest {

	private static Connection connection;

	@BeforeClass
	public static void setUp() throws SQLException {
		DriverManager.registerDriver(new org.h2.Driver());
		connection = DriverManager.getConnection("jdbc:h2:mem:UnitCategoryRepositoryTest");
		
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE unit_category ("
				+ "id INTEGER not NULL,"
				+ "name VARCHAR(255) not NULL,"
				+ "PRIMARY KEY(id))");
		
		statement.execute("INSERT INTO unit_category VALUES(0, 'name')");
	}
	
	@AfterClass
	public static void tearDown() throws SQLException {
		connection.close();
	}
	
	@Test
	public void testGetById() throws SQLException {
		UnitCategoryRepository repository = new UnitCategoryRepository(connection);
		UnitCategory category = repository.getById(0);
		
		assertEquals(0, category.getId());
		assertEquals("name", category.getName());
	}
	
	@Test
	public void testGetAll() throws SQLException {
		UnitCategoryRepository repository = new UnitCategoryRepository(connection);
		assertTrue(repository.getAll().length > 0);
	}
}
