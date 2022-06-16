import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

public class Banker {
	private int numberOfCustomers; // the number of customers
	private int numberOfResources; // the number of resources

	private int[] available; // the available amount of each resource
	private int[][] maximum; // the maximum demand of each customer
	private int[][] allocation; // the amount currently allocated
	private int[][] need; // the remaining needs of each customer

	/**
	 * Constructor for the Banker class.
	 * 
	 * @param resources An array of the available count for each resource.
	 * @param numberOfCustomers The number of customers.
	 */
	public Banker(int[] resources, int numberOfCustomers) {
		// Set the number of resources
		this.numberOfResources = resources.length;

		// Set the number of customers
		this.numberOfCustomers = numberOfCustomers;

		// Set the value of bank resources to available
		this.available = resources;

		// Set the array size for maximum, allocation, and need
		this.maximum = new int[this.numberOfCustomers][this.numberOfResources];
		this.allocation = new int[this.numberOfCustomers][this.numberOfResources];
		this.need = new int[this.numberOfCustomers][this.numberOfResources];
	}

	/**
	 * Sets the maximum number of demand of each resource for a customer.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param maximumDemand An array of the maximum demanded count for each resource.
	 */
	public void setMaximumDemand(int customerIndex, int[] maximumDemand) {
		// Add customer, update maximum and need
		this.maximum[customerIndex] = maximumDemand;
		for (int i = 0; i < this.numberOfResources; i++) { // Need is a n by m matrix (n
															// rows/customers, m columns/resources)
			this.need[customerIndex][i] =
					this.maximum[customerIndex][i] - this.allocation[customerIndex][i];
		}
	}

	/**
	 * Prints the current state of the bank.
	 */
	public void printState() {
		System.out.println("\nCurrent state:");
		// print available
		System.out.println("Available:");
		System.out.println(Arrays.toString(available));
		System.out.println("");

		// print maximum
		System.out.println("Maximum:");
		for (int[] aMaximum : maximum) {
			System.out.println(Arrays.toString(aMaximum));
		}
		System.out.println("");
		// print allocation
		System.out.println("Allocation:");
		for (int[] anAllocation : allocation) {
			System.out.println(Arrays.toString(anAllocation));
		}
		System.out.println("");
		// print need
		System.out.println("Need:");
		for (int[] aNeed : need) {
			System.out.println(Arrays.toString(aNeed));
		}
		System.out.println("");
	}

	/**
	 * Requests resources for a customer loan. If the request leave the bank in a safe state, it is
	 * carried out.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param request An array of the requested count for each resource.
	 * @return true if the requested resources can be loaned, else false.
	 */
	public synchronized boolean requestResources(int customerIndex, int[] request) {
		// Print the request
		System.out.println("Customer " + customerIndex + " requesting");
		System.out.println(Arrays.toString(request));

		// Check if request larger than need or available
		for (int i = 0; i < this.numberOfResources; i++) {
			if ((request[i] > this.need[customerIndex][i]) || (request[i] > this.available[i]))
				return false;
		}

		// Check if the state is safe or not
		if (this.checkSafe(customerIndex, request)) {
			// If it is safe, allocate the resources to customer customerNumber
			for (int i = 0; i < this.numberOfResources; i++) {
				this.available[i] -= request[i];
				this.allocation[customerIndex][i] += request[i];
				this.need[customerIndex][i] -= request[i];
			}

			return true;
		} else
			return false;
	}

	/**
	 * Releases resources borrowed by a customer. Assume release is valid for simplicity.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param release An array of the release count for each resource.
	 */
	public synchronized void releaseResources(int customerIndex, int[] release) {
		// Print the release
		System.out.println("Customer " + customerIndex + " releasing");
		System.out.println(Arrays.toString(release));

		// Release the resources from customer customerNumber
		for (int i = 0; i < this.numberOfResources; i++) {
			this.available[i] += release[i];
			this.allocation[customerIndex][i] -= release[i];
			this.need[customerIndex][i] += release[i];
		}
	}

	/**
	 * Checks if the request will leave the bank in a safe state.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param request An array of the requested count for each resource.
	 * @return true if the requested resources will leave the bank in a safe state, else false
	 */
	private synchronized boolean checkSafe(int customerIndex, int[] request) {
		// Check if the state is safe

		// Copy the available, need and allocation arrays
		int[] tempAvailable = this.available.clone();
		int[][] tempNeed = this.need.clone();
		int[][] tempAllocation = this.allocation.clone();

		// Initialize finish vector (defaults to false)
		boolean[] finish = new boolean[this.numberOfCustomers];

		// Initialize a boolean flag
		boolean possible = true;

		for (int i = 0; i < this.numberOfResources; i++) {
			tempAvailable[i] -= request[i];
			tempNeed[customerIndex][i] -= request[i];
			tempAllocation[customerIndex][i] += request[i];
		}

		// Initialize work vector
		int[] work = tempAvailable.clone();

		while (possible) {
			possible = false;
			for (int i = 0; i < this.numberOfCustomers; i++) {
				boolean needDoesNotExceedWork = true;

				for (int j = 0; j < this.numberOfResources; j++) {
					if (tempNeed[i][j] > work[j])
						needDoesNotExceedWork = false;
				}

				if (!finish[i] && needDoesNotExceedWork) {
					possible = true;

					for (int j = 0; j < this.numberOfResources; j++) {
						work[j] += tempAllocation[i][j];
					}

					finish[i] = true;
				}
			}
		}

		// Undo the temporary changes that have been made to tempAllocation and tempNeed
		for (int i = 0; i < this.numberOfResources; i++) {
			tempAllocation[customerIndex][i] -= request[i];
			tempNeed[customerIndex][i] += request[i];
		}

		// Check if all of the entries in the finish vector are true
		for (int i = 0; i < this.numberOfCustomers; i++) {
			if (!finish[i])
				return false;
		}

		return true;
	}

	/**
	 * Parses and runs the file simulating a series of resource request and releases. Provided for
	 * your convenience.
	 * 
	 * @param filename The name of the file.
	 */
	public static void runFile(String filename) {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(filename));

			String line = null;
			String[] tokens = null;
			int[] resources = null;

			int n, m;

			try {
				n = Integer.parseInt(fileReader.readLine().split(",")[1]);
			} catch (Exception e) {
				System.out.println("Error parsing n on line 1.");
				fileReader.close();
				return;
			}

			try {
				m = Integer.parseInt(fileReader.readLine().split(",")[1]);
			} catch (Exception e) {
				System.out.println("Error parsing n on line 2.");
				fileReader.close();
				return;
			}

			try {
				tokens = fileReader.readLine().split(",")[1].split(" ");
				resources = new int[tokens.length];
				for (int i = 0; i < tokens.length; i++)
					resources[i] = Integer.parseInt(tokens[i]);
			} catch (Exception e) {
				System.out.println("Error parsing resources on line 3.");
				fileReader.close();
				return;
			}

			Banker theBank = new Banker(resources, n);

			int lineNumber = 4;
			while ((line = fileReader.readLine()) != null) {
				tokens = line.split(",");
				if (tokens[0].equals("c")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.setMaximumDemand(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("r")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.requestResources(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("f")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.releaseResources(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("p")) {
					theBank.printState();
				}
			}
			fileReader.close();
		} catch (IOException e) {
			System.out.println("Error opening: " + filename);
		}

	}

	/**
	 * Main function
	 * 
	 * @param args The command line arguments
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			runFile(args[0]);
		}
	}
}
