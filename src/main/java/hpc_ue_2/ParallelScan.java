/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hpc_ue_2;

/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

//package org.jocl.samples;

import org.jocl.*;
import util.JOCLHelper;

import static org.jocl.CL.*;

/**
 * A small JOCL sample.
 */
public class ParallelScan implements ScanOperation {

    /**
     * The entry point of this sample
     *
     * @param args Not used
     */
    public static void main(String args[]) {

        ParallelScan scan = new ParallelScan();
        int size = 8;
        long elapsedTime = scan.executeForNElements(size);

        System.out.println(String.format("Scan %s elements in %s ms", size, elapsedTime));
    }

    @Override
    public long executeForNElements(int size) {

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // init jocl helper
        JOCLHelper jocl = new JOCLHelper(platformIndex, deviceType, deviceIndex);
        jocl.init();

        //int srcArrayA[] = randomNumbers(size);
        int srcArrayA[] = new int[]{5, 5, 5, 1, 1, 1, 1, 1};
        size = srcArrayA.length;

        int destArray[] = new int[size];
        Pointer srcA = Pointer.to(srcArrayA);
        Pointer dest = Pointer.to(destArray);

        cl_context context = jocl.createContext();

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(2);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * size, srcA);
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * size, null);

        cl_kernel kernel = jocl.createKernel("prescan", "sourceTask2_prescan.cl");

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2,
                Sizeof.cl_int, Pointer.to(new int[]{size}));
        // Set the work-item dimensions
        // Set the work-item dimensions
        long global_work_size[] = new long[]{size};
        long local_work_size[] = new long[]{1};

        int work_dim = 1;

        long start = System.currentTimeMillis();
        jocl.executeKernel(kernel, global_work_size, local_work_size, work_dim);

        // Read the output data
        long executionTime = System.currentTimeMillis() - start;
        jocl.readIntoBuffer(memObjects[1], CL_TRUE, 0, Sizeof.cl_int * size, dest);

        long copyTime = System.currentTimeMillis() - executionTime;
        long wholeTime = System.currentTimeMillis() - start;
        // Release kernel, program, and memory objects
        //jocl.releaseKernel(kernel, program);
        jocl.releaseKernel();
        jocl.releaseAndFinish();

        for (int i = 0; i < size; i++) {
            System.out.println(destArray[i]);
        }

        return wholeTime;
    }
}
