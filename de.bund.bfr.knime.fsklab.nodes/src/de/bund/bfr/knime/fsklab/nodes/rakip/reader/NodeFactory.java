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
package de.bund.bfr.knime.fsklab.nodes.rakip.reader;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class NodeFactory extends org.knime.core.node.NodeFactory<NodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeModel createNodeModel() {
		return new NodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.knime.core.node.NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.knime.core.node.NodeDialogPane createNodeDialogPane() {

		SettingsModelString filename = new SettingsModelString(NodeModel.CFGKEY_FILE, "");
		DialogComponentFileChooser filechooser = new DialogComponentFileChooser(filename, "filename-history",
				JFileChooser.OPEN_DIALOG, ".fskx");

		// Add widget
		DefaultNodeSettingsPane pane = new DefaultNodeSettingsPane();
		pane.createNewGroup("Data source");
		pane.addDialogComponent(filechooser);

		return pane;
	}
}
