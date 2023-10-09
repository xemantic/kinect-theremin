#version 330
precision highp float;

/*
 * Finds kinect pixel representing the minimal depth
 */

//TODO add precision

uniform int iWidth;
uniform int iHeight;

uniform sampler2D tex0;

out vec4 minDepth;

void main() {
  float min = 100.0;
  for (int y = 0; y < iHeight; y++) {
    for (int x = 0; x < iWidth; x++) {
      ivec2 coord = ivec2(x, y);
      float depth = texelFetch(tex0, coord, 0).r;
      if ((depth > 0.0) && (depth < min)) {
        min = depth;
      }
    }
  }
  minDepth = vec4(min);
}
