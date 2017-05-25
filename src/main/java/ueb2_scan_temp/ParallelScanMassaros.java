package ueb2_scan_temp;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;

public class ParallelScanMassaros {

    static String programSource = "";

    static String programSourceFinalScan = "";

    public static void main(String args[]) throws IOException {
        programSource = FileUtils.readFileToString(new File("src/main/resources/sourceTask2.cl"), defaultCharset());
        programSourceFinalScan = FileUtils.readFileToString(new File("src/main/resources/sourceTask2_addingScannedSums.cl"), defaultCharset());

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
        cl_program programFinalScan = clCreateProgramWithSource(context, 1, new String[]{programSourceFinalScan}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        clBuildProgram(programFinalScan, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "prescan", null);
        cl_kernel kernelFinalScan = clCreateKernel(programFinalScan, "addScannedSums", null);

        // Create input- and output data
        int numberOfElements = 16;
        int[] inputDataArray = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        Pointer inputDataPointer = Pointer.to(inputDataArray);

        int outputOfFirstScanArray[] = new int[numberOfElements];
        Pointer pointerToOutputOfFirstScan = Pointer.to(outputOfFirstScanArray);

        int numberOfWorkgroups = 4;
        int workgroupSumsArray[] = new int[numberOfWorkgroups];
        int workgroupSumsScannedArray[] = new int[numberOfWorkgroups];

        Pointer pointerWorkgroupSums = Pointer.to(workgroupSumsArray);
        Pointer pointerWorkgroupSumsScanned = Pointer.to(workgroupSumsScannedArray);

        int finalScannedArray[] = new int[numberOfElements];
        Pointer pointerToFinalScannedArray = Pointer.to(finalScannedArray);

        long global_work_size[] = new long[]{numberOfElements / 2};//anzahl der threads
        long local_work_size[] = new long[]{(numberOfElements / 2) / numberOfWorkgroups};//groß wie die workgroup

        // Allocate the memory objects for the input- and output data
        cl_mem memObjectsInputDataScan[] = new cl_mem[3];
        memObjectsInputDataScan[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, inputDataPointer, null);
        memObjectsInputDataScan[1] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//first scan result
        memObjectsInputDataScan[2] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//sums

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjectsInputDataScan[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjectsInputDataScan[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfElements}));
        clSetKernelArg(kernel, 3, numberOfElements * Sizeof.cl_int, null);// per workgroup temp memory
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObjectsInputDataScan[2]));
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{1}));//save == 1, later change to boolean

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjectsInputDataScan[1], CL_TRUE, 0, numberOfElements * Sizeof.cl_int, pointerToOutputOfFirstScan, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjectsInputDataScan[2], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, pointerWorkgroupSums, 0, null, null);

        cl_mem memObjectsSumScan[] = new cl_mem[1];
        memObjectsSumScan[0] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//scan sums

        // set kernel arguments 2nd time
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjectsInputDataScan[2]));//sums
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjectsSumScan[0]));//scanSums
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{numberOfWorkgroups}));
        clSetKernelArg(kernel, 3, numberOfWorkgroups * Sizeof.cl_int, null);//temp array
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, null);//will not be used
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{0}));// 0 means we are not saving the sums

        //Execute the kernel second time
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data 2nd time
        clEnqueueReadBuffer(commandQueue, memObjectsSumScan[0], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, pointerWorkgroupSumsScanned, 0, null, null);

        cl_mem memObjectsFinalScan[] = new cl_mem[1];
        memObjectsFinalScan[0] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//first scan result

        // set kernel arguments final scan
        clSetKernelArg(kernelFinalScan, 0, Sizeof.cl_mem, Pointer.to(memObjectsInputDataScan[1]));
        clSetKernelArg(kernelFinalScan, 1, Sizeof.cl_mem, Pointer.to(memObjectsSumScan[0]));
        clSetKernelArg(kernelFinalScan, 2, Sizeof.cl_mem, Pointer.to(memObjectsFinalScan[0]));//we are going to write the results here

        //Execute the kernel final time
        clEnqueueNDRangeKernel(commandQueue, kernelFinalScan, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data final time
        clEnqueueReadBuffer(commandQueue, memObjectsFinalScan[0], CL_TRUE, 0, numberOfElements * Sizeof.cl_int, pointerToFinalScannedArray, 0, null, null);

        // Release kernel, program, and memory objects
        releaseResources(context, commandQueue, memObjectsInputDataScan, memObjectsSumScan, memObjectsFinalScan, program, kernel, programFinalScan, kernelFinalScan);

        printResults(outputOfFirstScanArray, workgroupSumsArray, workgroupSumsScannedArray, finalScannedArray);
    }

    private static void printResults(int[] outputOfFirstScanArray, int[] workgroupSumsArray, int[] workgroupSumsScannedArray, int[] finalScannedArray) {
        System.out.println("Scan result:");
        for (int i = 0; i < outputOfFirstScanArray.length; i++) {
            System.out.print(outputOfFirstScanArray[i] + " ");
        }

        System.out.println("\nSum result:");
        for (int i = 0; i < workgroupSumsArray.length; i++) {
            System.out.print(workgroupSumsArray[i] + " ");
        }

        System.out.println("\nSum workgroup scan result:");
        for (int i = 0; i < workgroupSumsScannedArray.length; i++) {
            System.out.print(workgroupSumsScannedArray[i] + " ");
        }

        System.out.println("\nFinal scanned array:");
        for (int i = 0; i < finalScannedArray.length; i++) {
            System.out.print(finalScannedArray[i] + " ");
        }
    }

    private static void releaseResources(cl_context context, cl_command_queue commandQueue, cl_mem[] memObjectsInputDataScan, cl_mem[] memObjectsSumScan, cl_mem[] memObjectsFinalScan, cl_program program, cl_kernel kernel, cl_program program2, cl_kernel kernel2) {
        releaseMemoryObjects(memObjectsInputDataScan, memObjectsSumScan, memObjectsFinalScan);
        clReleaseKernel(kernel);
        clReleaseKernel(kernel2);
        clReleaseProgram(program);
        clReleaseProgram(program2);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    private static void releaseMemoryObjects(cl_mem[] memObjectsInputDataScan, cl_mem[] memObjectsSumScan, cl_mem[] memObjectsFinalScan) {
        for (int i = 0; i < memObjectsInputDataScan.length; i++) {
            clReleaseMemObject(memObjectsInputDataScan[i]);
        }
        for (int i = 0; i < memObjectsSumScan.length; i++) {
            clReleaseMemObject(memObjectsSumScan[i]);
        }
        for (int i = 0; i < memObjectsFinalScan.length; i++) {
            clReleaseMemObject(memObjectsFinalScan[i]);
        }
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
