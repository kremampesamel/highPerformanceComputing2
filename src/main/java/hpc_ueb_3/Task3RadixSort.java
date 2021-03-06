package hpc_ueb_3;

import helper.JOCLHelper;
import helper.Timeable;
import org.jocl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jocl.CL.*;

/**
 * Created by Flo on 28.05.2017.
 */
public class Task3RadixSort implements Timeable, RadixSort {

    private int numberOfElements;
    private int numberOfWorkgroups;

    private long wholeTime;
    private long wholeExecutionTime;
    private JOCLHelper jocl;
    private int[] finalArray;
    private int[] sortedArray;
    private int[] tmpArray;
    private Pointer inputDataPointer;
    private Pointer finalArrayPointer;
    private Pointer tmpArrayPointer;
    private Pointer sortedArrayPointer;

    public Task3RadixSort() {

        this.init();
    }

    public static void main(String args[]) throws Exception {

        // Create input- and output data
        int numberOfElements = 10240000;//16
        int[] inputDataArray = RadixRunMain.createInputData(numberOfElements);

        Task3RadixSort sort = new Task3RadixSort();
        int[] result = sort.executeForArray(inputDataArray, 8);

        String line = Timeable.printTime(numberOfElements, sort);
        System.out.println(line);
        System.out.println(line);
    }

    private static long timeFromBegin(long start, long... minus) {
        long value = System.currentTimeMillis() - start;
        for (long val : minus) {
            value -= val;
        }
        return value;
    }

    private void init() {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        jocl = new JOCLHelper(platformIndex, deviceType, deviceIndex);
        jocl.init();
        jocl.createContext();
        CL.setExceptionsEnabled(true);
    }

