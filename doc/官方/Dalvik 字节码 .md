# Dalvik 字节码

 





## 通用设计

- 机器模型和调用规范旨在大致模仿常见的真实架构和 C 样式的调用规范：

    - 机器基于寄存器，而帧的大小在创建时确定后就固定不变。每一帧由特定数量的寄存器（由相应方法指定）以及执行该方法所需的所有辅助数据构成，例如（但不限于）程序计数器和对包含该方法的 `.dex` 文件的引用。
    - 当用于位值（例如整数和浮点数）时，寄存器会被视为宽度为 32 位。如果值是 64 位，则使用两个相邻的寄存器。对于寄存器对，没有对齐要求。
    - 当用于对象引用时，寄存器会被视为其宽度正好能够容纳一个此类引用。
    - 对于按位表示，`(Object) null == (int) 0`。
    - 如果一个方法有 N 个参数，则在该方法的调用帧的最后 N 个寄存器中按顺序传递这些参数。宽参数占用两个寄存器。向实例方法传入一个 `this` 引用作为其第一个参数。

- 指令流中的存储单元是 16 位无符号数。某些指令中的某些位会被忽略/必须为 0。

- 指令并非一定限于特定类型。例如，在不做任何解释的情况下移动 32 位寄存器值的指令不一定非得指定移动对象的类型是整数还是浮点数。

- 至于对字符串、类型、字段和方法的引用，有已单独枚举且已编好索引的常量池。

- 按位字面数据在指令流中内嵌表示。

- 在实践中，一个方法需要 16 个以上的寄存器不太常见，而需要 8 个以上的寄存器却相当普遍，因此很多指令仅限于寻址前 16 个寄存器。在合理的可能情况下，指令允许引用最多前 256 个寄存器。此外，某些指令还具有允许更多寄存器的变体，包括可寻址 `v0` - `v65535` 范围内的寄存器的一对 catch-all `move` 指令。如果指令变体不能用于寻址所需的寄存器，寄存器内容会（在运算前）从原始寄存器移动到低位寄存器和/或（在运算后）从低位结果寄存器移动到高位寄存器。

- 有几个“伪指令”可用于容纳被常规指令（例如，`fill-array-data`）引用的可变长度数据负载。在正常执行流程中绝对不能遇到这类指令。此外，这类指令必须位于偶数字节码偏移（即以 4 字节对齐）上。为了满足这一要求，如果这类指令未对齐，则 dex 生成工具必须发出额外的 `nop` 指令进行填充。最后，虽然并非必须这样做，但是大多数工具会选择在方法的末尾发出这些额外的指令，否则可能需要额外的指令才能围绕这些方法进行分支。

