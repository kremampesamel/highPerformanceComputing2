/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hpc_ue_1;

/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

//package org.jocl.samples;

import org.jocl.*;
import util.JOCLHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.jocl.CL.*;

/**
 * A small JOCL sample.
 */
public class ImageRotate {

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

        // read image
        String path = "D:\\Google Drive\\FH\\Jetzt\\HPC\\UE\\image-slider2.jpg";
        File inputFile = new File(path);
        File outputFile = new File(path + "-new.jpg");

        BufferedImage in = null;
        try {
            in = ImageIO.read(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int[] inputArray = null;
        int imageH = in.getHeight();
        int imageW = in.getWidth();
        inputArray = in.getRGB(0, 0, imageW, imageH, null, 0, imageW);

        int[] outputArray = new int[inputArray.length];
        BufferedImage newImage = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_RGB);

        Pointer src = Pointer.to(inputArray);
        Pointer dst = Pointer.to(outputArray);

        cl_context context = jocl.createContext();

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = jocl.createManagedMemory(3);
        memObjects[0] = jocl.createBuffer(CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * inputArray.length, src);
        memObjects[1] = jocl.createBuffer(CL_MEM_READ_WRITE, Sizeof.cl_int * outputArray.length, null);

        cl_kernel kernel = jocl.createKernel("image_rotate", "hpc_1_image_rotate.cl");
        double rotation = 45.0;
        float floatSin = (float) Math.sin(Math.toRadians(rotation));
        float floatCos = (float) Math.cos(Math.toRadians(rotation));

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{imageW}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{imageH}));
        clSetKernelArg(kernel, 4, Sizeof.cl_float, Pointer.to(new float[]{floatSin}));
        clSetKernelArg(kernel, 5, Sizeof.cl_float, Pointer.to(new float[]{floatCos}));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{imageW, imageH};
        long local_work_size[] = new long[]{1, 1};
        int work_dim = 2;
        jocl.executeKernel(kernel, global_work_size, local_work_size, work_dim);

        // Read the output data
        jocl.bufferIntoPointer(memObjects[1], CL_TRUE, 0, Sizeof.cl_int * imageH * imageW, dst);

        // Release kernel, program, and memory objects
        //jocl.releaseKernel(kernel, program);
        jocl.releaseKernel();
        jocl.releaseAndFinish();

        String s = dst.toString();

        newImage.setRGB(0, 0, imageW, imageH, outputArray, 0, imageW);
        try {
            boolean write = ImageIO.write(newImage, "jpg", outputFile);

            System.out.println(String.format("Writing file: %s success: %s", outputFile.getAbsoluteFile(), write));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
