__kernel void addScannedSums(
    __global const int *g_scannedInputData, __global int *scannedSums,
    __global int *g_scannedOutputdata)
{

    //add first sum scan value to each value in first workgroup
    //second scan value to each value in second workgroup
    //third scan value to each value in third workgroup
    //fourth scan value to each value in fourth workgroup

    int thid = get_global_id(0);
    int localId = get_local_id(0);

    g_scannedOutputdata[thid] = g_scannedInputData[localId] + scannedSums[localId];
    g_scannedOutputdata[thid+1] = g_scannedInputData[localId+1] + scannedSums[localId+1];

}