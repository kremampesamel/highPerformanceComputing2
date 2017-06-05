int findLargestNum(int * array, int size){

  int i;
  int largestNum = -1;

  for(i = 0; i < size; i++){
    if(array[i] > largestNum)
      largestNum = array[i];
  }

  return largestNum;
}

// Radix Sort
/*
void radixSort(int * in, int * out, int size, int local_index){
  // Base 10 is used
  int i;
  int semiSorted[size];
  int significantDigit = 1;
  int * array = in;
  int largestNum = findLargestNum(array, size);

  while (largestNum / significantDigit > 0){
    int bucket[10] = { 0 };

    // Counts the number of "keys" or digits that will go into each bucket
    for (i = 0; i < size; i++)
      bucket[(array[i] / significantDigit) % 10]++;

    for (i = 1; i < 10; i++)
      bucket[i] += bucket[i - 1];

    // Use the bucket to fill a "semiSorted" array
    for (i = size - 1; i >= 0; i--)
      semiSorted[--bucket[(array[i] / significantDigit) % 10]] = array[i];

    for (i = 0; i < size; i++)
      array[i] = semiSorted[i];

    significantDigit *= 10;
  }
}
/**/

__kernel void radix_sort8(
	__global const int *global_data,
	__global int* out,
	//__local int* tmp,
	const int num_total, const int bucket_size
	) {

	int thid = get_global_id(0);
	// time is running out;
}