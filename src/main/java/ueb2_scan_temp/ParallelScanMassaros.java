package ueb2_scan_temp;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;

public class ParallelScanMassaros {

    static String programSource = "";

    public static void main(String args[]) throws IOException {
        programSource = FileUtils.readFileToString(new File("src/main/resources/sourceTask2.cl"), defaultCharset());

        // Create input- and output data
        int numberOfElements = 16;
        int[] srcArrayA = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

        int srcArrayFirstScan[] = new int[numberOfElements];
        int numberOfWorkgroups = 4;
        int srcArrayWorkgroupSums[] = new int[numberOfWorkgroups];
        int srcArrayWorkgroupSumsScanned[] = new int[numberOfWorkgroups];

        Pointer pointerInputData = Pointer.to(srcArrayA);
        Pointer pointerFirstScan = Pointer.to(srcArrayFirstScan);
        Pointer pointerSums = Pointer.to(srcArrayWorkgroupSums);
        Pointer pointerSumsScanned = Pointer.to(srcArrayWorkgroupSumsScanned);

        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the platforms
        cl_platform_id[] platforms = obtainThePlatforms();

        // Obtain a platform ID
        cl_platform_id platform = obtainPlatformId(platformIndex, platforms[platformIndex]);

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        cl_device_id device = obtainTheDevice(platformIndex, deviceType, deviceIndex, platform);

        // Create a context for the selected device
        cl_context context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);

        // Create a command-queue for the selected device
        cl_command_queue commandQueue = clCreateCommandQueue(context, device, 0, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "prescan", null);

        long global_work_size[] = new long[]{numberOfElements/2};//anzahl der threads

        long local_work_size[] = new long[]{(numberOfElements/2)/numberOfWorkgroups};//groß wie die workgroup

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[4];
        memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, pointerInputData, null);
        memObjects[1] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//first scan result
        memObjects[2] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//sums

        //second call of scan with the sums, empty array to save the scan sums
        memObjects[3] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//scan sums


        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernel, 3, numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{1}));//save == 1, later change to boolean

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0, numberOfElements * Sizeof.cl_int, pointerFirstScan, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, pointerSums, 0, null, null);

        // set kernel arguments 2nd time
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[2]));//sums
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[3]));//scanSums
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfWorkgroups}));
        clSetKernelArg(kernel, 3, numberOfWorkgroups * Sizeof.cl_int, null);//temp array
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, null);//will not be used
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{0}));// 0 means we are not saving the sums

        //Execute the kernel second time
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data 2nd time
        clEnqueueReadBuffer(commandQueue, memObjects[3], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, pointerSumsScanned, 0, null, null);

        // Release kernel, program, and memory objects
        releaseResources(context, commandQueue, memObjects, program, kernel);

        System.out.println("Scan result:");
        for (int i = 0; i < srcArrayFirstScan.length; i++) {
            System.out.print(srcArrayFirstScan[i] + " ");
        }

        System.out.println("\nSum result:");
        for (int i = 0; i < srcArrayWorkgroupSums.length; i++) {
            System.out.print(srcArrayWorkgroupSums[i] + " ");
        }

        System.out.println("\nSum workgroup scan result:");
        for (int i = 0; i < srcArrayWorkgroupSumsScanned.length; i++) {
            System.out.print(srcArrayWorkgroupSumsScanned[i] + " ");
        }
    }

    private static void releaseResources(cl_context context, cl_command_queue commandQueue, cl_mem[] memObjects, cl_program program, cl_kernel kernel) {
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseMemObject(memObjects[3]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    private static cl_device_id obtainTheDevice(int platformIndex, long deviceType, int deviceIndex, cl_platform_id platform) {
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        System.out.println("number of devices for platform " + platformIndex + ": " + numDevices);

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];
        System.out.println("select device: " + deviceIndex);
        return device;
    }

    private static cl_platform_id obtainPlatformId(int platformIndex, cl_platform_id platform1) {
        cl_platform_id platform = platform1;
        System.out.println("select platform: " + platformIndex);
        return platform;
    }

    private static cl_platform_id[] obtainThePlatforms() {
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
        return platforms;
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





//
// harcoden im kernel   x > 2
///oder als funktion die ich im kernel aufrufe
