/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hpc_sample;

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
public class JOCLSample {

    /**
     * The entry point of this sample
     *
     * @param args Not used
     */
    public static void main(String args[]) {

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // init jocl helper
        JOCLHelper jocl = new JOCLHelper(platformIndex, deviceType, deviceIndex);
        jocl.init();

        // Create input- and output data
        int n = 10;
        float srcArrayA[] = new float[n];
        float srcArrayB[] = new float[n];
        float dstArray[] = new float[n];
        for (int i = 0; i < n; i++) {
            srcArrayA[i] = i;
            srcArrayB[i] = i;
        }
        Pointer srcA = Pointer.to(srcArrayA);
        Pointer srcB = Pointer.to(srcArrayB);
        Pointer dst = Pointer.to(dstArray);

        cl_context context = jocl.createContext();

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(3);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * n, srcA);
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * n, srcB);
        memObjects[2] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_float * n, null);


        cl_kernel kernel = jocl.createKernel("sampleKernel", "hpc_0_sample.cl");

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2,
                Sizeof.cl_mem, Pointer.to(memObjects[2]));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{n};
        long local_work_size[] = new long[]{1};
        jocl.executeKernel(kernel, global_work_size, local_work_size, 1);

        // Read the output data
        jocl.readIntoBuffer(memObjects[2], CL_TRUE, 0, n * Sizeof.cl_float, dst);

        // Release kernel, program, and memory objects
        //jocl.releaseKernel(kernel, program);
        jocl.releaseKernel();
        jocl.releaseAndFinish();

        // Verify the result
        boolean passed = true;
        final float epsilon = 1e-7f;
        for (int i = 0; i < n; i++) {
            float x = dstArray[i];
            float y = srcArrayA[i] * srcArrayB[i];
            boolean epsilonEqual = Math.abs(x - y) <= epsilon * Math.abs(x);
            if (!epsilonEqual) {
                passed = false;
                break;
            }
        }
        System.out.println("Test " + (passed ? "PASSED" : "FAILED"));
        if (n <= 10) {
            System.out.println("Result: " + java.util.Arrays.toString(dstArray));
        }
    }
}
