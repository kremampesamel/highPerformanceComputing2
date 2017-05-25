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
        int[] srcArrayA = createInputArray(numberOfElements);

        int srcArrayFirstScan[] = new int[numberOfElements];
        int numberOfWorkgroups = 4;
        int srcArraySums[] = new int[numberOfWorkgroups];
        int srcArraySumsScanned[] = new int[numberOfWorkgroups];

        Pointer pointerInputData = Pointer.to(srcArrayA);
        Pointer pointerFirstScan = Pointer.to(srcArrayFirstScan);
        Pointer pointerSums = Pointer.to(srcArraySums);
        Pointer pointerSumsScanned = Pointer.to(srcArraySumsScanned);

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
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_command_queue commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[4];
        memObjects[0] = clCreateBuffer(context,
                CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * numberOfElements, pointerInputData, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * numberOfElements, null, null);
        memObjects[2] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * numberOfElements, null, null);
        memObjects[3] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * numberOfElements, null, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{programSource}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "prescan", null);

        // Set the arguments for the kernel
        setKernelArgumentsFirstTime(numberOfElements, memObjects, kernel);

        //[1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 ] = 16 elements
        //global work size = n/2 = 8 (ein thread 2 elemente
        //numberOfWorkgroups = 2 (global size muss vielfaches von lokal size sein)
        // local work size =  4  (n/2)/numberOfWorkgroups    // power of 2
        //global size muss ein ganzzahliges vielfaches der lokal size sein

        // Set the work-item dimensions
        long global_work_size[] = new long[]{numberOfElements/2};//anzahl der threads
        //jeder thread 2 elemente, deswegen nur halb so viele threads

        //wie viele threads sind eine workgroup?
        long local_work_size[] = new long[]{(numberOfElements/2)/numberOfWorkgroups};//groß wie die workgroup
        // 8 länge
        // 8 elemente , global work size=4
        // local work size größe der workgroup also 8

        //somit bei n/2, n/2 hab ich nureine workgroup

        //bei global=4, local=2
        //hab ich 2 workgroups(jeweils mit 2 threads, 2*2=4

        // Execute the kernel
        executeKernelAndGetResultsFirstTime(numberOfElements, numberOfWorkgroups, pointerFirstScan, pointerSums, commandQueue, memObjects, kernel, global_work_size, local_work_size);



        // set kernel arguments 2nd time
        //correct these

        //g_idata= sums         0 argument
        //g_odata = empty array contains sums scan     argument 1
        // number of elements argument 2
        // temp array argument 3

        /*
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[4]));
        clSetKernelArg(kernel, 2,
                Sizeof.cl_int, Pointer.to(new int[]{srcArraySumsScanned.length}));
        //temp array
        clSetKernelArg(kernel, 3,
                numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory

        //following will not be used
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, null);

        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{0}));


        //Execute the kernel second time
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data 2nd time
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                numberOfElements * Sizeof.cl_int, pointerSumsScanned, 0, null, null);
        */

        // Release kernel, program, and memory objects
        releaseResources(context, commandQueue, memObjects, program, kernel);

        System.out.println("Scan result:");
        for (int i = 0; i < srcArrayFirstScan.length; i++) {
            System.out.print(srcArrayFirstScan[i] + " ");
        }

        System.out.println("\nSum result:");
        // 1 1 1 1 | 1 1 1 1 | 1 1 1 1 | 1 1 1 1 "
        //4 | 4 | 4 | 4
        //summe der jeweiligen workgroup
        for (int i = 0; i < srcArraySums.length; i++) {
            System.out.print(srcArraySums[i] + " ");
        }

        //sumKernel
        //[0, 4, 8, 12]
        //[0 1 2 3 | 0 1 2 3 | 0 1 2 3 | 0 1 2 3]
    }

    private static void executeKernelAndGetResultsFirstTime(int numberOfElements, int numberOfWorkgroups, Pointer srcFirstScan, Pointer srcSums, cl_command_queue commandQueue, cl_mem[] memObjects, cl_kernel kernel, long[] global_work_size, long[] local_work_size) {
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                numberOfElements * Sizeof.cl_int, srcFirstScan, 0, null, null);

        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0,
                numberOfWorkgroups * Sizeof.cl_int, srcSums, 0, null, null);
    }

    private static int[] createInputArray(int numberOfElements) {
        int srcArrayA[] = new int[numberOfElements];
        srcArrayA[0] = 1;
        srcArrayA[1] = 1;
        srcArrayA[2] = 1;
        srcArrayA[3] = 1;
        srcArrayA[4] = 1;
        srcArrayA[5] = 1;
        srcArrayA[6] = 1;
        srcArrayA[7] = 1;
        srcArrayA[8] = 1;
        srcArrayA[9] = 1;
        srcArrayA[10] = 1;
        srcArrayA[11] = 1;
        srcArrayA[12] = 1;
        srcArrayA[13] = 1;
        srcArrayA[14] = 1;
        srcArrayA[15] = 1;
        return srcArrayA;
    }

    private static void setKernelArgumentsFirstTime(int numberOfElements, cl_mem[] memObjects, cl_kernel kernel) {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernel, 3, numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjects[2]));

        int saveSums = 1;
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{saveSums}));//save == 1, later change to boolean
    }

    private static void releaseResources(cl_context context, cl_command_queue commandQueue, cl_mem[] memObjects, cl_program program, cl_kernel kernel) {
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        //clReleaseMemObject(memObjects[3]);
        //clReleaseMemObject(memObjects[4]);
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
