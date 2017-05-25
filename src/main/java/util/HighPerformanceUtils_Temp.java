package util;

import org.jocl.Pointer;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformInfo;

public class HighPerformanceUtils_Temp {

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
