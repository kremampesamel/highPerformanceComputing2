package hpc_ue_2;

import java.io.IOException;


/**
 * Java implementation for comparison
 */
public class SequentialScan implements ScanOperation , Timeable{

    private long totalTime;

    public static void main(String args[]) throws IOException {
        int length = 10000000;
        SequentialScan scan = new SequentialScan();

        int[] numbers = ScanOperation.randomNumbers(length);

        int[] results = scan.executeForNElements(numbers);

        System.out.println(Timeable.printTime(results.length,scan));
    }



    public static int[] executeScanForElements(int[] numbers) {
        int[] result = new int[numbers.length];
        result[0] = 0;
        for (int i = 0; i < numbers.length -1; i++) {
            result[i + 1] = result[i] + numbers[i];
        }
        return result;
    }

    @Override
    public int[] executeForNElements(int[] data) {

        long startTime = System.currentTimeMillis();
        int[] result = executeScanForElements(data);
        totalTime= System.currentTimeMillis() - startTime;
        return result;
    }

    @Override
    public long getTotalTime() {
        return totalTime;
    }

    @Override
    public long getOperationTime() {
        return totalTime;
    }

    @Override
    public long getMemoryTime() {
        return 0;
    }
}
