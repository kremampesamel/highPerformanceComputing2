__kernel void image_rotate(
__global int *src_data, __global int *dest_data, int W, int H, float sinTheta, float cosTheta )
{
  //Thread gets its index within index space
  float x0 = W/2;
  float y0 = H/2;
  int x1 = get_global_id(0);
  int y1 = get_global_id(1);
  int x2 = (int)(( (((float) x1) - x0) *cosTheta - (((float) y1) - y0) * sinTheta) + x0);
  int y2 = (int)(( (((float) x1) - x0) * sinTheta + (((float) y1) - y0)*  cosTheta) + y0);
  if ( ( ((int)x2 >= 0) && ((int)x2 < W) ) && ( ((int)y2 >= 0) && ((int)y2 < H) ) )
  {
   dest_data[y1 * W + x1] = src_data[y2 * W + x2];
  }
}