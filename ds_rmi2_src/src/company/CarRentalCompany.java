package company;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import exception.ReservationException;

public class CarRentalCompany implements ICarRentalCompany{

	private static Logger logger = Logger.getLogger(CarRentalCompany.class.getName());
	
	private List<String> regions;
	private String name;
	private List<Car> cars;
	private Map<String,CarType> carTypes = new HashMap<String, CarType>();

	/***************
	 * CONSTRUCTOR *
	 ***************/

	public CarRentalCompany(String name, List<String> regions, List<Car> cars) {
		logger.log(Level.INFO, "<{0}> Car Rental Company {0} starting up...", name);
		this.name = name;
		this.cars = cars;
		this.regions = regions;
		for(Car car:cars)
			carTypes.put(car.getType().getName(), car.getType());
		logger.log(Level.INFO, this.toString());
	}

	private Car getCar(int uid) throws IllegalArgumentException {
		for (Car car : cars) {
			if (car.getId() == uid)
				return car;
		}
		throw new IllegalArgumentException("<" + name + "> No car with uid " + uid);
	}
	
	@Override
	public Collection<CarType> getAllCarTypes() {
		return carTypes.values();
	}

	@Override
	public Set<CarType> getAvailableCarTypes(Date start, Date end) throws RemoteException {
		Set<CarType> availableCarTypes = new HashSet<CarType>();
		for (Car car : cars) {
			if (car.isAvailable(start, end)) {
				availableCarTypes.add(car.getType());
			}
		}
		return availableCarTypes;
	}
	
	private List<Car> getAvailableCars(String carType, Date start, Date end) {
		List<Car> availableCars = new LinkedList<Car>();
		for (Car car : cars) {
			if (car.getType().getName().equals(carType) && car.isAvailable(start, end)) {
				availableCars.add(car);
			}
		}
		return availableCars;
	}

	@Override
	public List<String> getRegions() throws RemoteException {
		return this.regions;
	}

	@Override
	public Quote createQuote(ReservationConstraints constraints, String client) throws RemoteException, ReservationException, IllegalArgumentException {
		logger.log(Level.INFO, "<{0}> Creating tentative reservation for {1} with constraints {2}", 
                new Object[]{name, client, constraints.toString()});

		
		if(!operatesInRegion(constraints.getRegion()) || !isAvailable(constraints.getCarType(), constraints.getStartDate(), constraints.getEndDate()))
			throw new ReservationException("<" + name + "> No cars available to satisfy the given constraints.");
		
		CarType type = getCarType(constraints.getCarType());
		
		double price = calculateRentalPrice(type.getRentalPricePerDay(),constraints.getStartDate(), constraints.getEndDate());
		
		return new Quote(client, constraints.getStartDate(), constraints.getEndDate(), getName(), constraints.getCarType(), price);
	}
	
	private boolean isAvailable(String carTypeName, Date start, Date end) throws RemoteException, IllegalArgumentException {
		logger.log(Level.INFO, "<{0}> Checking availability for car type {1}", new Object[]{name, carTypeName});
		if(carTypes.containsKey(carTypeName)) {
			return getAvailableCarTypes(start, end).contains(carTypes.get(carTypeName));
		} else {
			throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
		}
	}
	
	private boolean operatesInRegion(String region) {
        return this.regions.contains(region);
    }
	
	private CarType getCarType(String carTypeName) {
		if(carTypes.containsKey(carTypeName))
			return carTypes.get(carTypeName);
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	private double calculateRentalPrice(double rentalPricePerDay, Date start, Date end) {
		return rentalPricePerDay * Math.ceil((end.getTime() - start.getTime())
						/ (1000 * 60 * 60 * 24D));
	}
	
	private String getName() {
		return name;
	}

	@Override
	public synchronized Reservation confirmQuote(Quote quote) throws RemoteException, ReservationException {
		logger.log(Level.INFO, "<{0}> Reservation of {1}", new Object[]{name, quote.toString()});
		List<Car> availableCars = getAvailableCars(quote.getCarType(), quote.getStartDate(), quote.getEndDate());
		if(availableCars.isEmpty())
			throw new ReservationException("Reservation failed, all cars of type " + quote.getCarType() + " are unavailable from " + quote.getStartDate() + " to " + quote.getEndDate());
		Car car = availableCars.get((int)(Math.random()*availableCars.size()));
		
		Reservation res = new Reservation(quote, car.getId());
		car.addReservation(res);
		return res;
	}

	@Override
	public void cancelReservation(Reservation res) throws RemoteException, IllegalArgumentException {
		logger.log(Level.INFO, "<{0}> Cancelling reservation {1}", new Object[]{name, res.toString()});
		getCar(res.getCarId()).removeReservation(res);
	}

	@Override
	public List<Reservation> getReservationsByRenter(String renterName) throws IllegalArgumentException, RemoteException {
		ArrayList<Reservation> renterReservations = new ArrayList<>();
		for(Car c : cars)
			for(Reservation r : c.getReservations())
				if(r.getCarRenter().equals(renterName)) renterReservations.add(r);
		if(renterReservations.isEmpty()) throw new IllegalArgumentException("No reservations were made by renter: " + renterName);
		return renterReservations;
	}

	@Override
	public int getNumberOfReservationsForCarType(String carType) throws RemoteException, IllegalArgumentException {
		getCarType(carType);
		int sum = 0;
		for(Car c : cars)
			if(c.getType().getName().equals(carType))
				sum += c.getReservations().size();
		return sum;
	}

	@Override
	public CarType getMostPopularCarType(Date start, Date end) throws RemoteException {
		Map<CarType, Integer> popularMap = new HashMap<>();
		for(String s : carTypes.keySet())
		{
			CarType c = getCarType(s);
			popularMap.put(c, 0);
			for(Car car : cars)
			{
				if(car.getType().equals(c)) popularMap.replace(c, popularMap.get(c) + car.getReservations().size());
			}
		}
		return Collections.max(popularMap.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
	}

	
	
}