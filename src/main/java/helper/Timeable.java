package helper;

public interface Timeable {

	/**
	 * measures total time
	 * @return
	 */
	long getTotalTime();

	/**
	 * measures time used for calculations
	 * @return
	 */
	long getOperationTime();

	/**
	 * measures time NOT used for calculations, for example preparation and memory.
	 * @return
	 */
	long getMemoryTime();

	static String printTime(int numberOfElements, Timeable scan) {
		return String.format("Scanned elements: n=%s\t\ttotal:%sms\t\texecution:%sms\t\tmemory:%sms", numberOfElements, scan.getTotalTime(), scan.getOperationTime(), scan.getMemoryTime());
	}

	static String printTimeCSV(int numberOfElements, Timeable scan) {
		return String.format("%s\t%s\t%s\t%s", numberOfElements, scan.getTotalTime(), scan.getOperationTime(), scan.getMemoryTime());
	}

	static String printTimeCSVTitle() {
		return String.format("n \t total ms\t execution ms \t memory ms");
	}
}
