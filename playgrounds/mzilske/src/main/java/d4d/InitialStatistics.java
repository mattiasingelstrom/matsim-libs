package d4d;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.geotools.referencing.GeodeticCalculator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class InitialStatistics {
	
	public InitialStatistics(String suffix) {
		this.suffix = suffix;
	}
	
	final private String suffix;
	
	
	public void run(Population population) {
		BetweenSightings betweenSightings = new BetweenSightings();
		betweenSightings.run(population);
		betweenSightings.close();
	}
	
	class BetweenSightings extends AbstractPersonAlgorithm {
		
		private CoordinateTransformation back = TransformationFactory.getCoordinateTransformation("EPSG:3395",TransformationFactory.WGS84);
		
		public BetweenSightings() {

			try {
				minutes = new PrintWriter("/Users/zilske/d4d/output/minutes" + suffix + ".txt");
				personAverageWriter = new PrintWriter("/Users/zilske/d4d/output/person-average" + suffix + ".txt");
				distancesBetweenMeasurements = new PrintWriter("/Users/zilske/d4d/output/distances" + suffix + ".txt");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			
		}
		
		
		
		

		private PrintWriter minutes;

		private PrintWriter personAverageWriter;

		private PrintWriter distancesBetweenMeasurements;
		
		final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		final DateTime beginning = dateTimeFormat.parseDateTime("2011-12-06 00:00:00");

		@Override
		public void run(Person person) {
			long personSum=0;

			long countPerPerson=0;
			Activity lastActivity = null;
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {

					Activity act = (Activity) pe;

					DateTime sighting = new DateTime((long) act.getEndTime()*1000 + beginning.getMillis());
					if (lastActivity != null) {

						DateTime lastSighting = new DateTime((long) lastActivity.getEndTime()*1000 + beginning.getMillis());
						long minutesBetween = new Duration(lastSighting, sighting).getStandardMinutes();
						minutes.println(minutesBetween);
						personSum += minutesBetween;
						countPerPerson++;
						
						GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
						Coord longLat = back.transform(act.getCoord());
						Coord previous = back.transform(lastActivity.getCoord());
						geodeticCalculator.setStartingGeographicPoint(previous.getX(), previous.getY());
						geodeticCalculator.setDestinationGeographicPoint(longLat.getX(), longLat.getY());
						
						double dist = geodeticCalculator.getOrthodromicDistance();
						if(dist != 0.0){
							distancesBetweenMeasurements.println(dist);
						}
						
					}
					
					

					lastActivity = act;
				}
			}
			if (countPerPerson != 0 && personSum != 0) {
				personAverageWriter.println(personSum / countPerPerson);
			}
		}
		
		public void close() {

			minutes.close();
			personAverageWriter.close();
			distancesBetweenMeasurements.close();
		}
		
		
	}

}


