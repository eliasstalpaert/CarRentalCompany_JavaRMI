package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import agency.*;
import company.*;
import exception.*;

public class MainServer {
		
	private final static int LOCAL = 0;
	private final static int REMOTE = 1;
	
	public static void main(String[] args) throws Exception {
		// The first argument passed to the `main` method (if present)
		// indicates whether the application is run on the remote setup or not.
		int localOrRemote = (args.length == 1 && args[0].equals("REMOTE")) ? REMOTE : LOCAL;

		Registry registry = LocateRegistry.createRegistry(1099); // not necessary if run with Ant build script
		
		
		
		CrcData data  = loadData("hertz.csv");
		CarRentalCompany crc = new CarRentalCompany(data.name, data.regions, data.cars);
		
		CrcData data2  = loadData("dockx.csv");
		CarRentalCompany crc2 = new CarRentalCompany(data2.name, data2.regions, data2.cars);
		
		ICarRentalCompany stub = (ICarRentalCompany) UnicastRemoteObject.exportObject(crc,0);
		ICarRentalCompany stub2 = (ICarRentalCompany) UnicastRemoteObject.exportObject(crc2,0);
		
		registry.rebind(data.name, stub);
		registry.rebind(data2.name, stub2);
		
		if(localOrRemote == LOCAL)
		{
			System.setSecurityManager(null);
		}
		
		try
		{
			CarRentalAgency agency = new CarRentalAgency();
			agency.registerCompany(data.name, "//localhost:1099/" + data.name);
			agency.registerCompany(data2.name, "//localhost:1099/" + data2.name);
		}
		catch(Exception e)
		{
			throw e;
		}
		
	}

	public static CrcData loadData(String datafile)
			throws ReservationException, NumberFormatException, IOException {

		CrcData out = new CrcData();
		int nextuid = 0;

		// open file
		InputStream stream = MethodHandles.lookup().lookupClass().getClassLoader().getResourceAsStream(datafile);
		if (stream == null) {
			System.err.println("Could not find data file " + datafile);
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		StringTokenizer csvReader;
		
		try {
			// while next line exists
			while (in.ready()) {
				String line = in.readLine();
				
				if (line.startsWith("#")) {
					// comment -> skip					
				} else if (line.startsWith("-")) {
					csvReader = new StringTokenizer(line.substring(1), ",");
					out.name = csvReader.nextToken();
					out.regions = Arrays.asList(csvReader.nextToken().split(":"));
				} else {
					// tokenize on ,
					csvReader = new StringTokenizer(line, ",");
					// create new car type from first 5 fields
					CarType type = new CarType(csvReader.nextToken(),
							Integer.parseInt(csvReader.nextToken()),
							Float.parseFloat(csvReader.nextToken()),
							Double.parseDouble(csvReader.nextToken()),
							Boolean.parseBoolean(csvReader.nextToken()));
					System.out.println(type);
					// create N new cars with given type, where N is the 5th field
					for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
						out.cars.add(new Car(nextuid++, type));
					}
				}
			}
		} finally {
			in.close();
		}

		return out;
	}
	
	static class CrcData {
		public List<Car> cars = new LinkedList<Car>();
		public String name;
		public List<String> regions =  new LinkedList<String>();
	}	
}
