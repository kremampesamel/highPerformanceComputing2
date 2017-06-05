package hpc_ueb_3;

import helper.Timeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RadixRunMain {

	public static final int MAX_DISPLAY = 10240;

	public static void main(String args[]) throws Exception {
		//printTestResult();
		createStats(8);
	}

	private static void createStats(int k) {

		int startSize = 1024 * 124 * 4;
		int size = startSize;

		int setSize = 10;
		log("Run statistics for " + size + "*2^" + setSize + " with k=" + k);

		List<String> lines = new ArrayList<>();
		for (int i = 0; i < setSize; i++) {
			size = (int) (startSize * i);
			int[] testSet = createInputData(size);
			Task3RadixSort sort = new Task3RadixSort();

			if (lines.size() > 0) {
				String lastLine = lines.get(lines.size()-1);
				log("Last Test i=" + (lines.size()-1) + " for "+lastLine);
			}
			log("Run Test i=" + i + " for " + size + " elements");

			try {
				int[] result = sort.executeForArray(testSet, k);
				verifyAndPrintResults(testSet, result);
				String line = Timeable.printTimeCSV(size, sort);
				lines.add(line);
			} catch (Exception e) {
				e.printStackTrace();
				log("NOT VALID!");
			}
		}

		log("Time");
		log(Timeable.printTimeCSVTitle());
		for (String line : lines) {
			log(line);
		}
	}

	private static void log(String line) {
		System.out.println(line);
	}

	private static void printTestResult() {
		// Create input- and output data
		int numberOfElements = 10240000;//16
		int[] inputDataArray = createInputData(numberOfElements);

		Task3RadixSort sort = new Task3RadixSort();
		int[] result = sort.executeForArray(inputDataArray, 8);

		String line = Timeable.printTime(numberOfElements, sort);
		System.out.println(line);
	}

	public static int[] createInputData(int numberOfElements) {
		int[] inputData = new int[numberOfElements];
		Random random = new Random();
		for (int i = 0; i < inputData.length; i++) {
			inputData[i] = random.nextInt(Integer.MAX_VALUE);
		}
		return inputData;
	}

	public static void verifyAndPrintResults(int[] inputData, int[] resultArray) throws Exception {
		System.out.println("Input data:");

		if (resultArray.length < MAX_DISPLAY) {
			for (int i = 0; i < inputData.length; i++) {
				System.out.print(inputData[i] + " ");
			}

			System.out.println("\nFinal scanned array:");
			for (int i = 0; i < resultArray.length; i++) {
				System.out.print(resultArray[i] + " ");
			}
		}

		int current = Integer.MIN_VALUE;

		for (int value : resultArray) {
			if (value < current) {
				throw new RuntimeException("That is not a sorted array.");
			}
			current = value;
		}
		System.out.print("Array is sorted correctly");
	}
}
