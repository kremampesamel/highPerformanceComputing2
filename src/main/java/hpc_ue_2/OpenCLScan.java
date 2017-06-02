package hpc_ue_2;

import helper.Timeable;
import org.jocl.*;
import helper.JOCLHelper;


import java.util.Random;

import static org.jocl.CL.*;

public class OpenCLScan implements ScanOperation, Timeable {

    private final int numberOfElements;
    private final int numberOfWorkgroups;

    private long wholeTime;
    private long wholeExecutionTime;

    public OpenCLScan(int numberOfElements, int numberOfWorkgroups) {
        this.numberOfElements = numberOfElements;
        this.numberOfWorkgroups = numberOfWorkgroups;
    }

    public static void main(String args[]) throws Exception {

        int numberOfElements = 16384;
        int numberOfWorkgroups = 32;//correlated to number of elements

        int[] inputDataArray = createInputData(numberOfElements);

        OpenCLScan scan = new OpenCLScan(numberOfElements, numberOfWorkgroups);
        scan.executeForNElements(inputDataArray);

        System.out.println("OpenCL execution information: ");
        String line = Timeable.printTime(numberOfElements, scan);
        System.out.println(line);
    }


    @Override
    public int[] executeForNElements(int[] inputDataArray) {
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        JOCLHelper jocl = new JOCLHelper(platformIndex, deviceType, deviceIndex);
        jocl.init();

        jocl.createContext();

        // Create the kernel
        cl_kernel kernel = jocl.createKernel("prescan", "sourceTask2_prescan.cl");
        cl_kernel kernelFinalScan = jocl.createKernel("finalScan", "sourceTask2_finalScan.cl");

        long global_work_size[] = new long[]{numberOfElements / 2};//anzahl der threads
        long local_work_size[] = new long[]{(numberOfElements / 2) / numberOfWorkgroups};//groß wie die workgroup

        Pointer inputDataPointer = Pointer.to(inputDataArray);

        int outputOfFirstScanArray[] = new int[numberOfElements];
        Pointer outputOfFirstScanPointer = Pointer.to(outputOfFirstScanArray);

        int workgroupSumsArray[] = new int[numberOfWorkgroups];
        Pointer workgroupSumsPointer = Pointer.to(workgroupSumsArray);

        int workgroupSumsScannedArray[] = new int[numberOfWorkgroups];
        Pointer workgroupSumsScannedPointer = Pointer.to(workgroupSumsScannedArray);

        int finalScannedArray[] = new int[numberOfElements];
        Pointer finalScannedArrayPointer = Pointer.to(finalScannedArray);

        long start = System.currentTimeMillis();
        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(5);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, inputDataPointer);//input
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);//firstInputScan
        memObjects[2] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);//sums
        memObjects[3] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);//scanSums
        memObjects[4] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);//finalScannedInput

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernel, 3, numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{1}));//save == 1, later change to boolean

        int work_dim = 1;
        // Execute the kernel
        long prepareTime = timeFromBegin(start);
        jocl.executeKernel(kernel, global_work_size, local_work_size, work_dim);

        // Read the output data
        long executionTime = timeFromBegin(start, prepareTime);

        // Read the output data
        jocl.bufferIntoPointer(memObjects[1], CL_TRUE, 0, Sizeof.cl_int * numberOfWorkgroups, outputOfFirstScanPointer);
        jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, Sizeof.cl_int * numberOfWorkgroups, workgroupSumsPointer);
        long secondPrepareTime = timeFromBegin(start, executionTime);

        // set kernel arguments 2nd time
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[2]));//sums
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[3]));//scanSums
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfWorkgroups}));
        clSetKernelArg(kernel, 3, numberOfWorkgroups * Sizeof.cl_int, null);//temp array
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, null);//will not be used
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{0}));// 0 means we are not saving the sums

        //Execute the kernel second time
        jocl.executeKernel(kernel, global_work_size, local_work_size, work_dim);
        long secondExecutionTime = timeFromBegin(start, secondPrepareTime);

        // Read the output data 2nd time
        jocl.bufferIntoPointer(memObjects[3], CL_TRUE, 0, Sizeof.cl_int * numberOfWorkgroups, workgroupSumsScannedPointer);
        long secondReadTime = timeFromBegin(start, secondExecutionTime);

        // set kernel arguments final scan
        clSetKernelArg(kernelFinalScan, 0, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernelFinalScan, 1, Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernelFinalScan, 2, Sizeof.cl_mem, Pointer.to(memObjects[4]));

        //Execute the kernel final time
        jocl.executeKernel(kernelFinalScan, global_work_size, local_work_size, work_dim);

        long finalScanTime = timeFromBegin(start, secondReadTime);

        // Read the output data final time
        jocl.bufferIntoPointer(memObjects[4], CL_TRUE, 0, Sizeof.cl_int * numberOfElements, finalScannedArrayPointer);

        jocl.releaseAndFinish();

        wholeTime = System.currentTimeMillis() - start;
        wholeExecutionTime = executionTime + secondExecutionTime + finalScanTime;

        try {
            verifyAndPrintResults(inputDataArray, outputOfFirstScanArray, workgroupSumsArray, workgroupSumsScannedArray, finalScannedArray);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return finalScannedArray;
    }

    private static long timeFromBegin(long start, long... minus) {
        long value = System.currentTimeMillis() - start;
        for (long val : minus) {
            value -= val;
        }
        return value;
    }

    private static int[] createInputData(int numberOfElements) {
        int[] inputData = new int[numberOfElements];
        Random random = new Random();
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = random.nextInt(11);
        }
        return inputData;
    }

    private static void verifyAndPrintResults(int[] inputData, int[] outputOfFirstScanArray, int[] workgroupSumsArray, int[] workgroupSumsScannedArray, int[] finalScannedArray) throws Exception {
        System.out.println("\nInput data:");
        for (int i = 0; i < inputData.length; i++) {
            System.out.print(inputData[i] + " ");
        }

        System.out.println("Scan result:");
        for (int i = 0; i < outputOfFirstScanArray.length; i++) {
            System.out.print(outputOfFirstScanArray[i] + " ");
        }

        System.out.println("\n\nSum result:");
        for (int i = 0; i < workgroupSumsArray.length; i++) {
            System.out.print(workgroupSumsArray[i] + " ");
        }

        System.out.println("\n\nSum workgroup scan result:");
        for (int i = 0; i < workgroupSumsScannedArray.length; i++) {
            System.out.print(workgroupSumsScannedArray[i] + " ");
        }

        System.out.println("\n\nFinal scanned array:");
        for (int i = 0; i < finalScannedArray.length; i++) {
            System.out.print(finalScannedArray[i] + " ");
        }

        verifyResult(inputData, finalScannedArray);
    }

    private static void verifyResult(int[] inputDataArray, int[] finalScannedArray) throws Exception {
        long start = System.currentTimeMillis();
        int[] sequentialScanResult = SequentialScan.executeScanForElements(inputDataArray);
        long sequentialTime = timeFromBegin(start);

        for (int i = 0; i < finalScannedArray.length; i++) {
            if (sequentialScanResult[i] != finalScannedArray[i]) {
                throw new Exception("did not work correctly");
            }
        }
        System.out.println("\n\nSequential execution array:");
        for (int i = 0; i < sequentialScanResult.length; i++) {
            System.out.print(sequentialScanResult[i] + " ");
        }

        System.out.println(String.format("\n\nSequential execution information:\nScanned elements: n=%s total:%s ms\n", inputDataArray.length, sequentialTime));
    }

    @Override
    public long getTotalTime() {
        return wholeTime;
    }

    @Override
    public long getOperationTime() {
        return wholeExecutionTime;
    }

    @Override
    public long getMemoryTime() {
        return wholeTime - wholeExecutionTime;
    }

}