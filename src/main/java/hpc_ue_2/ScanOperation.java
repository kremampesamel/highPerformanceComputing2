package hpc_ue_2;

/**
 * Created by Flo on 25.05.2017.
 */
public interface ScanOperation {
    /**
     * Implements the scan algorithm for a array with a given size 'size'
     * Must measure the elapsed (netto) time for the operation, preparation must not be included in time measurement.
     *
     * @param data  array to scan
     * @return array with result
     */
    int[] executeForNElements(int[] data);



    /**
     * Creates an int array filled with random numbers
     *
     * @param size
     * @return
     */
    static int[] randomNumbers(int size) {
        int[] numbers = new int[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = (int) (Math.random() * 100);
        }
        return numbers;
    }
}
