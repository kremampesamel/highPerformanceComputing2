package hpc_ue_2;

import ueb2_scan_temp.ParallelScanMassaros;

/**
 * Created by flo on 5/26/17.
 */
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
}
