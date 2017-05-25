

/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

//package org.jocl.samples;

import static org.jocl.CL.*;

import org.jocl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A small JOCL sample.
 */
public class JOCLSample
{
    /**
     * The source code of the OpenCL program to execute
     */
    private static String programSourceOld =
        "__kernel void "+
                "sampleKernel(__global const float *a,"+
                "             __global const float *b,"+
                "             __global float *c)"+
                "{"+
                "    int tid = get_global_id(0);"+
                "    int tid = get_global_id(0);"+
                "    c[tid] = a[tid] * b[tid];"+
                "}";

    private static String programSource3 =
            "__kernel void "+
                    "sampleKernel(__global const float *a,"+
                    "             __global const float *b,"+
                    "             __global float *c, int Wa, int Wb)"+
                    "{"+
                    "int tid = get_global_id(0);"+
                    "    int tid = get_global_id(0);"+
                    "    c[tid] = a[tid] * b[tid];"+
                    "}";


    private static String programSource4 =
            "__kernel void simpleMultiply(\n" +
                    "__global float* c, int Wa, int Wb,\n" +
                    "__global float* a, __global float* b) {\n" +
                    "//Get global position in Y direction\n" +
                    "int row = get_global_id(1);\n" +
                    "//Get global position in X direction\n" +
                    "int col = get_global_id(0);\n" +
                    "float sum = 0.0f;\n" +
                    "//Calculate result of one element\n" +
                    "for (int i = 0; i < Wa; i++) {\n" +
                    "sum +=\n" +
                    " a[row*Wa+i] * b[i*Wb+col];\n" +
                    "}\n" +
                    "c[row*Wb+col] = sum;\n" +
                    "}";

    private static String programSource =
            "__kernel void "+
                    "sampleKernel(__global const float *a,"+
                    "             __global const float *b,"+
                    "             __global float *c, int *width, int *height)"+
                    "{"+
                    "    int col = get_global_id(0);"+
                    "    int row = get_global_id(1);"+
                    "    int tid = row * witdh + col;"+
                    "    c[tid] = a[tid] * b[tid];"+
                    "}";

    /**
     * Returns the value of the device info parameter with the given name
     *
     * @param device The device
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_device_id device, int paramName)
    {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int)size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length-1);
    }

    /**
     * Returns the value of the platform info parameter with the given name
     *
     * @param platform The platform
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_platform_id platform, int paramName)
    {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int)size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length-1);
    }

    /**
     * The entry point of this sample
     *
     * @param args Not used
     */
    public static void main(String args[])
    {
        // Create input- and output data
        int n = 10;
        float srcArrayA[] = new float[n];
        float srcArrayB[] = new float[n];
        float dstArray[] = new float[n];
        for (int i=0; i<n; i++)
        {
            srcArrayA[i] = i;
            srcArrayB[i] = i;
        }
        Pointer srcA = Pointer.to(srcArrayA);
        Pointer srcB = Pointer.to(srcArrayB);
        Pointer dst = Pointer.to(dstArray);

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        System.out.println("number of platforms: " + numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);

        // print info on platforms
        for (int i = 0; i < numPlatforms; i++) {
            System.out.println("platform "+ i + " name: " + getString(platforms[i], CL_PLATFORM_NAME));
            System.out.println("platform "+ i + " vendor: " + getString(platforms[i], CL_PLATFORM_VENDOR));
            System.out.println("platform "+ i + " version: " + getString(platforms[i], CL_PLATFORM_VERSION));
        }

        // Obtain a platform ID
        cl_platform_id platform = platforms[platformIndex];
        System.out.println("select platform: " + platformIndex);

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        System.out.println("number of devices for platform " + platformIndex + ": " + numDevices);

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];
        System.out.println("select device: " + deviceIndex);

        // Create a context for the selected device
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_command_queue commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[3];
        memObjects[0] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * n, srcA, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * n, srcB, null);
        memObjects[2] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_float * n, null, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "sampleKernel", null);

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

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0,
                n * Sizeof.cl_float, dst, 0, null, null);

        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        // Verify the result
        boolean passed = true;
        final float epsilon = 1e-7f;
        for (int i=0; i<n; i++)
        {
            float x = dstArray[i];
            float y = srcArrayA[i] * srcArrayB[i];
            boolean epsilonEqual = Math.abs(x - y) <= epsilon * Math.abs(x);
            if (!epsilonEqual)
            {
                passed = false;
                break;
            }
        }
        System.out.println("Test "+(passed?"PASSED":"FAILED"));
        if (n <= 10)
        {
            System.out.println("Result: "+java.util.Arrays.toString(dstArray));
        }
    }
}

