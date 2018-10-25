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
package de.bund.bfr.knime.pmm.common;

import org.jdom2.Element;

public class IndepXml implements PmmXmlElementConvertable {

	public static final String ELEMENT_INDEP = "indep";

	private static final String ATT_NAME = "name";
	private static final String ATT_ORIGNAME = "origname";
	private static final String ATT_MIN = "min";
	private static final String ATT_MAX = "max";
	private static final String ATT_CATEGORY = "category";
	private static final String ATT_UNIT = "unit";
	private static final String ATT_DESCRIPTION = "description";

	public String name;
	public String origName;
	public Double min;
	public Double max;
	public String category;
	public String unit;
	public String description;

	/**
	 * Constructor with name, min and max.
	 * <ul>
	 * <li>origName is assigned name.
	 * <li>category, unit and description are null.
	 * </ul>
	 */
	public IndepXml(String name, Double min, Double max) {
		this(name, min, max, null, null);
	}

	/**
	 * Constructor with name, min, max, category and unit.
	 * <ul>
	 * <li>origName is assigned name
	 * <li>description is null
	 * </ul>
	 */
	public IndepXml(String name, Double min, Double max, String category, String unit) {
		this(name, name, min, max, category, unit, null);
	}

	/** Fully parameterized constructor. */
	public IndepXml(String name, String origName, Double min, Double max, String category, String unit,
			String description) {
		this.name = name;
		this.origName = origName;
		this.min = min;
		this.max = max;
		this.category = category;
		this.unit = unit;
		this.description = description;
	}

	/**
	 * Copy constructor. Take every property from a {@link org.jdom2.Element} with
	 * properties:
	 * <ul>
	 * <li>String "name"
	 * <li>String "origName"
	 * <li>Double "min"
	 * <li>Double "max"
	 * <li>String "category"
	 * <li>String "unit"
	 * <li>String "description"
	 * </ul>
	 */
	public IndepXml(Element el) {
		this(XmlHelper.getString(el, ATT_NAME), XmlHelper.getString(el, ATT_ORIGNAME), XmlHelper.getDouble(el, ATT_MIN),
				XmlHelper.getDouble(el, ATT_MAX), XmlHelper.getString(el, ATT_CATEGORY),
				XmlHelper.getString(el, ATT_UNIT), XmlHelper.getString(el, ATT_DESCRIPTION));
	}

	/**
	 * Generate a {@link org.jdom2.Element with name "indep" and properties:
	 * <ul>
	 * <li>String "name"
	 * <li>String "origName"
	 * <li>Double "min"
	 * <li>Double "max"
	 * <li>String "category"
	 * <li>String "unit"
	 * <li>String "description"
	 * </ul>
	 */
	@Override
	public Element toXmlElement() {
		Element ret = new Element(ELEMENT_INDEP);

		ret.setAttribute(ATT_NAME, XmlHelper.getNonNull(name));
		ret.setAttribute(ATT_ORIGNAME, XmlHelper.getNonNull(origName));
		ret.setAttribute(ATT_MIN, XmlHelper.getNonNull(min));
		ret.setAttribute(ATT_MAX, XmlHelper.getNonNull(max));
		ret.setAttribute(ATT_CATEGORY, XmlHelper.getNonNull(category));
		ret.setAttribute(ATT_UNIT, XmlHelper.getNonNull(unit));
		ret.setAttribute(ATT_DESCRIPTION, XmlHelper.getNonNull(description));

		return ret;
	}
}
