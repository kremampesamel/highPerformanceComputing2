__kernel void radix_merge(
	__global const int *g_input,
	__global int *g_out,
	const int num_total, const int bucket_size) {

	int thid = get_global_id(0);

	int left_start = thid*(bucket_size*2);
	int right_start = left_start + bucket_size;
	int bound = (thid+1)*(bucket_size*2);

	if (bound >= num_total) {
		bound = num_total;
	}

	int index_l = left_start;
	int index_r = right_start;

	int take_val = 0;

	for (int i = 0; i < 2*bucket_size; i++) {
		int elem_l;
		int elem_r;

		if (index_l >= right_start && index_r >= bound) {
			break;
		}

		if (index_l >= right_start) {
			elem_r = g_input[index_r];
			take_val = elem_r;
            index_r++;
		} else if (index_r >= bound) {
			elem_l = g_input[index_l];
			take_val = elem_l;
            index_l++;
		} else {
			elem_l = g_input[index_l];
            elem_r = g_input[index_r];
            if (elem_l <= elem_r) {
                take_val = elem_l;
                index_l++;
            } else {
                take_val = elem_r;
                index_r++;
            }
		}
		g_out[left_start+i] = take_val;
	}
}