    @Override
    public int[] executeForArray(int[] inputDataArray, int k) {
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        numberOfElements = inputDataArray.length;
        numberOfWorkgroups = numberOfElements / 4;

        sortedArray = new int[numberOfElements];
        tmpArray = new int[numberOfElements];
        finalArray = new int[numberOfElements];

        inputDataPointer = Pointer.to(inputDataArray);
        sortedArrayPointer = Pointer.to(sortedArray);
        tmpArrayPointer = Pointer.to(tmpArray);
        finalArrayPointer = Pointer.to(finalArray);

        long start = System.currentTimeMillis();

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(3);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, inputDataPointer);
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, sortedArrayPointer);
        memObjects[2] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);

        // 1. step: sort all data in arbitrary number of chunks
        performOneSort(memObjects, k);

        // mock
        int numBuckets = (int) Math.pow(2, 8);
        int bucketSize = Math.max(4, (int) numberOfElements / numBuckets);

        mockSortedArray(numBuckets, bucketSize);

        // Create the kernel
        cl_kernel kernelMerge = jocl.createKernel("radix_merge", "sourceTask3_radix_merge.cl");

        // 2. step: perform merges of chunks, to retrieve a proper sorted array
        int n = numberOfElements;

        // Start with a k = 8;
        iterativeMerge(kernelMerge, memObjects, numberOfElements, k);

        long executionTime = timeFromBegin(start);

        cleanup();

        wholeTime = System.currentTimeMillis() - start;
        wholeExecutionTime = executionTime;

        return finalArray;
    }

    public void cleanup() {
        try {
            jocl.releaseAndFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performOneSort(cl_mem[] memObjects, int k) {
        cl_kernel kernelSort = jocl.createKernel("radix_sort8", "sourceTask3_radix_sort.cl");

        long global_work_size[] = new long[]{numberOfElements / 2};
        long local_work_size[] = new long[]{(numberOfElements / 2) / numberOfWorkgroups};

        int numBuckets = (int) Math.pow(2, k);
        int bucketSize = Math.max(Math.min(4, numberOfElements), (int) numberOfElements / numBuckets);

        // Set the arguments for the kernel
        clSetKernelArg(kernelSort, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernelSort, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernelSort, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernelSort, 3, Sizeof.cl_int, Pointer.to(new int[]{bucketSize}));

        int work_dim = 1;
        // Execute the kernel
        jocl.executeKernel(kernelSort, global_work_size, local_work_size, work_dim);

        // Read the output data final time
        jocl.bufferIntoPointer(memObjects[1], CL_TRUE, 0, Sizeof.cl_int * numberOfElements, sortedArrayPointer);
    }

    private long iterativeMerge(cl_kernel kernelMerge, cl_mem[] memObjects, int numberOfElements, int k_start) {

        long start = System.currentTimeMillis();
        int work_dim = 1;
        int size = Sizeof.cl_int * numberOfElements;

        // repeat until k == 0!
        for (int k = k_start; k > 0; k--) {
            int numBuckets = (int) Math.pow(2, k);
            int bucketSize = Math.max(Math.min(4, numberOfElements), (int) numberOfElements / numBuckets);

            // for recap and local_wg, one thread for two buckets
            int global_work_size_int = numBuckets / 2;
            int local_work_size_int = 1; //Math.max(numBuckets / 16, 4);
            long[] global_work_size = new long[]{global_work_size_int};
            long[] local_work_size = new long[]{local_work_size_int};

            // sorted array
            clSetKernelArg(kernelMerge, 0, Sizeof.cl_mem, Pointer.to(memObjects[1]));
            // tmp array
            clSetKernelArg(kernelMerge, 1, Sizeof.cl_mem, Pointer.to(memObjects[2]));
            clSetKernelArg(kernelMerge, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
            clSetKernelArg(kernelMerge, 3, Sizeof.cl_int, Pointer.to(new int[]{bucketSize}));

            //SampleMerge merge = new SampleMerge(0, 0, global_work_size_int, local_work_size_int);
            //merge.radix_merge(finalArray, k, numBuckets,numberOfElements, bucketSize,tmp);

            pointerIntoBuffer(memObjects[1], size, sortedArrayPointer);
            pointerIntoBuffer(memObjects[2], size, tmpArrayPointer);
            //jocl.bufferIntoPointer(memObjects[1], CL_TRUE, 0, size, sortedArrayPointer);
            //jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, size, tmpArrayPointer);
            jocl.executeKernel(kernelMerge, global_work_size, local_work_size, work_dim);

            // Read the output data 2nd time
            jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, size, tmpArrayPointer);

            // write buffer 2 to 1 back again, store tmp in sorted
            // CAUTION: i tried to remove something, then it was broken. be aware..
            clEnqueueCopyBuffer(jocl.getCommandQueue(), memObjects[2], memObjects[1], 0, 0, size, 0, null, null);
            jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, size, sortedArrayPointer);

            long usedTime = timeFromBegin(start);
            System.out.println(String.format("time: %s ms", usedTime));
        }
        jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, size, tmpArrayPointer);
        jocl.bufferIntoPointer(memObjects[2], CL_TRUE, 0, size, finalArrayPointer);


        long usedTime = timeFromBegin(start);
        System.out.println(String.format("total time: %s ms", usedTime));
        return usedTime;
    }

    private void pointerIntoBuffer(cl_mem memObject, int size, Pointer pointer) {
        clEnqueueWriteBuffer(jocl.getCommandQueue(), memObject, true, 0, size, pointer, 0, null, null);
    }

    private void mockSortedArray(int numBuckets, int bucketSize) {
        List<Integer> mockList = new ArrayList<>();
        for (int i = 0; i < numBuckets; i++) {

            List<Integer> sub = new ArrayList<>();
            for (int j = 0; j < bucketSize; j++) {
                sub.add((int) ((int) 1 + (Math.random() * 40000)));
            }
            Collections.sort(sub);
            mockList.addAll(sub);
        }
        for (int i = 0; i < sortedArray.length && i < mockList.size(); i++) {
            sortedArray[i] = mockList.get(i);
        }
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
