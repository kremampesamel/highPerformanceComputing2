package hpc_ue_2;


import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;


/*
** kernel doesnt work right now. Trying with work group. But be inspired...
*
 */

public class ParallelScanMassarosOld {

    static String programSource = "";

    public static void main(String args[]) throws IOException {
        programSource = FileUtils.readFileToString(new File("src/main/resources/hpc_2_prescan.cl"), defaultCharset());

        // Create input- and output data
        int srcArrayA[] = new int[]{5, 5, 5, 1, 1, 1, 1, 1};
        int n = srcArrayA.length;

        int destArray[] = new int[8];
        Pointer srcA = Pointer.to(srcArrayA);
        Pointer srcB = Pointer.to(destArray);

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
            System.out.println("platform " + i + " name: " + getString(platforms[i], CL_PLATFORM_NAME));
            System.out.println("platform " + i + " vendor: " + getString(platforms[i], CL_PLATFORM_VENDOR));
            System.out.println("platform " + i + " version: " + getString(platforms[i], CL_PLATFORM_VERSION));
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
        cl_mem memObjects[] = new cl_mem[2];
        memObjects[0] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * n, srcA, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * n, null, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{programSource}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "prescan", null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2,
                Sizeof.cl_int, Pointer.to(new int[]{n}));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{n};
        long local_work_size[] = new long[]{1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                n * Sizeof.cl_int, srcB, 0, null, null);

        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
        for (int i = 0; i < n; i++) {
            System.out.println(destArray[i]);
        }
    }


    /**
     * Returns the value of the device info parameter with the given name
     *
     * @param device    The device
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_device_id device, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int) size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    /**
     * Returns the value of the platform info parameter with the given name
     *
     * @param platform  The platform
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_platform_id platform, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int) size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

}

