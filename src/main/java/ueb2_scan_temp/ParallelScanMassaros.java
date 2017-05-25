package ueb2_scan_temp;

import hpc_ue_2.SequentialScan;
import org.apache.commons.io.FileUtils;
import org.jocl.*;
import util.HighPerformanceUtils_Temp;

import java.io.File;
import java.util.Random;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;

public class ParallelScanMassaros {

    static String programSource = "";

    static String programSourceFinalScan = "";

    public static void main(String args[]) throws Exception {
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
        int numberOfElements = 128;
        int numberOfWorkgroups = 4;

        long global_work_size[] = new long[]{numberOfElements / 2};//anzahl der threads
        long local_work_size[] = new long[]{(numberOfElements / 2) / numberOfWorkgroups};//groß wie die workgroup

        int[] inputDataArray = createInputData(numberOfElements);
        Pointer inputDataPointer = Pointer.to(inputDataArray);

        int outputOfFirstScanArray[] = new int[numberOfElements];
        Pointer outputOfFirstScanPointer = Pointer.to(outputOfFirstScanArray);

        int workgroupSumsArray[] = new int[numberOfWorkgroups];
        Pointer workgroupSumsPointer = Pointer.to(workgroupSumsArray);

        int workgroupSumsScannedArray[] = new int[numberOfWorkgroups];
        Pointer workgroupSumsScannedPointer = Pointer.to(workgroupSumsScannedArray);

        int finalScannedArray[] = new int[numberOfElements];
        Pointer finalScannedArrayPointer = Pointer.to(finalScannedArray);

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[5];
        memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * numberOfElements, inputDataPointer, null);
        memObjects[1] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//first scan result
        memObjects[2] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//sums

        //memory object for scan sum
        memObjects[3] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//scan sums

        //memory object for final scan
        memObjects[4] = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * numberOfElements, null, null);//first scan result

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
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0, numberOfElements * Sizeof.cl_int, outputOfFirstScanPointer, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, workgroupSumsPointer, 0, null, null);

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
        clEnqueueReadBuffer(commandQueue, memObjects[3], CL_TRUE, 0, numberOfWorkgroups * Sizeof.cl_int, workgroupSumsScannedPointer, 0, null, null);

        // set kernel arguments final scan
        clSetKernelArg(kernelFinalScan, 0, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernelFinalScan, 1, Sizeof.cl_mem, Pointer.to(memObjects[3]));
        clSetKernelArg(kernelFinalScan, 2, Sizeof.cl_mem, Pointer.to(memObjects[4]));

        //Execute the kernel final time
        clEnqueueNDRangeKernel(commandQueue, kernelFinalScan, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data final time
        clEnqueueReadBuffer(commandQueue, memObjects[4], CL_TRUE, 0, numberOfElements * Sizeof.cl_int, finalScannedArrayPointer, 0, null, null);

        // Release kernel, program, and memory objects
        releaseResources(context, commandQueue, memObjects, program, kernel, programFinalScan, kernelFinalScan);

        verifyAndPrintResults(inputDataArray, outputOfFirstScanArray, workgroupSumsArray, workgroupSumsScannedArray, finalScannedArray);
    }

    private static int[] createInputData(int numberOfElements) {
        int[] inputData = new int[numberOfElements];
        Random random = new Random();
        for(int i = 0; i < inputData.length; i++) {
            inputData[i] = random.nextInt(11);
        }
        return inputData;
    }

    private static void verifyAndPrintResults(int[] inputData, int[] outputOfFirstScanArray, int[] workgroupSumsArray, int[] workgroupSumsScannedArray, int[] finalScannedArray) throws Exception {
        System.out.println("Input data:");
        for (int i = 0; i < inputData.length; i++) {
            System.out.print(inputData[i] + " ");
        }

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

        verifyResult(inputData, finalScannedArray);
    }

    private static void verifyResult(int[] inputDataArray, int[] finalScannedArray) throws Exception {
        int[] sequentialScanResult = SequentialScan.executeScanForElements(inputDataArray);

        for(int i = 0; i < finalScannedArray.length; i++) {
            if(sequentialScanResult[i] != finalScannedArray[i]) {
                throw new Exception("did not work correct");
            }
        }
        System.out.println("\nSequential scanned array:");
        for (int i = 0; i < sequentialScanResult.length; i++) {
            System.out.print(sequentialScanResult[i] + " ");
        }
    }

    private static void releaseResources(cl_context context, cl_command_queue commandQueue, cl_mem[] memObjects, cl_program program, cl_kernel kernel, cl_program program2, cl_kernel kernel2) {
        releaseMemoryObjects(memObjects);
        clReleaseKernel(kernel);
        clReleaseKernel(kernel2);
        clReleaseProgram(program);
        clReleaseProgram(program2);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    private static void releaseMemoryObjects(cl_mem[] memObjects) {
        for (int i = 0; i < memObjects.length; i++) {
            clReleaseMemObject(memObjects[i]);
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
            System.out.println("platform " + i + " name: " + HighPerformanceUtils_Temp.getString(platforms[i], CL_PLATFORM_NAME));
            System.out.println("platform " + i + " vendor: " + HighPerformanceUtils_Temp.getString(platforms[i], CL_PLATFORM_VENDOR));
            System.out.println("platform " + i + " version: " + HighPerformanceUtils_Temp.getString(platforms[i], CL_PLATFORM_VERSION));
        }
        return platforms;
    }

}