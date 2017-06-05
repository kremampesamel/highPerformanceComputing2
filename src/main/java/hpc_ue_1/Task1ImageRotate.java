package hpc_ue_1;

import helper.JOCLHelper;
import org.apache.commons.io.FileUtils;
import org.jocl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.jocl.CL.*;

public class Task1ImageRotate {

    static BufferedImage inputImage;

    static BufferedImage outputImage;

    static String programSource = "";

    public static void main(String args[]) throws IOException {
        float rotation = -40.0f;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        programSource = FileUtils.readFileToString(new File("src/main/resources/sourceTask1.cl"), defaultCharset());

        inputImage = ImageIO.read(new File("lena512color.png"));
        outputImage = new BufferedImage(
                inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        int srcArrayA[] = new int[inputImage.getWidth() * inputImage.getHeight()];
        int dstArray[] = new int[inputImage.getWidth() * inputImage.getHeight()];

        inputImage.getRGB(0, 0, inputImage.getWidth(), inputImage.getHeight(), srcArrayA, 0, inputImage.getWidth());

        Pointer srcA = Pointer.to(srcArrayA);
        Pointer dst = Pointer.to(dstArray);

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Obtain the platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        System.out.println("number of platforms: " + numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);

        // print info on platforms
        for (int i = 0; i < numPlatforms; i++) {
            System.out.println("platform " + i + " name: " + JOCLHelper.getString(platforms[i], CL_PLATFORM_NAME));
            System.out.println("platform " + i + " vendor: " + JOCLHelper.getString(platforms[i], CL_PLATFORM_VENDOR));
            System.out.println("platform " + i + " version: " + JOCLHelper.getString(platforms[i], CL_PLATFORM_VERSION));
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
                Sizeof.cl_float * inputImage.getWidth() * inputImage.getHeight(), srcA, null);
        memObjects[1] = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_float * inputImage.getWidth() * inputImage.getHeight(), null, null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{programSource}, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "image_rotate", null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0,
                Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1,
                Sizeof.cl_mem, Pointer.to(memObjects[1]));

        clSetKernelArg(kernel, 2,
                Sizeof.cl_int, Pointer.to(new int[]{inputImage.getWidth()}));

        clSetKernelArg(kernel, 3,
                Sizeof.cl_int, Pointer.to(new int[]{inputImage.getHeight()}));

        clSetKernelArg(kernel, 4,
                Sizeof.cl_float, Pointer.to(new float[]{(float) Math.sin(Math.toRadians(rotation))}));

        clSetKernelArg(kernel, 5,
                Sizeof.cl_float, Pointer.to(new float[]{(float) Math.cos(Math.toRadians(rotation))}));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{inputImage.getWidth(), inputImage.getHeight()};
        long local_work_size[] = new long[]{1, 1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                inputImage.getWidth() * inputImage.getHeight() * Sizeof.cl_int, dst, 0, null, null);

        outputImage.setRGB(0, 0, outputImage.getWidth(), outputImage.getHeight(), dstArray, 0, outputImage.getWidth());

        File outputfile = new File("rotated.png");
        ImageIO.write(outputImage, "png", outputfile);

        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

}