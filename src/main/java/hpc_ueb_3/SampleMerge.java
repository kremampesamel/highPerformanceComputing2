package hpc_ueb_3;

public class SampleMerge {

	public int global_id = 0;
	public int local_id = 0;
	public int global_size = 128;
	public int local_size = 64;
	public int group_id = 0;

	public SampleMerge(int global_id, int local_id, int global_size, int local_size) {
		this.global_id = global_id;
		this.local_id = local_id;
		this.global_size = global_size;
		this.local_size = local_size;
	}

	public void radix_merge(int[] global_data, final int k, final int num_buckets, final int num_total, final int bucket_size, int[] tmp) {

		int thid = get_global_id(0);
		int lid = get_local_id(0);

		int num_global = get_global_size(0);
		int num_local = get_local_size(0);
		int gid = get_group_id(0);

		int tmp_index = bucket_size * 2 * lid;

		int left_start = thid * (bucket_size * 2);
		int right_start = left_start + bucket_size;
		int bound = (thid + 1) * (bucket_size * 2);

		if (bound >= num_total) {
			bound = num_total;
		}

		// each last local thread must watch out for this neighbour
		//if (thid % num_local == num_local - 1) {
		printf("last: k=%d thid=%d num_local=%d gid=%d", k, thid, num_local, gid);
		printf("  index: left=%d right=%d size=%d \n", left_start, right_start, bucket_size);
		//}

		//int tmp [2*bucket_size] = 0;
		int index_l = left_start;
		int index_r = right_start;
		for (int i = 0; i < 2 * bucket_size; i++) {
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
			printf(String.format("tid=%d tti=%d i=%d siz:%d inl:%d inr:%d val:%d  \n", thid, tmp_index, i, bucket_size, index_l, index_r, tmp[i]));
		}
		// write
		for (int i = 0; i < 2 * bucket_size && (left_start + 1) < num_total; i++) {
			global_data[left_start + i] = tmp[tmp_index + i];
			//printf("i=%d: %d    ",left_start+i,tmp[tmp_index + i]);
		}
	}

	private static void printf(String format) {
		System.out.println(format);
	}

	private static void printf(String s, int left_start, int right_start, int bucket_size) {
		String komplett = String.format(s, left_start, right_start, bucket_size);
		System.out.println(komplett);
	}

	private static void printf(String s, int k, int thid, int num_local, int gid) {
		String komplett = String.format(s, k, thid, num_local, gid);
		System.out.println(komplett);
	}

	private int get_group_id(int i) {
		return this.group_id;
	}

	private int get_local_size(int i) {
		return this.local_size;
	}

	private int get_global_size(int i) {
		return this.global_size;
	}

	private int get_local_id(int i) {
		return this.local_id;
	}

	private int get_global_id(int i) {

		return this.global_id;
	}
}
