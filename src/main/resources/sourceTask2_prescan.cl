__kernel void prescan(
    __global const int *g_idata, __global int *g_odata,
    const int numberOfElements, __local int *temp,  __global int *sums, const int saveSums)
{
    int thid = get_global_id(0);
    int localId = get_local_id(0); //one dimension
    int offset = 1;

    temp[2*localId] = g_idata[2*thid]; // load input into shared memory
    temp[2*localId+1] = g_idata[2*thid+1];

    for (int d = numberOfElements>>1; d > 0; d >>= 1)   // build sum in place up the tree
    {
        barrier(CLK_LOCAL_MEM_FENCE);
        if (localId < d)
        {
            int ai = offset*(2*localId+1)-1;
            int bi = offset*(2*localId+2)-1;
            temp[bi] += temp[ai];
        }
        offset *= 2;
    }
    if (localId == 0) {
        if (saveSums == 1) {
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

    g_odata[2*thid] = temp[2*localId];
    g_odata[2*thid+1] = temp[2*localId+1];
}