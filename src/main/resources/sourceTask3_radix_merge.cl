__kernel void radix_merge(__global int *global_data, const int k, const int num_buckets, const int num_total, const int bucket_size, __local int* tmp) {

	int thid = get_global_id(0);
	int lid = get_local_id(0);

	int num_global = get_global_size(0);
	int num_local = get_local_size(0);
	int gid = get_group_id(0);

	int tmp_index = bucket_size*2*lid;

	int left_start = thid*(bucket_size*2);
	int right_start = left_start + bucket_size;
	int bound = (thid+1)*(bucket_size*2);

	if (bound >= num_total) {
		bound = num_total;
	}

	// each last local thread must watch out for this neighbour
	//if (thid % num_local == num_local - 1) {
		printf("last: k=%d thid=%d num_local=%d gid=%d",k,thid, num_local, gid);
		printf("  index: left=%d right=%d size=%d \n",left_start, right_start, bucket_size);
	//}

	//int tmp [2*bucket_size] = 0;
	int index_l = left_start;
	int index_r = right_start;
	for (int i = 0; i < 2*bucket_size, index_l < right_start, index_r < bound; i++) {
		int elem_l;
		int elem_r;

		if (index_l < right_start && index_r < bound) {

			elem_l = global_data[index_l];
			elem_r = global_data[index_r];
			if (elem_l <= elem_r) {
				tmp[tmp_index + i] = elem_l;
				index_l++;
			} else {
				tmp[tmp_index + i] = elem_r;
	            index_r++;
			}
		} else {

			if (index_l >= right_start && index_r >= bound) {
				break;
			}
			if (index_l >= right_start) {
				elem_r = global_data[index_r];
				tmp[tmp_index + i] = elem_r;
                index_r++;
			} else {
				elem_l = global_data[index_l];
				tmp[tmp_index + i] = elem_l;
            	index_l++;
			}
		}
		printf("tid=%d tti=%d i=%d siz:%d inl:%d inr:%d val:%d  \n",thid,tmp_index,i,bucket_size,index_l, index_r,tmp[i]);
	}
	// write
	for (int i = 0; i < 2*bucket_size, (left_start  + 1) < num_total; i++) {
		global_data[left_start + i] = tmp[tmp_index + i];
		//printf("i=%d: %d    ",left_start+i,tmp[tmp_index + i]);
	}
}