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

import de.bund.bfr.knime.fsklab.vocabularies.data.PublicationStatusRepository;
import de.bund.bfr.knime.fsklab.vocabularies.domain.PublicationStatus;

public class PublicationStatusRepositoryTest {

	private static Connection connection;

	@BeforeClass
	public static void setUp() throws SQLException {
		DriverManager.registerDriver(new org.h2.Driver());
		connection = DriverManager.getConnection("jdbc:h2:mem:PublicationStatusRepositoryTest");
		
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE publication_status("
				+ "id INTEGER not NULL,"
				+ "name VARCHAR(50) not NULL,"
				+ "comment VARCHAR(255),"
				+ "PRIMARY KEY (id))");
		
		statement.execute("INSERT INTO publication_status VALUES(0, 'name', 'comment')");
	}
	
	@AfterClass
	public static void tearDown() throws SQLException {
		connection.close();
	}

	@Test
	public void testGetById() throws Exception {
		PublicationStatusRepository repository = new PublicationStatusRepository(connection);
		PublicationStatus publicationStatus = repository.getById(0);
		
		assertEquals(0, publicationStatus.getId());
		assertEquals("name", publicationStatus.getName());
		assertEquals("comment", publicationStatus.getComment());
	}
	
	@Test
	public void testGetAll() throws SQLException {
		PublicationStatusRepository repository = new PublicationStatusRepository(connection);
		assertTrue(repository.getAll().length > 0);
	}
}
