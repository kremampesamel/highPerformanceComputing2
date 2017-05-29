__kernel void radix_sort8(
	__global const int *global_data,
	__global int* out
	) {
    typedef union {
        int vec;
        int array[8];
    } vec_array;

    int one_count, zero_count;
    int cmp_value = 1;
    vec_array mask, ones, data;

    data.vec = global_data[0];

    for(int i=0; i<3; i++) {
        zero_count = 0;
        one_count = 0;
        for(int j=0; j < 8; j++) {
            if (data.array[j] & cmp_value) {
                ones.array[one_count++] == data.array[j];
            } else {
                mask.array[zero_count++] = j;
            }
        }
        // created sorted vector
        for (int j= zero_count; j < 8; j++) {
            mask.array[j] = 8 - zero_count + j;
        }
        //data.vec = shuffle2(data.vec, ones.vec, mask.vec);
        cmp_value <<= 1;
    }
    //global_data[0] = data.vec;
    out[0] = 42;
}