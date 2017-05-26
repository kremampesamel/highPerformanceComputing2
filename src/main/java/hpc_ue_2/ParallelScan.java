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

        // Create input- and output data
        int numberOfElements = 1024;//16
        int numberOfWorkgroups = 32;//4  this seems to work for multiples correlated to number of elements


        //int srcArrayA[] = randomNumbers(size);
        int srcArrayA[] = new int[]{5, 5, 5, 1, 1, 1, 1, 1};
        int[] inputDataArray = srcArrayA;
        size = srcArrayA.length;

        int destArray[] = new int[size];
        Pointer srcA = Pointer.to(srcArrayA);
        Pointer dest = Pointer.to(destArray);

        cl_context context = jocl.createContext();

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(5);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, srcA);
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);
        memObjects[2] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);
        memObjects[3] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);
        memObjects[4] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null);

        cl_kernel kernel = jocl.createKernel("prescan", "sourceTask2_prescan.cl");

        // Set the arguments for the kernel
        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernel, 3, numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{1}));//save == 1, later change to boolean


        // Set the work-item dimensions
        // Set the work-item dimensions

        //anzahl der threads
        long global_work_size[] = new long[]{numberOfElements / 2};
        //size of workgroup
        long local_work_size[] = new long[]{(numberOfElements / 2) / numberOfWorkgroups};


        int work_dim = 1;

        long start = System.currentTimeMillis();
        jocl.executeKernel(kernel, global_work_size, local_work_size, work_dim);

        // Read the output data
        long executionTime = System.currentTimeMillis() - start;
        jocl.readIntoBuffer(memObjects[1], CL_TRUE, 0, Sizeof.cl_int * size, dest);
        long copyTime = System.currentTimeMillis() - executionTime;


        cl_kernel kernelFinalScannel = jocl.createKernel("finalScan", "sourceTask2_finalScan.cl");

        // Read the output data


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
