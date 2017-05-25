__kernel void prescan(
    __global const int *g_idata, __global int *g_odata,
    const int numberOfElements, __local int *temp,  __global int *sums, const int saveSums)
{
    int thid = get_global_id(0);
    int localId = get_local_id(0); //eine dimension, deswegen 0
    int offset = 1;

    temp[2*localId] = g_idata[2*thid]; // load input into shared memory
    temp[2*localId+1] = g_idata[2*thid+1];

    for (int d = numberOfElements>>1; d > 0; d >>= 1)   // build sum in place up the tree
    {
        barrier(CLK_LOCAL_MEM_FENCE); //CLK_LOCAL_MEM_FENCE CLK_GLOBAL_MEM_FENCE
        if (localId < d)
        {
            int ai = offset*(2*localId+1)-1;
            int bi = offset*(2*localId+2)-1;
            temp[bi] += temp[ai];
        }
        offset *= 2;
    }
    //printf("Threadid %d , localId: %d , tempValue %d ,tempValueWithLocalId2 %d \n", thid, localId, temp[localId], temp[localId+1]);

    if (localId == 0) {
        //boolean saveSums
        if (saveSums == 1) {
        //group_id 0 <- erste dimension
        sums[get_group_id(0)] = temp[(get_local_size(0)*2)-1];//localsize 2 (4 elemente 2 threads)
        }
        temp[numberOfElements - 1] = 0;
    } // clear the last element
    for (int d = 1; d < numberOfElements; d *= 2) // traverse down tree & build scan
    {
        offset >>= 1;
        barrier(CLK_LOCAL_MEM_FENCE);
        if (localId < d)
        {
            int ai = offset*(2*localId+1)-1;
            int bi = offset*(2*localId+2)-1;
            float t = temp[ai];
            temp[ai] = temp[bi];
            temp[bi] += t;
        }
    }
    barrier(CLK_LOCAL_MEM_FENCE);

    // wieder thid, weil das element localmemory 0 will ich wieder
    //an global stelle 500.000
    g_odata[2*thid] = temp[2*localId]; // write results to device memory
    g_odata[2*thid+1] = temp[2*localId+1];
}