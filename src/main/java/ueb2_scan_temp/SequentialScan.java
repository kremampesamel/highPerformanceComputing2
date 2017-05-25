package ueb2_scan_temp;

import java.io.IOException;

public class SequentialScan {

    public static void main(String args[]) throws IOException {
        int length = 10000000;
        //leave it at this
        int[] numbers = new int[length];
        for (int i = 0; i < length; i++) {
            numbers[i] = (int) (Math.random()*100);
        }

        int[] result = new int[length + 1];
        long startTime = System.currentTimeMillis();
        result[0] = 0;
         for (int i = 0; i < numbers.length; i++) {
            result[i + 1] = result[i] + numbers[i];
//            System.out.println(result[i]);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Sequential scan takes: " + (float)elapsedTime/1000 + " seconds");
    }

}
