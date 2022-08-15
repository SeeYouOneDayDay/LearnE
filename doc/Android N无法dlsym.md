# Android N无法dlsym

上文提到，为了实现对象和指针的转换，我们需要 dlsym 一个 libart.so 中的导出函数；但不幸地是，在Android N中，Google禁止了这种行为，如果你用 dlsym
去取符号，返回的结果是nullptr。怎么办呢？

libart.so 不过是一个加载在内存中的elf文件而已。我们通过读取 /proc/self/maps
拿到这个文件的加载基地址，然后直接解析ELF文件格式，查出这个符号在ELF文件中的偏移，再加上内存基址，就能得到这个符号真正的地址。不过这过程已经有人实现了，而且放在了github上：Nougat_dlfunctions
可以直接使用 :)


* https://github.com/avs333/Nougat_dlfunctions