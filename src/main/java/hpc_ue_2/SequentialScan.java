package hpc_ue_2;

import java.io.IOException;


/**
 * Java implementation for comparison
 */
public class SequentialScan implements ScanOperation {

    public static void main(String args[]) throws IOException {
        int length = 10000000;
        SequentialScan scan = new SequentialScan();
        long elapsedTime = scan.executeForNElements(length);

        System.out.println("Sequential scan takes: " + (float) elapsedTime / 1000 + " seconds");
    }

    @Override
    public long executeForNElements(int size) {
        //leave it at this

        int[] numbers = randomNumbers(size);

        int[] result = new int[size + 1];
        long startTime = System.currentTimeMillis();
        result[0] = 0;
        for (int i = 0; i < numbers.length; i++) {
            result[i + 1] = result[i] + numbers[i];
//            System.out.println(result[i]);
        }

        long stopTime = System.currentTimeMillis();
        return stopTime - startTime;
    }

    public static int[] executeScanForElements(int[] numbers) {
        int[] result = new int[numbers.length];
        result[0] = 0;
        for (int i = 0; i < numbers.length -1; i++) {
            result[i + 1] = result[i] + numbers[i];
        }
        return result;
    }

}
