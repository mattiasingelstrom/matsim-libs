/* *********************************************************************** *
 * project: org.matsim.*
 * Income1TravelCostCalculatorFactory
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
package playground.southafrica.gauteng.routing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author kn after bkick after dgrether
 *
 */
public class AutosensingTravelDisutilityInclTollFactory implements TravelDisutilityFactory {

	private final RoadPricingScheme scheme;
	private final Scenario scenario;
	private final ScoringFunctionFactory scoringFunctionFactory;

	public AutosensingTravelDisutilityInclTollFactory( RoadPricingScheme scheme, Scenario scenario, ScoringFunctionFactory scoringFunctionFactory ) {
		this.scheme = scheme ;
		this.scenario = scenario ;
		this.scoringFunctionFactory = scoringFunctionFactory ;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, 
			PlanCalcScoreConfigGroup cnScoringGroup) {
		final EffectiveMarginalUtilitiesContainer muc = RouterUtils.createMarginalUtilitiesContrainer(scenario, scoringFunctionFactory) ;
		final TravelDisutility delegate = new PersonIndividualTimeDistanceDisutility(timeCalculator, muc) ;
		final RoadPricingScheme localScheme = this.scheme ;
		
		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
				double toll_usually_positive = 0. ;
				Cost cost = localScheme.getLinkCostInfo(link.getId(), time, person.getId() ) ;
				if ( cost != null ) {
					/* This needed to be introduced after the GautengRoadPricingScheme started to return null instead of
					 * Cost objects with amount=0.  kai, apr'12
					 */
					if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_DISTANCE) ) {
						toll_usually_positive = link.getLength() * cost.amount ;
					} else if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_LINK ) ) {
						toll_usually_positive = cost.amount ;
					} else {
						/* I guess we can/should take out this exception since `cordon' should now be working? - JWJ Apr '12 */
						/* This still does not work for cordon, and I currently think it never will.  Marcel's cordon toll
						 * is different from other software packages, and so I don't want to mirror the
						 * computation here, especially since we do not need it.  kai, apr'12 */
						throw new RuntimeException("not set up for toll type: " + localScheme.getType() + ". aborting ...") ;
					}

//					double utilityOfMoney_normally_positive = localUtlOfMon.getUtilityOfMoney_normally_positive(person.getId() ) ;
					double utilityOfMoney_normally_positive = muc.getMarginalUtilityOfMoney().get( person.getId() );

					linkTravelDisutility += utilityOfMoney_normally_positive * toll_usually_positive ;
					// positive * positive = positive, i.e. correct (since it is a positive disutility contribution)
				}
				
				return linkTravelDisutility;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};
	}

}
