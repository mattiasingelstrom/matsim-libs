/**
 * 
 */
package playground.yu.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;
import playground.yu.utils.io.SimpleWriter;

/**
 * this class contains some codes from ChangeLegMode
 * 
 * @author ychen
 * 
 */
public class ChangeLegModeWithParkLocation extends AbstractMultithreadedModule {
	private final static String CONFIG_MODULE = "changeLegModeWithParkLocation";
	private final static String CONFIG_PARAM_MODES = "modes";
	private TransportMode[] availableModes = new TransportMode[] {
			TransportMode.car, TransportMode.pt };

	public ChangeLegModeWithParkLocation() {
	}

	/**
	 * @param availableModes
	 *            an array for TransportMode
	 */
	public ChangeLegModeWithParkLocation(final TransportMode[] availableModes) {
		super();
		this.availableModes = availableModes.clone();
	}

	public ChangeLegModeWithParkLocation(final Config config) {
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = new TransportMode[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes[i] = TransportMode.valueOf(parts[i].trim());
			}
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ChooseRandomLegModeWithParkLink(availableModes, MatsimRandom
				.getLocalInstance());
	}

	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
		Controler ctl = new ChangeLegModeWithParkLocationControler(args);
		ctl.addControlerListener(new LegChainModesListener());
		ctl.setCreateGraphs(false);
		ctl.setWriteEventsInterval(0);
		ctl
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
						config.charyparNagelScoring()));
		ctl.run();
	}

	private static class ChooseRandomLegModeWithParkLink implements
			PlanAlgorithm {
		private TransportMode[] possibleModes;

		private Random rnd;

		public ChooseRandomLegModeWithParkLink(TransportMode[] possibleModes,
				Random rnd) {
			this.possibleModes = possibleModes;
			this.rnd = rnd;
		}

		public void run(Plan plan) {
			int peSize = plan.getPlanElements().size();
			Plan copyPlan = new PlanImpl(plan.getPerson());
			if (peSize > 1) {
				boolean done = false;
				while (!done) {
					copyPlan.getPlanElements().clear();
					copyPlan.copyPlan(plan);
					// Idx for leg in PlanElements, whose TransportMode would be
					// changed.
					int legIdx = rnd.nextInt(peSize / 2) * 2 + 1;

					// System.out.println(done + "\tpersonId:\t"
					// + plan.getPerson().getId() + "\tlegIdx:" + legIdx);

					TransportMode crtMode = ((Leg) copyPlan.getPlanElements()
							.get(legIdx)).getMode();
					TransportMode newMode = crtMode;
					while (newMode.equals(crtMode)) {
						newMode = possibleModes[rnd
								.nextInt(possibleModes.length)];
					}

					if (crtMode.equals(TransportMode.car)) {
						done = changeCar2UnCar(copyPlan, legIdx, newMode);
					} else {
						if (newMode.equals(TransportMode.car))
							done = changeUnCar2Car(copyPlan, legIdx);
						else
							done = changeUnCar2UnCar(copyPlan, legIdx, newMode);
					}
				}
				plan.getPlanElements().clear();
				plan.copyPlan(copyPlan);
			}
		}

		private boolean changeUnCar2UnCar(Plan plan, int legIdx,
				TransportMode mode) {
			PlanElement pe = plan.getPlanElements().get(legIdx);
			if (pe != null) {
				((Leg) pe).setMode(mode);
				return true;
			}
			return false;
		}

		private boolean changeCar2UnCar(Plan plan, int legIdx,
				TransportMode mode) {
			CarLegChain clc = new CarLegChain(plan);

			// System.out.println("personId:\t" + personId + "\tcarChain:\t"
			// + clc.getActIdxs4CarLegs());

			int l = -1, r = -1;
			// boundary of the CarLegChain, where the legIdx stands.
			for (Tuple<Integer, Integer> tuple : clc.getActIdxs4CarLegs()) {
				if (tuple.getFirst() < legIdx && tuple.getSecond() > legIdx) {
					l = tuple.getFirst();
					r = tuple.getSecond();
					break;
				}
			}
			// System.out.println("personId :\t" + personId + "\tl :\t" + l
			// + "\tr :\t" + r);

			List<PlanElement> pes = plan.getPlanElements();
			Set<Tuple<Integer, Integer>> tuples = new HashSet<Tuple<Integer, Integer>>();
			// looks for "car" legs from left to right, where man can "get off"
			ParkLocation leftPl = new ParkLocation((Activity) pes.get(l));
			int tmpL = l;
			for (int i = l + 2; i < r; i += 2) {
				if (new ParkLocation((Activity) pes.get(i)).equals(leftPl)) {
					tuples.add(new Tuple<Integer, Integer>(tmpL, i));
					tmpL = i;
				}
			}

			// System.out.println("tuples Size :\t" + tuples.size());

			// looks for "car" legs from right to left, where man can "get off"
			ParkLocation rightPl = new ParkLocation((Activity) pes.get(r));
			int tmpR = r;
			for (int j = r - 2; j > l; j -= 2)
				if (new ParkLocation((Activity) pes.get(j)).equals(rightPl)) {
					tuples.add(new Tuple<Integer, Integer>(j, tmpR));
					tmpR = j;
				}

			// looks for "car" legs from both sides to middle, where man can
			// "get off"
			for (int i = l + 2; i <= r - 4; i += 2)
				for (int j = r - 2; j > i; j -= 2)
					if (new ParkLocation((Activity) pes.get(i))
							.equals(new ParkLocation((Activity) pes.get(j))))
						tuples.add(new Tuple<Integer, Integer>(i, j));

			if (tuples.size() > 0) {
				// Is there changeable leg chain, which also contains leg with
				// this
				// legIdx
				Set<Tuple<Integer, Integer>> legTuples = new HashSet<Tuple<Integer, Integer>>();
				for (Tuple<Integer, Integer> tpl : tuples) {
					if (tpl.getFirst() < legIdx && tpl.getSecond() > legIdx)
						legTuples.add(tpl);
				}

				// which is the shortest?
				Tuple<Integer, Integer> tuple = null;
				boolean toReturn = true;
				if (legTuples.size() > 0) {
					tuple = getShortestLegTuple(legTuples);
					toReturn = toReturn
							&& setLegRandomModeWithoutModeA(plan, tuple,
									TransportMode.car);
					toReturn = toReturn && setLegMode(plan, legIdx, mode);
				} else {// there is not changeable leg chain containing legIdx
					tuple = getShortestLegTuple(tuples);
					toReturn = toReturn
							&& setLegRandomModeWithoutModeA(plan, tuple,
									TransportMode.car);
				}
				return toReturn;
			}
			int lb = 10000, rb = -1;
			// System.out.println("----->" + clc.getActIdxs4CarLegs());
			for (Tuple<Integer, Integer> tuple : clc.getActIdxs4CarLegs()) {
				int f = tuple.getFirst();
				lb = (f < lb) ? f : lb;
				int s = tuple.getSecond();
				rb = (s > rb) ? s : rb;
			}
			// System.out.println("----->lb=" + lb + "\trb=" + rb);
			return setLegRandomModeWithoutModeA(plan,
					new Tuple<Integer, Integer>(lb, rb), TransportMode.car);
		}

		/*
		 * private boolean setLegRandomModeWithoutModeAs(Plan plan,
		 * Tuple<Integer, Integer> tuple, Set<TransportMode> modeAs) { boolean
		 * toReturn = true; for (int i = tuple.getFirst() + 1; i <
		 * tuple.getSecond(); i += 2) { TransportMode newMode =
		 * possibleModes[rnd .nextInt(possibleModes.length)]; while
		 * (modeAs.contains(newMode)) newMode =
		 * possibleModes[rnd.nextInt(possibleModes.length)]; toReturn = toReturn
		 * && setLegMode(plan, i, newMode); } return toReturn; }
		 */

		private boolean setLegRandomModeWithoutModeA(Plan plan,
				Tuple<Integer, Integer> tuple, TransportMode modeA) {
			boolean toReturn = true;
			for (int i = tuple.getFirst() + 1; i < tuple.getSecond(); i += 2) {
				TransportMode newMode = possibleModes[rnd
						.nextInt(possibleModes.length)];
				while (modeA.equals(newMode))
					newMode = possibleModes[rnd.nextInt(possibleModes.length)];
				toReturn = toReturn && setLegMode(plan, i, newMode);
			}
			return toReturn;
		}

		private Tuple<Integer, Integer> getShortestLegTuple(
				Set<Tuple<Integer, Integer>> legTuples) {
			List<Tuple<Integer, Integer>> tmpTpls = new ArrayList<Tuple<Integer, Integer>>();
			int shortestLegActIdxDiff = 10000;
			if (legTuples.size() > 1) {
				for (Tuple<Integer, Integer> tpl : legTuples) {
					int diff = tpl.getSecond() - tpl.getFirst();
					if (diff < shortestLegActIdxDiff) {
						shortestLegActIdxDiff = diff;
						tmpTpls.clear();
						tmpTpls.add(tpl);
					} else if (diff == shortestLegActIdxDiff) {
						tmpTpls.add(tpl);
					}
				}
				if (tmpTpls.size() == 1)
					return tmpTpls.iterator().next();
				else {
					int cnt = 0;
					int rndInt = rnd.nextInt(tmpTpls.size());
					for (Iterator<Tuple<Integer, Integer>> itr = tmpTpls
							.iterator(); itr.hasNext();) {
						if (cnt == rndInt)
							return itr.next();
						cnt++;
					}
				}
			} else if (legTuples.size() == 1)
				return legTuples.iterator().next();
			return null;
		}

		private boolean changeUnCar2Car(Plan plan, int legIdx) {
			CarLegChain clc = new CarLegChain(plan);
			List<PlanElement> pes = plan.getPlanElements();
			int firstCarActIdx = clc.getFirstActIdx();
			int lastCarActIdx = clc.getLastActIdx();
			List<Tuple<Integer, Integer>> carChainTuples = clc
					.getActIdxs4CarLegs();
			int clcSize = carChainTuples.size();
			ParkLocation firstPark = null;
			if (clcSize > 0)
				firstPark = new ParkLocation((Activity) pes.get(firstCarActIdx));
			int pesSize = pes.size();

			if (clcSize == 0) {// no car legs in this copyPlan
				System.out.println("----->no car legs in this plan, legIdx="
						+ legIdx);
				List<Tuple<Integer, Integer>> samePLActIds = new ArrayList<Tuple<Integer, Integer>>();
				for (int l = 0; l < pesSize; l += 2)
					for (int r = pesSize - 1; r > l; r -= 2)
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r)))) {
							if (samePLActIds.size() > 0) {
								Tuple<Integer, Integer> lastTuple = samePLActIds
										.get(samePLActIds.size() - 1);
								if (lastTuple.getFirst() != l
										&& lastTuple.getSecond() != r) {
									samePLActIds
											.add(new Tuple<Integer, Integer>(l,
													r));
								}
							} else
								samePLActIds.add(new Tuple<Integer, Integer>(l,
										r));
						}
				int size = samePLActIds.size();
				if (size >= 2) {
					Tuple<Integer, Integer> firstBoundary = samePLActIds.get(0);
					for (int i = 1; i < size; i++) {
						Tuple<Integer, Integer> iBoundary = samePLActIds.get(i);
						if ((legIdx > firstBoundary.getFirst() && legIdx < iBoundary
								.getFirst())
								|| (legIdx > iBoundary.getSecond() && legIdx < firstBoundary
										.getSecond())) {
							return setLegChainMode(plan, firstBoundary
									.getFirst(), iBoundary.getFirst(),
									TransportMode.car)
									&& setLegChainMode(plan, iBoundary
											.getSecond(), firstBoundary
											.getSecond(), TransportMode.car);
						}
					}
					Tuple<Integer, Integer> secondBoundary = samePLActIds
							.get(1);

					return setLegChainMode(plan, firstBoundary.getFirst(),
							secondBoundary.getFirst(), TransportMode.car)
							&& setLegChainMode(plan,
									secondBoundary.getSecond(), firstBoundary
											.getSecond(), TransportMode.car);
				} else if (size == 1) {
					Tuple<Integer, Integer> boundary = samePLActIds.get(0);
					return setLegChainMode(plan, boundary.getFirst(), boundary
							.getSecond(), TransportMode.car);
				} else if (size < 1)
					System.out
							.println("2 park locations with the same parklocation can not be found in this copyPlan");
				return false;
			} else if (legIdx < firstCarActIdx || legIdx > lastCarActIdx) {
				boolean done = false;
				int firstL = -1, firstR = -1;
				for (int l = firstCarActIdx; l >= 0; l -= 2)
					for (int r = lastCarActIdx; r < pesSize; r += 2)
						// act"l" and act"r" has the same ParkLocations
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r))))
							if (l != firstCarActIdx || r != lastCarActIdx) {
								if (firstL < 0 || firstR < 0) {
									firstL = l;
									firstR = r;
								}

								if ((legIdx > l && legIdx < firstCarActIdx)
										|| (legIdx > lastCarActIdx && legIdx < r)) {

									boolean leftDone = false;
									for (int ll = l + 2; ll < firstCarActIdx; ll += 2)
										if (firstPark.equals(new ParkLocation(
												(Activity) pes.get(ll)))) {
											leftDone = setLegChainMode(plan, l,
													ll, TransportMode.car);
											break;
										}
									if (!leftDone)
										leftDone = setLegChainMode(plan, l,
												firstCarActIdx,
												TransportMode.car);

									boolean rightDone = false;
									for (int rr = r - 2; rr > lastCarActIdx; rr -= 2)
										if (firstPark.equals(new ParkLocation(
												(Activity) pes.get(rr)))) {
											rightDone = setLegChainMode(plan,
													rr, r, TransportMode.car);
											break;
										}
									if (!rightDone)
										rightDone = setLegChainMode(plan,
												lastCarActIdx, r,
												TransportMode.car);

									return leftDone || rightDone;
								}
							}

				if (firstL >= 0 && firstR >= 0)
					done = setLegChainMode(plan, firstL, firstCarActIdx,
							TransportMode.car)
							|| setLegChainMode(plan, lastCarActIdx, firstR,
									TransportMode.car);
				return done;
			} else if (legIdx > firstCarActIdx && legIdx < lastCarActIdx) {
				int l = -1, r = lastCarActIdx;// boundary for the legIdx
				for (Tuple<Integer, Integer> tuple : carChainTuples) {
					int sec = tuple.getSecond();
					int fir = tuple.getFirst();
					if (legIdx > sec && sec > l)
						l = sec;
					if (legIdx < fir && fir < r)
						r = fir;
				}
				List<Integer> sameParkActIdxs = new ArrayList<Integer>();
				for (int i = l; i <= r; i += 2) {
					if (new ParkLocation((Activity) pes.get(l))
							.equals(new ParkLocation((Activity) pes.get(i))))
						sameParkActIdxs.add(i);
				}
				sameParkActIdxs.add(legIdx);
				Collections.sort(sameParkActIdxs);
				int legIdxInList = sameParkActIdxs.indexOf(legIdx);
				return (setLegChainMode(plan, sameParkActIdxs
						.get(legIdxInList - 1), sameParkActIdxs
						.get(legIdxInList + 1), TransportMode.car));
			}
			return false;
		}

		private static boolean setLegChainMode(Plan plan, int leftActIdx,
				int rightActIdx, TransportMode mode) {
			if (leftActIdx % 2 != 0 || rightActIdx % 2 != 0 || leftActIdx < 0
					|| rightActIdx > plan.getPlanElements().size()) {
				throw new RuntimeException(
						"----->ERROR: leftActIdx and rightActIdx should be 2 even number! Or actIdx out of Bound!");
			}
			for (int i = leftActIdx + 1; i < rightActIdx; i += 2)
				setLegMode(plan, i, mode);
			return rightActIdx >= leftActIdx + 2;
		}

		private static boolean setLegMode(Plan plan, int legIdx,
				TransportMode mode) {
			List<PlanElement> pes = plan.getPlanElements();
			int size = pes.size();
			if (legIdx % 2 == 0 || legIdx < 0 || legIdx > size) {
				throw new RuntimeException(
						"----->ERROR: legIdx should be an odd number! Or legIdx out of bound!");
			}
			((Leg) pes.get(legIdx)).setMode(mode);
			return legIdx % 2 != 0 && legIdx > 0 && legIdx < size;
		}
		// reserve
		/*
		 * private static boolean checkParkSensible(Plan plan) { int carLegCnt =
		 * 0; Id OrigPark = null, lastNextPark = null;
		 * 
		 * for (PlanElement pe : plan.getPlanElements()) { if (pe instanceof
		 * Leg) { Leg leg = (Leg) pe; if
		 * (leg.getMode().equals(TransportMode.car)) { Id prePark =
		 * plan.getPreviousActivity(leg).getLinkId(); Id nextPark =
		 * plan.getNextActivity(leg).getLinkId();
		 * 
		 * if (carLegCnt == 0) OrigPark = prePark; else if
		 * (!prePark.equals(lastNextPark)) return false;
		 * 
		 * lastNextPark = nextPark; carLegCnt++; } } } return
		 * OrigPark.equals(lastNextPark); }
		 */
	}

	/**
	 * contains act/ park location information
	 * 
	 * @author ychen
	 * 
	 */
	public static class ParkLocation {
		private Activity act;

		// private Coord coord=null;
		// private Link link=null;

		public ParkLocation(Activity act) {
			this.act = act;
			// coord=act.getCoord();
			// link=act.getLink();
		}

		public boolean equals(Object pl) {
			Coord plCoord = ((ParkLocation) pl).act.getCoord();
			Coord thisCoord = this.act.getCoord();

			Link plLink = ((ParkLocation) pl).act.getLink();
			Link thisLink = this.act.getLink();

			if (plCoord != null && thisCoord != null) {// they both have
				// coordinates.
				return thisCoord.equals(plCoord);
			} else if (plLink != null && thisLink != null) {// they both have
				// Links
				if (plLink.equals(thisLink)) {
					return true;
				} else {
					return (plLink.getFromNode().equals(thisLink.getToNode()) && plLink
							.getToNode().equals(thisLink.getFromNode()));
				}
			} else {
				System.err
						.println("----->the 2 acts don't simultaneously have comparable location information!");
				return false;
			}
		}

		public int hashCode() {
			assert false : "hashCode not designed";
			return 42; // any arbitrary constant will do
		}

		// reserve
		// public Activity getAct() {
		// return act;
		// }
	}

	/**
	 * contains activity indexs of car-leg-chain, e.g. a plan has leg chain:
	 * car-car-pt-walk-car, then the activity indexs looks like: [0-4, 8-10]
	 * 
	 * @author ychen
	 * 
	 */
	private static class CarLegChain {

		private List<Tuple<Integer, Integer>> actIdxs4CarLegs = new ArrayList<Tuple<Integer, Integer>>();

		private int firstActIdx = Integer.MIN_VALUE,
				lastActIdx = Integer.MAX_VALUE;

		public CarLegChain(Plan plan) {
			int actIdxA = -1, actIdxB = -1;
			List<PlanElement> pes = plan.getPlanElements();
			for (int i = 1; i <= pes.size() - 2; i += 2) {
				if (((Leg) pes.get(i)).getMode().equals(TransportMode.car)) {
					if (actIdxA == -1)
						actIdxA = i - 1;
					actIdxB = i + 1;
				} else {
					if (actIdxA != -1)
						actIdxs4CarLegs.add(new Tuple<Integer, Integer>(
								actIdxA, actIdxB));
					actIdxA = -1;
				}
			}
			if (actIdxA != -1)// if the last leg is a "car" leg
				actIdxs4CarLegs.add(new Tuple<Integer, Integer>(actIdxA,
						actIdxB));

			int actIdxs4CarLegsSize = actIdxs4CarLegs.size();
			// System.out.println("----->actIdxs4CarLegsSize: "
			// + actIdxs4CarLegsSize);
			if (actIdxs4CarLegsSize > 0) {
				firstActIdx = actIdxs4CarLegs.get(0).getFirst();
				lastActIdx = actIdxs4CarLegs.get(actIdxs4CarLegsSize - 1)
						.getSecond();
			}
		}

		public List<Tuple<Integer, Integer>> getActIdxs4CarLegs() {
			return actIdxs4CarLegs;
		}

		public int getFirstActIdx() {
			return firstActIdx;
		}

		public int getLastActIdx() {
			return lastActIdx;
		}
	}

	private static class ChangeLegModeWithParkLocationControler extends
			Controler {
		public ChangeLegModeWithParkLocationControler(String[] args) {
			super(args);
		}

		protected StrategyManager loadStrategyManager() {
			StrategyManager manager = new StrategyManager();

			manager.setMaxPlansPerAgent(5);

			// ChangeExpBeta
			PlanStrategy strategy1 = new PlanStrategy(new ExpBetaPlanChanger());
			manager.addStrategy(strategy1, 0.7);

			// ChangeLegModeWithParkLocation
			PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
			strategy2.addStrategyModule(new ChangeLegModeWithParkLocation(
					this.config));
			strategy2.addStrategyModule(new ReRoute(this));
			manager.addStrategy(strategy2, 0.1);

			// ReRoute
			PlanStrategy strategy3 = new PlanStrategy(new RandomPlanSelector());
			strategy3.addStrategyModule(new ReRoute(this));
			manager.addStrategy(strategy3, 0.1);

			// TimeAllocationMutator
			PlanStrategy strategy4 = new PlanStrategy(new RandomPlanSelector());
			strategy4.addStrategyModule(new TimeAllocationMutator());
			manager.addStrategy(strategy4, 0.1);

			return manager;
		}
	}

	private static class LegChainModesListener implements StartupListener,
			IterationEndsListener, ShutdownListener {
		private SimpleWriter writer = null;
		private Set<String> patterns;

		public void notifyStartup(StartupEvent event) {
			writer = new SimpleWriter(Controler
					.getOutputFilename("legModesPattern.txt"));
			writer.writeln("iteration\tlegModesPatterns");
			patterns = new HashSet<String>();
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			Controler ctl = event.getControler();
			int itr = event.getIteration();

			// get the leg modes of the selected plan of the first Person
			for (Iterator<Person> pIt = ctl.getPopulation().getPersons()
					.values().iterator(); pIt.hasNext();) {
				StringBuilder legChainModes = new StringBuilder("|");
				for (PlanElement pe : pIt.next().getSelectedPlan()
						.getPlanElements())
					if (pe instanceof Leg)
						legChainModes.append(((Leg) pe).getMode() + "|");
				// writer.writeln(itr + "\t" + legChainModes.toString());
				// writer.flush();
				patterns.add(legChainModes.toString());
			}
		}

		public void notifyShutdown(ShutdownEvent event) {
			writer.writeln("--------------------------------");
			for (String s : patterns) {
				writer.writeln(s);
			}
			writer.close();
		}
	}

}
