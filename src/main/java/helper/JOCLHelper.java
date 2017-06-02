package helper;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;

public class JOCLHelper {

    private final int platformIndex;
    private final long deviceType;
    private final int deviceIndex;
    private cl_command_queue commandQueue;
    private cl_context context;
    private cl_mem[] cl_mems;
    private cl_program program;
    private cl_kernel kernel;
    private List<cl_program> programs = new ArrayList<>();
    private List<cl_kernel> kernels = new ArrayList<>();

    public JOCLHelper(int platformIndex, long deviceType, int deviceIndex) {
        this.platformIndex = platformIndex;
        this.deviceType = deviceType;
        this.deviceIndex = deviceIndex;
    }

    /**
     * Returns the value of the device info parameter with the given name
     *
     * @param device    The device
     * @param paramName The parameter name
     * @return The value
     */
    public static String getDeviceString(cl_device_id device, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
//        clGetDeviceInfo(device, paramName, 0, null, size);

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
    public static String getPlatformName(cl_platform_id platform, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int) size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    public void init() {
        CL.setExceptionsEnabled(true);
    }

    public cl_context createContext() {
        cl_platform_id platform = createPlatform();

        // Obtain a platform ID and init context
        return prepareContext(platform);
    }

    public cl_program createProgram(String codeLines) {
        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{codeLines}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        return program;
    }

    public cl_kernel createKernel(String sampleKernel, cl_program program) {
        return clCreateKernel(program, sampleKernel, null);
    }

    public cl_command_queue getCommandQueue() {
        return commandQueue;
    }

    public cl_context getContext() {
        return context;
    }

    public void executeKernel(cl_kernel kernel, long[] global_work_size, long[] local_work_size, int work_dim) {
        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, work_dim, null,
                global_work_size, local_work_size, 0, null, null);
    }

    private cl_platform_id createPlatform() {
        // Obtain the platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        System.out.println("number of platforms: " + numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);

        // print info on platforms
        for (int i = 0; i < numPlatforms; i++) {
            System.out.println("platform " + i + " name: " + JOCLHelper.getPlatformName(platforms[i], CL_PLATFORM_NAME));
            System.out.println("platform " + i + " vendor: " + JOCLHelper.getPlatformName(platforms[i], CL_PLATFORM_VENDOR));
            System.out.println("platform " + i + " version: " + JOCLHelper.getPlatformName(platforms[i], CL_PLATFORM_VERSION));
        }

        cl_platform_id platform = platforms[platformIndex];
        System.out.println("select platform: " + platformIndex);

        return platform;
    }

    private cl_context prepareContext(cl_platform_id platform) {
        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        cl_device_id device = createDevice(platformIndex, deviceType, deviceIndex, platform);


        // Create a context for the selected device
        context = createContext(contextProperties, device);

        // Create a command-queue for the selected device
        commandQueue = createCommandQueue(device, context);
        return context;
    }

    private cl_command_queue createCommandQueue(cl_device_id device, cl_context context) {
        return clCreateCommandQueue(context, device, 0, null);
    }

    private cl_context createContext(cl_context_properties contextProperties, cl_device_id device) {
        return clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);
    }

    private cl_device_id createDevice(int platformIndex, long deviceType, int deviceIndex, cl_platform_id platform) {
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
        return device;
    }

    public cl_kernel createKernel(String kernelName, String fileName) {
        String sampleKernel = kernelName;
        String codeLines = "";

        try {
            codeLines = FileUtils.readFileToString(new File("src/main/resources/" + fileName), defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        program = this.createProgram(codeLines);
        kernel = this.createKernel(sampleKernel, program);

        programs.add(program);
        kernels.add(kernel);
        return kernel;
    }

    public void releaseMemObjects(cl_mem memObjects[]) {
        for (cl_mem memObject : memObjects) {
            if (memObject != null) {
                clReleaseMemObject(memObject);
            }
        }
    }

    public void releaseKernel(cl_kernel kernel, cl_program program) {
        clReleaseKernel(kernel);
        clReleaseProgram(program);
    }

    public void releaseAndFinish() {
        releaseMemObjects(this.cl_mems);
        for (cl_kernel kernel : this.kernels) {
            clReleaseKernel(kernel);
        }
        for (cl_program program : this.programs) {
            clReleaseProgram(program);
        }
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    public cl_mem[] createManagedMemory(int i) {
        cl_mems = new cl_mem[i];
        return cl_mems;
    }

    public cl_mem createBuffer(long flags, int size, Pointer pointer) {
        return clCreateBuffer(context,
                flags,
                size, pointer, null);
    }


    public void bufferIntoPointer(cl_mem memObject, boolean clTrue, int offset, int size, Pointer dst, int events_wait_list, cl_event o[], cl_event o1) {
        clEnqueueReadBuffer(commandQueue, memObject, clTrue, offset,
                size, dst, events_wait_list, o, o1);
    }

    public void bufferIntoPointer(cl_mem memObject, boolean clTrue, int offset, int size, Pointer dst) {
        bufferIntoPointer(memObject, clTrue, offset,
                size, dst, 0, null, null);
    }

    public void releaseKernel() {
        releaseKernel(kernel, program);
    }


    /**
     * Returns the value of the device info parameter with the given name
     *
     * @param device    The device
     * @param paramName The parameter name
     * @return The value
     */
    public static String getString(cl_device_id device, int paramName) {
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
    public static String getString(cl_platform_id platform, int paramName) {
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
