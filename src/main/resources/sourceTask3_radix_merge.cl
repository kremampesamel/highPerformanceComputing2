__kernel void radix_merge(
	__global const int *g_input,
	__global int *g_out,
	const int k, const int num_buckets, const int num_total, const int bucket_size) {

	int thid = get_global_id(0);
	//int lid = get_local_id(0);

	//int num_global = get_global_size(0);
	//int num_local = get_local_size(0);
	//int gid = get_group_id(0);

//	int tmp_index = bucket_size*2*lid;

	int left_start = thid*(bucket_size*2);
	int right_start = left_start + bucket_size;
	int bound = (thid+1)*(bucket_size*2);

	if (bound >= num_total) {
		bound = num_total;
	}

	// each last local thread must watch out for this neighbour
	//if (thid % num_local == num_local - 1) {
		//printf("last: k=%d thid=%d num_local=%d gid=%d",k,thid);
		//printf("  index: left=%d right=%d size=%d \n",left_start, right_start, bucket_size);
	//}

	int index_l = left_start;
	int index_r = right_start;

	int take_val = 0;

	for (int i = 0; i < 2*bucket_size; i++) {
		int elem_l;
		int elem_r;

		if (index_l >= right_start && index_r >= bound) {
			//printf("bound: %d %d %d",index_l,index_r, bound);
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
		//printf("     : %d %d %d - %d \n",index_l,index_r, bound, take_val);
		//tmp[tmp_index + i] = elem_r;
		g_out[left_start+i] = take_val;
		//printf("take_val; i=%d val=%d g_out[left_start+i]=%d",i, take_val, g_out[left_start+i]);
		//barrier(CLK_LOCAL_MEM_FENCE );
		//printf("pre %d",i);
		//printf("tid=%d tti=%d i=%d siz:%d inl:%d inr:%d val:%d  \n",thid,tmp_index,i,bucket_size,index_l, index_r,tmp[i]);
		//printf("post %d",i);
	}

	//printf("thid: %d, finish %d items", thid, bucket_size);
	//arrier(CLK_GLOBAL_MEM_FENCE );
	//printf("thid: %d, check %d items", thid, bucket_size);
}