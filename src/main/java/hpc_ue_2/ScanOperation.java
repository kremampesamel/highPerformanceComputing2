package hpc_ue_2;

/**
 * Created by Flo on 25.05.2017.
 */
public interface ScanOperation {
    /**
     * Implements the scan algorithm for a random array with a given size 'size'
     * Must measure the elapsed (netto) time for the operation, preparation must not be included in time measurement.
     *
     * @param size size of array to scan
     * @return elapsed time
     */
    long executeForNElements(int size);

    /**
     * Creates an int array filled with random numbers
     *
     * @param size
     * @return
     */
    default int[] randomNumbers(int size) {
        int[] numbers = new int[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = (int) (Math.random() * 100);
        }
        return numbers;
    }
}
