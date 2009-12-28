/* *********************************************************************** *
 * project: org.matsim.*
 * PlainPopulationBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.plans.plain;

import org.matsim.api.core.v01.Id;

/**
 * @author illenberger
 *
 */
public interface PlainPopulationBuilder {

	public PlainPopulation createPopulation();
	
	public PlainPerson createPerson(Id id);
	
	public PlainPlan createPlan();
	
	public PlainActivity createActivity();
	
	public PlainLeg createLeg();
	
	public PlainRoute createRoute();
	
}
