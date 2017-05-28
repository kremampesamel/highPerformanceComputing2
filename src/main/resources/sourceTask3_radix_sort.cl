__kernel void radix_sort8(__global ushorts *global_data) {

    typedef union {
        ushort vec;
        ushort array[8];
    } vec_array;

    uint one_count, zero_count;
    uint cmp_value = 1;
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
    }
}