__kernel void finalScan(
    __global const int *g_scannedInputData, __global int *scannedSums,
    __global int *g_scannedOutputdata)
{

    //add first sum scan value to each value in first workgroup
    //second scan value to each value in second workgroup
    //third scan value to each value in third workgroup
    //fourth scan value to each value in fourth workgroup

    int thid = get_global_id(0);
    int groupId = get_group_id(0);

    g_scannedOutputdata[2*thid] = g_scannedInputData[2*thid] + scannedSums[groupId];
    g_scannedOutputdata[2*thid+1] = g_scannedInputData[2*thid+1] + scannedSums[groupId];
}