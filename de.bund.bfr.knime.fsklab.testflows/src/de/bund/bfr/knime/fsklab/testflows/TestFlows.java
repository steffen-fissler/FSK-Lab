package de.bund.bfr.knime.fsklab.testflows;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.util.LockFailedException;
import org.knime.testing.core.TestrunConfiguration;

import nl.esciencecenter.e3dchem.knime.testing.TestFlowRunner;

public class TestFlows {

	@Rule
	public ErrorCollector collector = new ErrorCollector();
	private TestFlowRunner runner;

	@Before
	public void setUp() {
		TestrunConfiguration runConfiguration = new TestrunConfiguration();

		runConfiguration.setCheckLogMessages(false);
		runner = new TestFlowRunner(collector, runConfiguration);
	}

	@Test
	public void testAddressCreator() throws IOException, InvalidSettingsException, CanceledExecutionException,
			UnsupportedWorkflowVersionException, LockFailedException, InterruptedException {
		runner.runTestWorkflow(new File("workflows/FSKX-Reader-Writer-Test"));
	}
}
