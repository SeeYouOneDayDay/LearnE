package utils.gs.demo;

import utils.gs.UnsafeHelper;
import uts.XL;

public class OffHeapArray {
    // 一个int等于4个字节
    private static final int INT = 4;
    private long size;
    private long address;

    // 构造方法，分配内存
    public OffHeapArray(long size) {
        allocateMemory(size);
    }

    public long allocateMemory(long _size) {
        this.size = _size;
        // 参数字节数
        address = UnsafeHelper.allocateMemory(_size * INT);
        XL.d("OffHeapArray  address:" + address);
        return address;
    }

    // 获取指定索引处的元素
    public int get(long i) {
        return UnsafeHelper.getInt(address + i * INT);
    }

    // 设置指定索引处的元素
    public void set(long position, int value) {
        UnsafeHelper.putInt(address + position * INT, value);
    }

    // 元素个数
    public long size() {
        return size;
    }

    // 释放堆外内存
    public void freeMemory() {
        UnsafeHelper.freeMemory(address);
    }

    public static void cases() {
        OffHeapArray offHeapArray = new OffHeapArray(4);
        offHeapArray.set(0, 1);
        offHeapArray.set(1, 2);
        offHeapArray.set(2, 3);
        offHeapArray.set(3, 4);
        offHeapArray.set(2, 5); // 在索引2的位置重复放入元素

        int sum = 0;
        for (int i = 0; i < offHeapArray.size(); i++) {
            int current = offHeapArray.get(i);
            sum += current;
            XL.d("OffHeapArray  " + i + "---->:" + current);

        }
// 打印12
        System.out.println(sum);
        XL.d("OffHeapArray  sum---->:" + sum);

        offHeapArray.freeMemory();
    }
}
