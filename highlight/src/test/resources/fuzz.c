#include <stdint.h>
#include <stdio.h>
#include <stddef.h>

int crash() {
	__builtin_trap();
}

int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
  if (size > 0 && data[0] == 'H')
    if (size > 1 && data[1] == 'I')
      if (size > 2 && data[2] == '!')
         crash();

  return 0;
}