- 如果安装到正在运行的系统中，某些指令可能会被改变，因为在其安装过程中执行的静态链接优化可能会更改它们的格式。这样可以在链接已知之后加快执行的速度。有关建议的变体，请参阅相关的[指令格式文档](https://source.android.com/devices/tech/dalvik/instruction-formats)。特意使用“建议”一词是因为并非必须强制实施这些变体。

- 符合人类语言习惯的语法与助记符：

    - 对参数进行 Dest-then-source 排序。

    - 一些运算码具有消除歧义的名称后缀，这些后缀表示运算类型：

        - 常规类型的 32 位运算码未标记。
        - 常规类型的 64 位运算码以 `-wide` 为后缀。
        - 特定类型的运算码以其类型（或简单缩写）为后缀，这些类型包括：`-boolean`、`-byte`、`-char`、`-short`、`-int`、`-long`、`-float`、`-double`、`-object`、`-string`、`-class` 和 `-void`。

    - 一些运算码具有消除歧义的后缀，这些后缀用于区分除指令版式或选项不同之外其他完全相同的运算。这些后缀与主要名称之间以斜杠（“`/`”）分开，主要目的是使生成和解析可执行文件的代码中存在与静态常量的一对一映射关系（即，降低让代码查看者感到模糊不清的可能性）。

    - 在本文档的说明部分，我们使用 4 位宽的字符来强调值的宽度（例如，指示常量的范围或可能寻址的寄存器的数量）。

    - 例如，在指令“

        ```
        move-wide/from16 vAA, vBBBB
        ```

        ”中：

        - “`move`”为基础运算码，表示基础运算（移动寄存器的值）。
        - “`wide`”为名称后缀，表示指令对宽（64 位）数据进行运算。
        - “`from16`”为运算码后缀，表示具有 16 位寄存器引用源的变体。
        - “`vAA`”为目标寄存器（隐含在运算中；并且，规定目标参数始终在前），取值范围为 `v0` - `v255`。
        - “`vBBBB`”是源寄存器，取值范围为 `v0` - `v65535`。

- 请参阅[指令格式文档](https://source.android.com/devices/tech/dalvik/instruction-formats)，详细了解各种指令格式（在“运算和格式”下列出）以及运算码语法。

- 请参阅 [`.dex` 文件格式文档](https://source.android.com/devices/tech/dalvik/dex-format)，详细了解字节码如何融入整个编码环境。

## 字节码集合的总结

| 运算和格式 | 助记符/语法                                                  | 参数                                                         | 说明                                                         |
| :--------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| 00 10x     | nop                                                          |                                                              | 空循环。**注意**：数据传送伪指令用此运算码标记，在这种情况下，运算码单元的高阶字节表示数据的性质。请参阅下文的“`packed-switch-payload` 格式”、“`sparse-switch-payload` 格式”和“`fill-array-data-payload` 格式”。 |
| 01 12x     | move vA, vB                                                  | `A:` 目标寄存器（4 位） `B:` 源寄存器（4 位）                | 将一个非对象寄存器的内容移到另一个非对象寄存器中。           |
| 02 22x     | move/from16 vAA, vBBBB                                       | `A:` 目标寄存器（8 位） `B:` 源寄存器（16 位）               | 将一个非对象寄存器的内容移到另一个非对象寄存器中。           |
| 03 32x     | move/16 vAAAA, vBBBB                                         | `A:` 目标寄存器（16 位） `B:` 源寄存器（16 位）              | 将一个非对象寄存器的内容移到另一个非对象寄存器中。           |
| 04 12x     | move-wide vA, vB                                             | `A:` 目标寄存器对（4 位） `B:` 源寄存器对（4 位）            | 将一个寄存器对的内容移到另一个寄存器对中。**注意**：可以从 `v*N*` 移到 `v*N-1*` 或 `v*N+1*`，因此必须在执行写入运算之前，为要读取的寄存器对的两部分均安排实现。 |
| 05 22x     | move-wide/from16 vAA, vBBBB                                  | `A:` 目标寄存器对（8 位） `B:` 源寄存器对（16 位）           | 将一个寄存器对的内容移到另一个寄存器对中。**注意**：实现的注意事项与上文的 `move-wide` 相同。 |
| 06 32x     | move-wide/16 vAAAA, vBBBB                                    | `A:` 目标寄存器对（16 位） `B:` 源寄存器对（16 位）          | 将一个寄存器对的内容移到另一个寄存器对中。**注意**：实现的注意事项与上文的 `move-wide` 相同。 |
| 07 12x     | move-object vA, vB                                           | `A:` 目标寄存器（4 位） `B:` 源寄存器（4 位）                | 将一个对象传送寄存器的内容移到另一个对象传送寄存器中。       |
| 08 22x     | move-object/from16 vAA, vBBBB                                | `A:` 目标寄存器（8 位） `B:` 源寄存器（16 位）               | 将一个对象传送寄存器的内容移到另一个对象传送寄存器中。       |
| 09 32x     | move-object/16 vAAAA, vBBBB                                  | `A:` 目标寄存器（16 位） `B:` 源寄存器（16 位）              | 将一个对象传送寄存器的内容移到另一个对象传送寄存器中。       |
| 0a 11x     | move-result vAA                                              | `A:` 目标寄存器（8 位）                                      | 将最新的 `invoke-*kind*` 的单字非对象结果移到指定的寄存器中。该指令必须紧跟在其（单字非对象）结果不会被忽略的 `invoke-*kind*` 之后执行，否则无效。 |
| 0b 11x     | move-result-wide vAA                                         | `A:` 目标寄存器对（8 位）                                    | 将最新的 `invoke-*kind*` 的双字结果移到指定的寄存器对中。该指令必须紧跟在其（双字）结果不会被忽略的 `invoke-*kind*` 之后执行，否则无效。 |
| 0c 11x     | move-result-object vAA                                       | `A:` 目标寄存器（8 位）                                      | 将最新的 `invoke-*kind*` 的对象结果移到指定的寄存器中。该指令必须紧跟在其（对象）结果不会被忽略的 `invoke-*kind*` 或 `filled-new-array` 之后执行，否则无效。 |
| 0d 11x     | move-exception vAA                                           | `A:` 目标寄存器（8 位）                                      | 将刚刚捕获的异常保存到给定寄存器中。该指令必须为捕获的异常不会被忽略的任何异常处理程序的第一条指令，且该指令必须仅作为异常处理程序的第一条指令执行，否则无效。 |
| 0e 10x     | return-void                                                  |                                                              | 从 `void` 方法返回。                                         |
| 0f 11x     | return vAA                                                   | `A:` 返回值寄存器（8 位）                                    | 从单字宽度（32 位）非对象值返回方法返回。                    |
| 10 11x     | return-wide vAA                                              | `A:` 返回值寄存器对（8 位）                                  | 从双字宽度（64 位）值返回方法返回。                          |
| 11 11x     | return-object vAA                                            | `A:` 返回值寄存器（8 位）                                    | 从对象返回方法返回。                                         |
| 12 11n     | const/4 vA, #+B                                              | `A:` 目标寄存器（4 位） `B:` 有符号整数（4 位）              | 将给定的字面量值（符号扩展为 32 位）移到指定的寄存器中。     |
| 13 21s     | const/16 vAA, #+BBBB                                         | `A:` 目标寄存器（8 位） `B:` 有符号整数（16 位）             | 将给定的字面量值（符号扩展为 32 位）移到指定的寄存器中。     |
| 14 31i     | const vAA, #+BBBBBBBB                                        | `A:` 目标寄存器（8 位） `B:` 任意 32 位常量                  | 将给定的字面量值移到指定的寄存器中。                         |
| 15 21h     | const/high16 vAA, #+BBBB0000                                 | `A:` 目标寄存器（8 位） `B:` 有符号整数（16 位）             | 将给定的字面量值（右零扩展为 32 位）移到指定的寄存器中。     |
| 16 21s     | const-wide/16 vAA, #+BBBB                                    | `A:` 目标寄存器（8 位） `B:` 有符号整数（16 位）             | 将给定的字面量值（符号扩展为 64 位）移到指定的寄存器对中。   |
| 17 31i     | const-wide/32 vAA, #+BBBBBBBB                                | `A:` 目标寄存器（8 位） `B:` 有符号整数（32 位）             | 将给定的字面量值（符号扩展为 64 位）移到指定的寄存器对中。   |
| 18 51l     | const-wide vAA, #+BBBBBBBBBBBBBBBB                           | `A:` 目标寄存器（8 位） `B:` 任意双字宽度（64 位）常量       | 将给定的字面量值移到指定的寄存器对中。                       |
| 19 21h     | const-wide/high16 vAA, #+BBBB000000000000                    | `A:` 目标寄存器（8 位） `B:` 有符号整数（16 位）             | 将给定的字面量值（右零扩展为 64 位）移到指定的寄存器对中。   |
| 1a 21c     | const-string vAA, string@BBBB                                | `A:` 目标寄存器（8 位） `B:` 字符串索引                      | 将通过给定索引指定的字符串的引用移到指定的寄存器中。         |
| 1b 31c     | const-string/jumbo vAA, string@BBBBBBBB                      | `A:` 目标寄存器（8 位） `B:` 字符串索引                      | 将通过给定索引指定的字符串的引用移到指定的寄存器中。         |
| 1c 21c     | const-class vAA, type@BBBB                                   | `A:` 目标寄存器（8 位） `B:` 类型索引                        | 将通过给定索引指定的类的引用移到指定的寄存器中。如果指定的类型是原始类型，则将存储对原始类型的退化类的引用。 |
| 1d 11x     | monitor-enter vAA                                            | `A:` 引用传送寄存器（8 位）                                  | 获取指定对象的监视锁。                                       |
| 1e 11x     | monitor-exit vAA                                             | `A:` 引用传送寄存器（8 位）                                  | 释放指定对象的监视锁。**注意**：如果该指令需要抛出异常，则必须像 PC 已超出该指令那样抛出。不妨将其想象成，该指令（在某种意义上）已成功执行，并在该指令执行后但下一条指令找到机会执行前抛出异常。这种定义使得某个方法有可能将监视锁清理 catch-all（例如 `finally`）分块用作该分块自身的监视锁清理，以便处理可能由于 `Thread.stop()` 的既往实现而抛出的任意异常，同时仍尽力维持适当的监视锁安全机制。 |
| 1f 21c     | check-cast vAA, type@BBBB                                    | `A:` 引用传送寄存器（8 位） `B:` 类型索引（16 位）           | 如果给定寄存器中的引用不能转型为指定的类型，则抛出 `ClassCastException`。**注意**：由于 `A` 必须一律为引用（而非基元值），因此如果 `B` 引用基元类型，则必然会在运行时失败（即抛出异常）。 |
| 20 22c     | instance-of vA, vB, type@CCCC                                | `A:` 目标寄存器（4 位） `B:` 引用传送寄存器（4 位） `C:` 类型索引（16 位） | 如果指定的引用是给定类型的实例，则为给定目标寄存器赋值 `1`，否则赋值 `0`。**注意**：由于 `B` 必须一律为引用（而非基元值），因此如果 `C` 引用基元类型，则始终赋值 `0`。 |
| 21 12x     | array-length vA, vB                                          | `A:` 目标寄存器（4 位） `B:` 数组引用传送寄存器（4 位）      | 将指定数组的长度（条目个数）赋值给给定目标寄存器             |
| 22 21c     | new-instance vAA, type@BBBB                                  | `A:` 目标寄存器（8 位） `B:` 类型索引                        | 根据指定的类型构造新实例，并将对该新实例的引用存储到目标寄存器中。该类型必须引用非数组类。 |
| 23 22c     | new-array vA, vB, type@CCCC                                  | `A:` 目标寄存器（4 位） `B:` 大小寄存器 `C:` 类型索引        | 根据指定的类型和大小构造新数组。该类型必须是数组类型。       |
| 24 35c     | filled-new-array {vC, vD, vE, vF, vG}, type@BBBB             | `A:` 数组大小和参数字数（4 位） `B:` 类型索引（16 位） `C..G:` 参数寄存器（每个寄存器各占 4 位） | 根据给定类型和大小构造数组，并使用提供的内容填充该数组。该类型必须是数组类型。数组的内容必须是单字类型（即不接受 `long` 或 `double` 类型的数组，但接受引用类型的数组）。构造的实例会存储为一个“结果”，方式与方法调用指令存储其结果的方式相同，因此构造的实例必须移到后面紧跟 `move-result-object` 指令（如果要使用的话）的寄存器。 |
| 25 3rc     | filled-new-array/range {vCCCC .. vNNNN}, type@BBBB           | `A:` 数组大小和参数字数（8 位） `B:` 类型索引（16 位） `C:` 第一个参数寄存器（16 位） `N = A + C - 1` | 根据给定类型和大小构造数组，并使用提供的内容填充该数组。相关的说明和限制与上文所述 `filled-new-array` 相同。 |
| 26 31t     | fill-array-data vAA, +BBBBBBBB（有关补充数据，请参阅下文的“`fill-array-data-payload` 格式”） | `A:` 数组引用（8 位） `B:` 到表格数据伪指令的有符号“分支”偏移量（32 位） | 用指定的数据填充给定数组。必须引用基元类型的数组，且数据表格的类型必须与数组匹配；此外，数据表格所包含的元素个数不得超出数组中的元素个数。也就是说，数组可以比表格大；如果是这样，仅设置数组的初始元素，而忽略剩余元素。 |
| 27 11x     | throw vAA                                                    | `A:` 异常传送寄存器（8 位）                                  | 抛出指定的异常。                                             |
| 28 10t     | goto +AA                                                     | `A:` 有符号分支偏移量（8 位）                                | 无条件地跳转到指定的指令。**注意**：分支偏移量不得为 `0`。（自旋循环可以用 `goto/32` 正常构造，也可以通过在分支之前添加 `nop` 作为目标来正常构造。） |
| 29 20t     | goto/16 +AAAA                                                | `A:` 有符号分支偏移量（16 位）                               | 无条件地跳转到指定的指令。**注意**：分支偏移量不得为 `0`。（自旋循环可以用 `goto/32` 正常构造，也可以通过在分支之前添加 `nop` 作为目标来正常构造。） |
| 2a 30t     | goto/32 +AAAAAAAA                                            | `A:` 有符号分支偏移量（32 位）                               | 无条件地跳转到指定的指令。                                   |
| 2b 31t     | packed-switch vAA, +BBBBBBBB（有关补充数据，请参阅下文的“`packed-switch-payload` 格式”） | `A:` 要测试的寄存器 `B:` 到表格数据伪指令的有符号“分支”偏移量（32 位） | 通过使用与特定整数范围内的每个值相对应的偏移量表，基于给定寄存器中的值跳转到新指令；如果没有匹配项，则跳转到下一条指令。 |
| 2c 31t     | sparse-switch vAA, +BBBBBBBB（有关补充数据，请参阅下文的“`sparse-switch-payload` 格式”） | `A:` 要测试的寄存器 `B:` 到表格数据伪指令的有符号“分支”偏移量（32 位） | 通过使用偏移值对的有序表，基于给定寄存器中的值跳转到新指令；如果没有匹配项，则跳转到下一条指令。 |
| 2d..31 23x | cmp kind vAA, vBB, vCC 2d: cmpl-float (lt bias) 2e: cmpg-float (gt bias) 2f: cmpl-double (lt bias) 30: cmpg-double (gt bias) 31: cmp-long | `A:` 目标寄存器（8 位） `B:` 第一个源寄存器或寄存器对 `C:` 第二个源寄存器或寄存器对 | 执行指定的浮点或 `long` 比较，如果 `b == c`，则将 `a` 设为 `0`，如果 `b > c`，则设为 `1`，如果 `b < c`，则设为 `-1`。为浮点运算列出的“bias”表示如何处理 `NaN` 比较运算：对于 `NaN` 比较，“gt bias”指令返回 `1`，而“lt bias”指令返回 `-1`。例如，建议使用 `cmpg-float` 检查浮点数是否满足条件 `x < y`；如果结果是 `-1`，则表示测试结果为 true，其他值则表示测试结果为 false，原因是当前比较是有效比较但是结果不符合预期或其中一个值是 `NaN`。 |
| 32..37 22t | if-test vA, vB, +CCCC 32: if-eq 33: if-ne 34: if-lt 35: if-ge 36: if-gt 37: if-le | `A:` 要测试的第一个寄存器（4 位） `B:` 要测试的第二个寄存器（4 位） `C:` 有符号分支偏移量（16 位） | 如果两个给定寄存器的值比较结果符合预期，则分支到给定目标寄存器。**注意**：分支偏移量不得为 `0`。（自旋循环可以通过围绕后向 `goto` 进行分支或通过在分支之前添加 `nop` 作为目标来正常构造。） |
| 38..3d 21t | if-testz vAA, +BBBB 38: if-eqz 39: if-nez 3a: if-ltz 3b: if-gez 3c: if-gtz 3d: if-lez | `A:` 要测试的寄存器（8 位） `B:` 有符号分支偏移量（16 位）   | 如果给定寄存器的值与 0 的比较结果符合预期，则分支到给定目标寄存器。**注意**：分支偏移量不得为 `0`。（自旋循环可以通过围绕后向 `goto` 进行分支或通过在分支之前添加 `nop` 作为目标来正常构造。） |
| 3e..43 10x | （未使用）                                                   |                                                              | （未使用）                                                   |
| 44..51 23x | arrayop vAA, vBB, vCC 44: aget 45: aget-wide 46: aget-object 47: aget-boolean 48: aget-byte 49: aget-char 4a: aget-short 4b: aput 4c: aput-wide 4d: aput-object 4e: aput-boolean 4f: aput-byte 50: aput-char 51: aput-short | `A:` 值寄存器或寄存器对；可以是源寄存器或寄存器对，也可以是目标寄存器或寄存器对（8 位） `B:` 数组寄存器（8 位） `C:` 索引寄存器（8 位） | 在给定数组的已标识索引处执行已确定的数组运算，并将结果加载或存储到值寄存器中。 |
| 52..5f 22c | i*instanceop* vA, vB, field@CCCC 52: iget 53: iget-wide 54: iget-object 55: iget-boolean 56: iget-byte 57: iget-char 58: iget-short 59: iput 5a: iput-wide 5b: iput-object 5c: iput-boolean 5d: iput-byte 5e: iput-char 5f: iput-short | `A:` 值寄存器或寄存器对；可以是源寄存器或寄存器对，也可以是目标寄存器或寄存器对（4 位） `B:` 对象寄存器（4 位） `C:` 实例字段引用索引（16 位） | 对已标识的字段执行已确定的对象实例字段运算，并将结果加载或存储到值寄存器中。**注意**：这些运算码是静态链接的合理候选项，将字段参数更改为更直接的偏移量。 |
| 60..6d 21c | s*staticop* vAA, field@BBBB 60: sget 61: sget-wide 62: sget-object 63: sget-boolean 64: sget-byte 65: sget-char 66: sget-short 67: sput 68: sput-wide 69: sput-object 6a: sput-boolean 6b: sput-byte 6c: sput-char 6d: sput-short | `A:` 值寄存器或寄存器对；可以是源寄存器或寄存器对，也可以是目标寄存器或寄存器对（8 位） `B:` 静态字段引用索引（16 位） | 对已标识的静态字段执行已确定的对象静态字段运算，并将结果加载或存储到值寄存器中。**注意**：这些运算码是静态链接的合理候选项，将字段参数更改为更直接的偏移量。 |
| 6e..72 35c | invoke-*kind* {vC, vD, vE, vF, vG}, meth@BBBB 6e: invoke-virtual 6f: invoke-super 70: invoke-direct 71: invoke-static 72: invoke-interface | `A:` 参数字数（4 位） `B:` 方法引用索引（16 位） `C..G:` 参数寄存器（每个寄存器各占 4 位） | 调用指定的方法。可使用相应的 `move-result*` 变体将所得结果（如果有的话）存储为紧跟其后的指令。`invoke-virtual` 用于调用正常的虚方法（该方法不是 `private`、`static` 或 `final`，也不是构造函数）。当 `method_id` 引用非接口类方法时，使用 `invoke-super` 调用最近父类的虚方法（这与调用类中具有相同 `method_id` 的方法相反）。`invoke-virtual` 具有相同的方法限制。在版本 `037` 或更高版本的 Dex 文件中，如果 `method_id` 引用接口方法，则使用 `invoke-super` 调用在该接口上定义的该方法的最具体、未被覆盖的版本。`invoke-virtual` 具有相同的方法限制。在版本 `037` 之前的 Dex 文件中，具有接口 `method_id` 是违反规则且未定义的。`invoke-direct` 用于调用非 `static` 直接方法（也就是说，本质上不可覆盖的实例方法，即 `private` 实例方法或构造函数）。`invoke-static` 用于调用 `static` 方法（该方法始终被视为直接方法）。`invoke-interface` 用于调用 `interface` 方法，也就是说，对具体类未知的对象使用引用 `interface` 的 `method_id`。**注意**：这些运算码是静态链接的合理候选项，将方法参数更改为更直接的偏移量（或相关的寄存器对）。 |
| 73 10x     | （未使用）                                                   |                                                              | （未使用）                                                   |
| 74..78 3rc | invoke-*kind*/range {vCCCC .. vNNNN}, meth@BBBB 74: invoke-virtual/range 75: invoke-super/range 76: invoke-direct/range 77: invoke-static/range 78: invoke-interface/range | `A:` 参数字数（8 位） `B:` 方法引用索引（16 位） `C:` 第一个参数寄存器（16 位） `N = A + C - 1` | 调用指定的方法。有关详情、注意事项和建议，请参阅上文第一个 `invoke-*kind*` 说明。 |
| 79..7a 10x | （未使用）                                                   |                                                              | （未使用）                                                   |
| 7b..8f 12x | *unop* vA, vB 7b: neg-int 7c: not-int 7d: neg-long 7e: not-long 7f: neg-float 80: neg-double 81: int-to-long 82: int-to-float 83: int-to-double 84: long-to-int 85: long-to-float 86: long-to-double 87: float-to-int 88: float-to-long 89: float-to-double 8a: double-to-int 8b: double-to-long 8c: double-to-float 8d: int-to-byte 8e: int-to-char 8f: int-to-short | `A:` 目标寄存器或寄存器对（4 位） `B:` 源寄存器或寄存器对（4 位） | 对源寄存器执行已确定的一元运算，并将结果存储到目标寄存器中。 |
| 90..af 23x | *binop* vAA, vBB, vCC 90: add-int 91: sub-int 92: mul-int 93: div-int 94: rem-int 95: and-int 96: or-int 97: xor-int 98: shl-int 99: shr-int 9a: ushr-int 9b: add-long 9c: sub-long 9d: mul-long 9e: div-long 9f: rem-long a0: and-long a1: or-long a2: xor-long a3: shl-long a4: shr-long a5: ushr-long a6: add-float a7: sub-float a8: mul-float a9: div-float aa: rem-float ab: add-double ac: sub-double ad: mul-double ae: div-double af: rem-double | `A:` 目标寄存器或寄存器对（8 位） `B:` 第一个源寄存器或寄存器对（8 位） `C:` 第二个源寄存器或寄存器对（8 位） | 对两个源寄存器执行已确定的二元运算，并将结果存储到目标寄存器中。**注意**：与其他 `-long` 数学运算（对第一个和第二个源采用寄存器对）相反，`shl-long`、`shr-long` 和 `ushr-long` 会对其第一个源采用寄存器对（存放要移位的值），但会对其第二个源采用单个寄存器（存放移位的距离）。 |
| b0..cf 12x | *binop*/2addr vA, vB b0: add-int/2addr b1: sub-int/2addr b2: mul-int/2addr b3: div-int/2addr b4: rem-int/2addr b5: and-int/2addr b6: or-int/2addr b7: xor-int/2addr b8: shl-int/2addr b9: shr-int/2addr ba: ushr-int/2addr bb: add-long/2addr bc: sub-long/2addr bd: mul-long/2addr be: div-long/2addr bf: rem-long/2addr c0: and-long/2addr c1: or-long/2addr c2: xor-long/2addr c3: shl-long/2addr c4: shr-long/2addr c5: ushr-long/2addr c6: add-float/2addr c7: sub-float/2addr c8: mul-float/2addr c9: div-float/2addr ca: rem-float/2addr cb: add-double/2addr cc: sub-double/2addr cd: mul-double/2addr ce: div-double/2addr cf: rem-double/2addr | `A:` 目标寄存器或寄存器对和第一个源寄存器或寄存器对（4 位） `B:` 第二个源寄存器或寄存器对（4 位） | 对两个源寄存器执行已确定的二元运算，并将结果存储到第一个源寄存器中。**注意**：与其他 `-long/2addr` 数学运算（对其目标/第一个源和第二个源都采用寄存器对）相反，`shl-long/2addr`、`shr-long/2addr` 和 `ushr-long/2addr` 会对其目标/第一个源采用寄存器对（存放要移位的值），但会对其第二个源采用单个寄存器（存放移位的距离）。 |
| d0..d7 22s | *binop*/lit16 vA, vB, #+CCCC d0: add-int/lit16 d1: rsub-int (reverse subtract) d2: mul-int/lit16 d3: div-int/lit16 d4: rem-int/lit16 d5: and-int/lit16 d6: or-int/lit16 d7: xor-int/lit16 | `A:` 目标寄存器（4 位） `B:` 源寄存器（4 位） `C:` 有符号整数常量（16 位） | 对指定的寄存器（第一个参数）和字面量值（第二个参数）执行指定的二元运算，并将结果存储到目标寄存器中。**注意**：`rsub-int` 不含后缀，因为此版本是其一系列运算码中的主运算码。另外，有关语义的详细信息，请参阅下文。 |
| d8..e2 22b | *binop*/lit8 vAA, vBB, #+CC d8: add-int/lit8 d9: rsub-int/lit8 da: mul-int/lit8 db: div-int/lit8 dc: rem-int/lit8 dd: and-int/lit8 de: or-int/lit8 df: xor-int/lit8 e0: shl-int/lit8 e1: shr-int/lit8 e2: ushr-int/lit8 | `A:` 目标寄存器（8 位） `B:` 源寄存器（8 位） `C:` 有符号整数常量（8 位） | 对指定的寄存器（第一个参数）和字面量值（第二个参数）执行指定的二元运算，并将结果存储到目标寄存器中。**注意**：有关 `rsub-int` 语义的详细信息，请参阅下文。 |
| e3..f9 10x | （未使用）                                                   |                                                              | （未使用）                                                   |
| fa 45cc    | invoke-polymorphic {vC, vD, vE, vF, vG}, meth@BBBB, proto@HHHH | `A:` 参数字数（4 位） `B:` 方法引用索引（16 位） `C:` 接收器（4 位） `D..G:` 参数寄存器（每个寄存器各占 4 位） `H:` 原型引用索引（16 位） | 调用指定的签名多态方法。可使用相应的 `move-result*` 变体将所得结果（如果有的话）存储为紧跟其后的指令。  方法引用必须针对签名多态方法，例如 `java.lang.invoke.MethodHandle.invoke` 或 `java.lang.invoke.MethodHandle.invokeExact`。  接收器必须是一个支持所调用签名多态方法的对象。  原型引用说明了所提供的参数类型和预期的返回类型。  `invoke-polymorphic` 字节码执行时可能会引发异常。有关这些异常的相关说明，请参阅所调用签名多态方法的 API 文档。  存在于 `038` 及更高版本的 Dex 文件中。 |
| fb 4rcc    | invoke-polymorphic/range {vCCCC .. vNNNN}, meth@BBBB, proto@HHHH | `A:` 参数字数（8 位） `B:` 方法引用索引（16 位） `C:` 接收器（16 位） `H:` 原型引用索引（16 位） `N = A + C - 1` | 调用指定的方法句柄。有关详情，请参阅上文的 `invoke-polymorphic` 说明。  存在于 `038` 及更高版本的 Dex 文件中。 |
| fc 35c     | invoke-custom {vC, vD, vE, vF, vG}, call_site@BBBB           | `A:` 参数字数（4 位） `B:` 调用点引用索引（16 位） `C..G:` 参数寄存器（每个寄存器各占 4 位） | 解析并调用指定的调用点。可使用相应的 `move-result*` 变体将调用的结果（如果有的话）存储为紧跟其后的指令。  该指令分两个阶段执行：调用点解析和调用点调用。  调用点解析会检查指定调用点是否有关联的 `java.lang.invoke.CallSite` 实例。如果没有，则使用 Dex 文件中存在的参数调用指定调用点的引导程序链接器方法（请参阅 [call_site_item](https://source.android.com/devices/tech/dalvik/dex-format#call-site-item)）。引导程序链接器方法会返回一个 `java.lang.invoke.CallSite` 实例；如果不存在关联，则该实例将与指定的调用点关联。另一个线程可能已先进行了关联；如果是这种情况，则通过第一个关联的 `java.lang.invoke.CallSite` 实例继续执行该指令。  对所解析的 `java.lang.invoke.CallSite` 实例的 `java.lang.invoke.MethodHandle` 目标进行调用点调用。目标的调用就像执行 `invoke-polymorphic`（如上所述）一样（使用 `invoke-custom` 指令的方法句柄和参数作为精确方法句柄调用的参数）。  引导程序链接器方法引发的异常会封装在 `java.lang.BootstrapMethodError` 中。如果出现下列情况，还将引发 `BootstrapMethodError`：该引导程序链接器方法无法返回 `java.lang.invoke.CallSite` 实例。返回的 `java.lang.invoke.CallSite` 具有 `null` 方法句柄目标。该方法句柄目标不属于所请求的类型。存在于版本 `038` 及更高版本的 Dex 文件中。 |
| fd 3rc     | invoke-custom/range {vCCCC .. vNNNN}, call_site@BBBB         | `A:` 参数字数（8 位） `B:` 调用点引用索引（16 位） `C:` 第一个参数寄存器（16 位） `N = A + C - 1` | 解析并调用一个调用点。有关详情，请参阅上文的 `invoke-custom` 说明。  存在于 `038` 及更高版本的 Dex 文件中。 |
| fe 21c     | const-method-handle vAA, method_handle@BBBB                  | `A:` 目标寄存器（8 位） `B:` 方法句柄索引（16 位）           | 将通过给定索引指定的方法句柄的引用移到指定的寄存器中。  存在于 `039` 及更高版本的 Dex 文件中。 |
| ff 21c     | const-method-type vAA, proto@BBBB                            | `A:` 目标寄存器（8 位） `B:` 方法原型引用（16 位）           | 将通过给定索引指定的方法原型的引用移到指定的寄存器中。  存在于 `039` 及更高版本的 Dex 文件中。 |

## packed-switch-payload 格式

| 名称      | 格式            | 说明                                                         |
| :-------- | :-------------- | :----------------------------------------------------------- |
| ident     | ushort = 0x0100 | 识别伪运算码                                                 |
| size      | ushort          | 表格中的条目数                                               |
| first_key | int             | 第一位（即最低位）switch case 的值                           |
| targets   | int[]           | 与 `size` 相对的分支目标的列表。这些目标相对应的是 switch 运算码的地址（而非此表格的地址）。 |

**注意**：此表格一个实例的代码单元总数为 `(size * 2) + 4`。

## sparse-switch-payload 格式

| 名称    | 格式            | 说明                                                         |
| :------ | :-------------- | :----------------------------------------------------------- |
| ident   | ushort = 0x0200 | 识别伪运算码                                                 |
| size    | ushort          | 表格中的条目数                                               |
| keys    | int[]           | `size` 键值列表，从低到高排序                                |
| targets | int[]           | 与 `size` 相对的分支目标的列表，每一个目标对应相同索引下的键值。这些目标相对应的是 switch 运算码的地址（而非此表格的地址）。 |

**注意**：此表格一个实例的代码单元总数为 `(size * 4) + 2`。

## fill-array-data-payload 格式

| 名称          | 格式            | 说明             |
| :------------ | :-------------- | :--------------- |
| ident         | ushort = 0x0300 | 识别伪运算码     |
| element_width | ushort          | 每个元素的字节数 |
| size          | uint            | 表格中的元素数   |
| data          | ubyte[]         | 数据值           |

**注意**：此表格一个实例的代码单元总数为 `(size * element_width + 1) / 2 + 4`。

## 数学运算详情

**注意**：除非另有说明，否则浮点运算必须遵循 IEEE 754 规则，使用最近舍入和渐进式下溢。

| 运算码          | C 语义                                             | 备注                                                         |
| :-------------- | :------------------------------------------------- | :----------------------------------------------------------- |
| neg-int         | int32 a; int32 result = -a;                        | 一元二进制补码。                                             |
| not-int         | int32 a; int32 result = ~a;                        | 一元反码。                                                   |
| neg-long        | int64 a; int64 result = -a;                        | 一元二进制补码。                                             |
| not-long        | int64 a; int64 result = ~a;                        | 一元反码。                                                   |
| neg-float       | float a; float result = -a;                        | 浮点否定。                                                   |
| neg-double      | double a; double result = -a;                      | 浮点否定。                                                   |
| int-to-long     | int32 a; int64 result = (int64) a;                 | 将 `int32` 符号扩展为 `int64`。                              |
| int-to-float    | int32 a; float result = (float) a;                 | 使用最近舍入，将 `int32` 转换为 `float`。这会导致某些值不够精准。 |
| int-to-double   | int32 a; double result = (double) a;               | 将 `int32` 转换为 `double`。                                 |
| long-to-int     | int64 a; int32 result = (int32) a;                 | 将 `int64` 截断为 `int32`。                                  |
| long-to-float   | int64 a; float result = (float) a;                 | 使用最近舍入，将 `int64` 转换为 `float`。这会导致某些值不够精准。 |
| long-to-double  | int64 a; double result = (double) a;               | 使用最近舍入，将 `int64` 转换为 `double`。这会导致某些值不够精准。 |
| float-to-int    | float a; int32 result = (int32) a;                 | 使用向零舍入，将 `float` 转换为 `int32`。`NaN` 和 `-0.0`（负零）转换为整数 `0`。无穷数和因所占幅面过大而无法表示的值根据符号转换为 `0x7fffffff` 或 `-0x80000000`。 |
| float-to-long   | float a; int64 result = (int64) a;                 | 使用向零舍入，将 `float` 转换为 `int64`。适用于 `float-to-int` 的特殊情况规则也适用于此，但超出范围的值除外，这些值根据符号转换为 `0x7fffffffffffffff` 或 `-0x8000000000000000`。 |
| float-to-double | float a; double result = (double) a;               | 将 `float` 转换为 `double`，值依然精准。                     |
| double-to-int   | double a; int32 result = (int32) a;                | 使用向零舍入，将 `double` 转换为 `int32`。适用于 `float-to-int` 的特殊情况规则也适用于此。 |
| double-to-long  | double a; int64 result = (int64) a;                | 使用向零舍入，将 `double` 转换为 `int64`。适用于 `float-to-long` 的特殊情况规则也适用于此。 |
| double-to-float | double a; float result = (float) a;                | 使用最近舍入，将 `double` 转换为 `float`。这会导致某些值不够精准。 |
| int-to-byte     | int32 a; int32 result = (a << 24) >> 24;           | 将 `int32` 截断为 `int8`，对结果进行符号扩展。               |
| int-to-char     | int32 a; int32 result = a & 0xffff;                | 将 `int32` 截断为 `uint16`，无需进行符号扩展。               |
| int-to-short    | int32 a; int32 result = (a << 16) >> 16;           | 将 `int32` 截断为 `int16`，对结果进行符号扩展。              |
| add-int         | int32 a, b; int32 result = a + b;                  | 二进制补码加法。                                             |
| sub-int         | int32 a, b; int32 result = a - b;                  | 二进制补码减法。                                             |
| rsub-int        | int32 a, b; int32 result = b - a;                  | 二进制补码反向减法。                                         |
| mul-int         | int32 a, b; int32 result = a * b;                  | 二进制补码乘法。                                             |
| div-int         | int32 a, b; int32 result = a / b;                  | 二进制补码除法，向零舍入（即截断为整数）。如果 `b == 0`，则会抛出 `ArithmeticException`。 |
| rem-int         | int32 a, b; int32 result = a % b;                  | 二进制补码除后取余数。结果的符号与 `a` 的符号相同，可更精确地定义为 `result == a - (a / b) * b`。如果 `b == 0`，则会抛出 `ArithmeticException`。 |
| and-int         | int32 a, b; int32 result = a & b;                  | 按位与运算。                                                 |
| or-int          | int32 a, b; int32 result = a \| b;                 | 按位或运算。                                                 |
| xor-int         | int32 a, b; int32 result = a ^ b;                  | 按位异或运算。                                               |
| shl-int         | int32 a, b; int32 result = a << (b & 0x1f);        | 按位左移（带掩码参数）。                                     |
| shr-int         | int32 a, b; int32 result = a >> (b & 0x1f);        | 按位有符号右移（带掩码参数）。                               |
| ushr-int        | uint32 a, b; int32 result = a >> (b & 0x1f);       | 按位无符号右移（带掩码参数）。                               |
| add-long        | int64 a, b; int64 result = a + b;                  | 二进制补码加法。                                             |
| sub-long        | int64 a, b; int64 result = a - b;                  | 二进制补码减法。                                             |
| mul-long        | int64 a, b; int64 result = a * b;                  | 二进制补码乘法。                                             |
| div-long        | int64 a, b; int64 result = a / b;                  | 二进制补码除法，向零舍入（即截断为整数）。如果 `b == 0`，则会抛出 `ArithmeticException`。 |
| rem-long        | int64 a, b; int64 result = a % b;                  | 二进制补码除后取余数。结果的符号与 `a` 的符号相同，可更精确地定义为 `result == a - (a / b) * b`。如果 `b == 0`，则会抛出 `ArithmeticException`。 |
| and-long        | int64 a, b; int64 result = a & b;                  | 按位与运算。                                                 |
| or-long         | int64 a, b; int64 result = a \| b;                 | 按位或运算。                                                 |
| xor-long        | int64 a, b; int64 result = a ^ b;                  | 按位异或运算。                                               |
| shl-long        | int64 a; int32 b; int64 result = a << (b & 0x3f);  | 按位左移（带掩码参数）。                                     |
| shr-long        | int64 a; int32 b; int64 result = a >> (b & 0x3f);  | 按位有符号右移（带掩码参数）。                               |
| ushr-long       | uint64 a; int32 b; int64 result = a >> (b & 0x3f); | 按位无符号右移（带掩码参数）。                               |
| add-float       | float a, b; float result = a + b;                  | 浮点加法。                                                   |
| sub-float       | float a, b; float result = a - b;                  | 浮点减法。                                                   |
| mul-float       | float a, b; float result = a * b;                  | 浮点乘法。                                                   |
| div-float       | float a, b; float result = a / b;                  | 浮点除法。                                                   |
| rem-float       | float a, b; float result = a % b;                  | 浮点除后取余数。该函数不同于 IEEE 754 取余，定义为 `result == a - roundTowardZero(a / b) * b`。 |
| add-double      | double a, b; double result = a + b;                | 浮点加法。                                                   |
| sub-double      | double a, b; double result = a - b;                | 浮点减法。                                                   |
| mul-double      | double a, b; double result = a * b;                | 浮点乘法。                                                   |
| div-double      | double a, b; double result = a / b;                | 浮点除法。                                                   |
| rem-double      | double a, b; double result = a % b;                | 浮点除后取余数。该函数不同于 IEEE 754 取余，定义为 `result == a - roundTowardZero(a / b) * b`。 |