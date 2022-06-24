/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp.lspReplanning;

import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.ShipmentAssigner;
import lsp.replanning.LSPPlanStrategyManagerFactory;

/*package-private*/ class TomorrowShipmentAssignerStrategyManagerFactoryImpl implements LSPPlanStrategyManagerFactory {

	@Override
	public GenericStrategyManager<LSPPlan, LSP> createStrategyManager(LSP lsp) {
		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManager<>();
		ShipmentAssigner maybeTodayAssigner = new MaybeTodayAssigner();
		maybeTodayAssigner.setLSP(lsp);

		strategyManager.addStrategy(new TomorrowShipmentAssignerStrategyFactory(maybeTodayAssigner).createStrategy(), null, 1);
		return strategyManager;
	}

}
