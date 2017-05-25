__kernel void prescan(
    __global const int *g_idata, __global int *g_odata,
    const int n, __local int *temp)
{

    int thid = get_global_id(0);
    int localId = get_local_id(0); //eine dimension, deswegen 0
    int offset = 1;

    temp[2*localId] = g_idata[2*thid]; // load input into shared memory
    temp[2*localId+1] = g_idata[2*thid+1];

    for (int d = n>>1; d > 0; d >>= 1)   // build sum in place up the tree
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
              if (localId == 0) { temp[n - 1] = 0; } // clear the last element

              for (int d = 1; d < n; d *= 2) // traverse down tree & build scan
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


//die letzten elemente rauskopieren in eigenen buffer (weiterer)
//den nochmal scannen und intelligent draufaddieren