# APEX 文件格式

 





Android Pony EXpress (APEX) 是 Android 10 中引入的一种容器格式，用于较低级别系统模块的安装流程中。此格式可帮助更新不适用于标准 Android 应用模型的系统组件。一些示例组件包括原生服务和原生库、硬件抽象层 ([HAL](https://source.android.com/devices/architecture/hal-types)))、运行时 ([ART](https://source.android.com/devices/tech/dalvik)) 以及类库。

“APEX”这一术语也可以指 APEX 文件。

## 背景

虽然 Android 支持通过软件包安装程序应用（例如 Google Play 商店应用）更新适用于标准应用模型（如服务、Activity）的模块，但是对于较低级别的操作系统组件，使用类似模型具有以下缺陷：

- 基于 APK 的模块不能在启动序列早期使用。软件包管理器是应用相关信息的中央代码库，只能从 Activity 管理器（在启动过程的后期阶段准备就绪）启动。
- APK 格式（特别是清单）专用于 Android 应用，系统模块并不总是适用。

## 设计

本部分简要介绍了 APEX 文件格式和 APEX 管理器的设计，后者是一项管理 APEX 文件的服务。

如需详细了解为 APEX 选择此设计的原因，请参阅[开发 APEX 时考虑的替代方案](https://source.android.com/devices/tech/ota/apex#alternatives-when-developing)。

### APEX 格式

这是 APEX 文件的格式。

![APEX 文件格式](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151713398.png)

**图 1.** APEX 文件格式

在顶层，APEX 文件是一个 ZIP 文件，其中的文件以未压缩的形式存储，且位于 4 KB 边界。

APEX 文件中的 4 个文件如下所示：

- `apex_manifest.json`
- `AndroidManifest.xml`
- `apex_payload.img`
- `apex_pubkey`

`apex_manifest.json` 文件包含软件包名称和版本，可标识 APEX 文件。

`AndroidManifest.xml` 文件允许 APEX 文件使用与 APK 相关的工具和基础架构，例如 ADB、PackageManager 和软件包安装程序应用（如 Play 商店）。例如，APEX 文件可以使用现有工具（如 `aapt`）来检查文件中的基本元数据。该文件包含软件包名称和版本信息。这些信息通常也可以在 `apex_manifest.json` 中找到。

对于处理 APEX 的新代码和系统，建议使用 `apex_manifest.json`，而非 `AndroidManifest.xml`。`AndroidManifest.xml` 可能包含可供现有应用发布工具使用的其他定位信息。

`apex_payload.img` 是由 dm-verity 支持的 ext4 文件系统映像。该映像通过环回设备在运行时装载。具体而言，哈希树和元数据块是使用 `libavb` 库创建的。因为该映像应该可以装载到位，所以文件系统载荷不会进行解析。常规文件包含在 `apex_payload.img` 文件中。

`apex_pubkey` 是用于为文件系统映像签名的公钥。在运行时，此密钥可确保使用为内置分区中的相同 APEX 签名的同一实体为已下载的 APEX 签名。

### APEX 管理器

APEX 管理器（即 `apexd`）是一个独立的原生进程，负责验证、安装和卸载 APEX 文件。此进程已启动，并在引导序列早期准备就绪。APEX 文件通常预安装在设备的 `/system/apex` 下。如果没有可用的更新，APEX 管理器默认使用这些软件包。

APEX 的更新序列使用 [PackageManager 类](https://developer.android.com/reference/android/content/pm/PackageManager)，如下所示。

1. 通过软件包安装程序应用、ADB 或其他来源下载 APEX 文件。
2. 软件包管理器启动安装过程。在识别出该文件是 APEX 文件后，软件包管理器会将控制权转交给 APEX 管理器。
3. APEX 管理器验证 APEX 文件。
4. 如果 APEX 文件通过验证，则 APEX 管理器的内部数据库会进行更新，以反映 APEX 文件会在下次启动时激活。
5. 在成功验证软件包后，安装请求者接收广播。
6. 为了继续安装，系统必须重新启动。
7. 下次启动时，APEX 管理器会启动、读取内部数据库，并针对列出的每个 APEX 文件执行以下操作：
    1. 验证 APEX 文件。
    2. 从 APEX 文件创建环回设备。
    3. 在环回设备之上创建设备映射器块设备。
    4. 将设备映射器块设备装载到唯一路径（例如 `/apex/name@ver`）上。

装载内部数据库中列出的所有 APEX 文件后，APEX 管理器为其他系统组件提供 Binder 服务，以查询有关已安装的 APEX 文件的信息。例如，其他系统组件可以查询设备中安装的 APEX 文件列表，也可以查询装载特定 APEX 的确切路径，以便可以访问这些文件。

### APEX 文件是 APK 文件

APEX 文件是有效的 APK 文件，因为它们是包含 `AndroidManifest.xml` 文件的已签名 ZIP 归档文件（使用 APK 签名方案）。这允许 APEX 文件使用 APK 文件的基础架构，例如软件包安装程序应用、签名实用程序和软件包管理器。

APEX 文件中的 `AndroidManifest.xml` 文件是最小的，由软件包 `name`、`versionCode` 以及用于精细定位的可选 `targetSdkVersion`、`minSdkVersion` 和 `maxSdkVersion` 组成。这些信息允许通过已有渠道（如软件包安装程序应用和 ADB）传递 APEX 文件。

### 支持的文件类型

APEX 格式支持以下文件类型：

- 原生共享库
- 原生可执行文件
- JAR 文件
- 数据文件
- 配置文件

这并不意味着 APEX 可以更新所有这些文件类型。能否更新某个文件类型，取决于具体平台以及这些文件类型的接口定义的稳定性如何。

### 签名

可通过两种方式为 APEX 文件签名。第一种方式是使用密钥为 `apex_payload.img`（具体来说是附加到 `apex_payload.img` 的 vbmeta 描述符）文件签名。第二种方式是使用 [APK 签名方案 v3](https://source.android.com/security/apksigning/v3) 为整个 APEX 签名。在此过程中使用两个不同的密钥。

在设备端，安装了与用于为 vbmeta 描述符签名的私钥对应的公钥。APEX 管理器使用该公钥来验证请求安装的 APEX。必须使用不同的密钥为每个 APEX 签名，并在构建时和运行时强制执行此操作。

### 内置分区中的 APEX

APEX 文件可以位于内置分区（如 `/system`）中。该分区已通过 dm-verity 验证，因此 APEX 文件会直接装载到环回设备上。

如果内置分区中存在 APEX，可以通过提供具有相同软件包名称和更高或相同版本代码的 APEX 软件包来更新 APEX。新的 APEX 存储在 `/data` 中，与 APK 类似，新安装的版本会替换内置分区中已存在的版本。但与 APK 不同的是，新安装的 APEX 版本仅在重新启动后才会激活。

## 内核要求

要在 Android 设备上支持 APEX Mainline 模块，需要以下 Linux 内核功能：环回驱动程序和 dm-verity。环回驱动程序将文件系统映像装载到某个 APEX 模块中，然后 dm-verity 会验证该 APEX 模块。

在使用 APEX 模块时，环回驱动程序和 dm-verity 的性能对于实现良好的系统性能来说非常重要。

### 支持的内核版本

使用内核版本 4.4 或更高版本的设备支持 APEX Mainline 模块。搭载 Android 10 或更高版本的新设备必须使用内核版本 4.9 或更高版本来支持 APEX 模块。

### 必需的内核补丁程序

用于支持 APEX 模块所必需的内核补丁程序包含在 Android 公共树中。要获得补丁程序以支持 APEX，请使用最新版本的 Android 公共树。

#### 内核版本 4.4

只有从 Android 9 升级到 Android 10 且要支持 APEX 模块的设备支持此版本。如需获得必需的补丁程序，强烈建议从 `android-4.4` 分支向下合并。以下是内核版本 4.4 所需的各个补丁程序的列表。

- UPSTREAM: loop: add ioctl for changing logical block size ([4.4](https://android-review.googlesource.com/c/kernel/common/+/777013))
- BACKPORT: block/loop: set hw_sectors ([4.4](https://android-review.googlesource.com/c/kernel/common/+/777014/7))
- UPSTREAM: loop: Add LOOP_SET_BLOCK_SIZE in compat ioctl ([4.4](https://android-review.googlesource.com/c/kernel/common/+/777015/7))
- ANDROID: mnt: Fix next_descendent ([4.4](https://android-review.googlesource.com/c/kernel/common/+/405314))
- ANDROID: mnt: remount should propagate to slaves of slaves ([4.4](https://android-review.googlesource.com/c/kernel/common/+/320406))
- ANDROID: mnt: Propagate remount correctly ([4.4](https://android-review.googlesource.com/c/kernel/common/+/928253))
- Revert "ANDROID: dm verity: add minimum prefetch size" ([4.4](https://android-review.googlesource.com/c/kernel/common/+/867875))
- UPSTREAM: loop: drop caches if offset or block_size are changed ([4.4](https://android-review.googlesource.com/c/kernel/common/+/854265))

#### 内核版本 4.9/4.14/4.19

如需获得内核版本 4.9/4.14/4.19 所必需的补丁程序，请从 `android-common` 分支向下合并。

### 必需的内核配置选项

以下列表显示了支持 Android 10 中引入的 APEX 模块的基本配置要求。带星号 (*) 的项是 Android 9 及更低版本的现有要求。

```
(*) CONFIG_AIO=Y # AIO support (for direct I/O on loop devices)
CONFIG_BLK_DEV_LOOP=Y # for loop device support
CONFIG_BLK_DEV_LOOP_MIN_COUNT=16 # pre-create 16 loop devices
(*) CONFIG_CRYPTO_SHA1=Y # SHA1 hash for DM-verity
(*) CONFIG_CRYPTO_SHA256=Y # SHA256 hash for DM-verity
CONFIG_DM_VERITY=Y # DM-verity support
```

### 内核命令行参数要求

如需支持 APEX，请确保内核命令行参数满足以下要求；

- 不得设置 `loop.max_loop`
- `loop.max_part` 必须小于等于 8

## 构建 APEX

本部分介绍了如何使用 Android 构建系统构建 APEX。下面的示例展示了名为 `apex.test` 的 APEX 的 `Android.bp`。

```
apex {
    name: "apex.test",
    manifest: "apex_manifest.json",
    file_contexts: "file_contexts",
    // libc.so and libcutils.so are included in the apex
    native_shared_libs: ["libc", "libcutils"],
    binaries: ["vold"],
    java_libs: ["core-all"],
    prebuilts: ["my_prebuilt"],
    compile_multilib: "both",
    key: "apex.test.key",
    certificate: "platform",
}
```

`apex_manifest.json` 示例：

```
{
  "name": "com.android.example.apex",
  "version": 1
}
```

`file_contexts` 示例：

```
(/.*)?           u:object_r:system_file:s0
/sub(/.*)?       u:object_r:sub_file:s0
/sub/file3       u:object_r:file3_file:s0
```

#### APEX 中的文件类型及其位置

| 文件类型   | 在 APEX 中的位置                                             |
| :--------- | :----------------------------------------------------------- |
| 共享库     | `/lib` 和 `/lib64`（在 x86 中，翻译后的 ARM 的位置为 `/lib/arm`） |
| 可执行文件 | `/bin`                                                       |
| Java 库    | `/javalib`                                                   |
| 预编译文件 | `/etc`                                                       |

### 传递依赖项

APEX 文件自动包含原生共享库或可执行文件的传递依赖项。例如，如果 `libFoo` 依赖于 `libBar`，则仅当 `libFoo` 在 `native_shared_libs` 属性中列出时才会包含这两个库。

### 处理多个 ABI

为设备的主应用二进制接口 (ABI) 和辅助 ABI 安装 `native_shared_libs` 属性。如果 APEX 以具有单个 ABI 的设备（即仅 32 位或仅 64 位）为目标平台，则仅安装具有相应 ABI 的库。

仅为设备的主 ABI 安装 `binaries` 属性，如下所述：

- 如果设备仅支持 32 位 ABI，则仅安装二进制文件的 32 位变体。
- 如果设备仅支持 64 位 ABI，则仅安装二进制文件的 64 位变体。

如需更为精细地控制原生库和二进制文件的 ABI，请使用 `multilib.[first|lib32|lib64|prefer32|both].[native_shared_libs|binaries]` 属性。

- `first`：匹配设备的主 ABI。这是二进制文件的默认值。
- `lib32`：匹配设备的 32 位 ABI（如果支持）。
- `lib64`：匹配设备的 64 位 ABI（如果支持）。
- `prefer32`：匹配设备的 32 位 ABI（如果支持）。如果不支持 32 位 ABI，则匹配 64 位 ABI。
- `both`：匹配 32 位和 64 位 ABI。这是 `native_shared_libraries` 的默认值。

`java`、`libraries` 和 `prebuilts` 属性与 ABI 无关。

下面的示例展示了支持 32 位和 64 位 ABI 且不优先使用 32 位 ABI 的设备：

```
apex {
    // other properties are omitted
    native_shared_libs: ["libFoo"], // installed for 32 and 64
    binaries: ["exec1"], // installed for 64, but not for 32
    multilib: {
        first: {
            native_shared_libs: ["libBar"], // installed for 64, but not for 32
            binaries: ["exec2"], // same as binaries without multilib.first
        },
        both: {
            native_shared_libs: ["libBaz"], // same as native_shared_libs without multilib
            binaries: ["exec3"], // installed for 32 and 64
        },
        prefer32: {
            native_shared_libs: ["libX"], // installed for 32, but not for 64
        },
        lib64: {
            native_shared_libs: ["libY"], // installed for 64, but not for 32
        },
    },
}
```

### vbmeta 签名

使用不同的密钥为每个 APEX 签名。需要新密钥时，可创建公钥-私钥对并创建 `apex_key` 模块。使用 `key` 属性为使用该密钥的 APEX 签名。公钥自动包含在 APEX 中，名为 `avb_pubkey`。

```
# create an rsa key pair
openssl genrsa -out foo.pem 4096

# extract the public key from the key pair
avbtool extract_public_key --key foo.pem --output foo.avbpubkey

# in Android.bp
apex_key {
    name: "apex.test.key",
    public_key: "foo.avbpubkey",
    private_key: "foo.pem",
}
```

在上述示例中，公钥的名称 (`foo`) 成为密钥的 ID。用于为 APEX 签名的密钥 ID 采用 APEX 格式。在运行时，`apexd` 使用设备中具有相同 ID 的公钥验证 APEX。

### ZIP 签名

使用为 APK 签名的方式为 APEX 签名。为 APEX 进行两次签；一次针对迷你文件系统（`apex_payload.img` 文件），另一次针对整个文件。

如需在文件级别为 APEX 签名，请按以下三种方式之一设置 `certificate` 属性：

- 未设置：如果未设置任何值，则使用位于 `PRODUCT_DEFAULT_DEV_CERTIFICATE` 的证书为 APEX 签名。如果未设置任何标志，路径默认为 `build/target/product/security/testkey`。
- `<name>`：使用 `PRODUCT_DEFAULT_DEV_CERTIFICATE` 所在目录中的 `<name>` 证书为 APEX 签名。
- `:<name>`：使用由名为 `<name>` 的 Soong 模块定义的证书为 APEX 签名。该证书模块可定义如下。

```
android_app_certificate {
    name: "my_key_name",
    certificate: "dir/cert",
    // this will use dir/cert.x509.pem (the cert) and dir/cert.pk8 (the private key)
}
```

**注意**：`key` 和 `certificate` 值不需要从相同的公钥/私钥对中派生。APEX 是一种 APK，因此需要 APK 签名（由 `certificate` 指定）。

## 安装 APEX

要安装 APEX，请使用 ADB。

```
adb install apex_file_name
adb reboot
```

## 使用 APEX

重新启动后，APEX 会装载到 `/apex/<apex_name>@<version>` 目录中。可以同时装载同一 APEX 的多个版本。在装载路径中，对应于最新版本的路径绑定装载到 `/apex/<apex_name>`。

客户端可以使用绑定装载路径从 APEX 读取或执行文件。

通常可按如下方式使用 APEX：

1. OEM 或 ODM 会在设备出厂时在 `/system/apex` 下预加载 APEX。
2. 通过 `/apex/<apex_name>/` 路径访问 APEX 中的文件。
3. 在 `/data/apex` 中安装 APEX 的更新后版本后，该路径将在重新启动后指向新的 APEX。

### 使用 APEX 更新服务

要使用 APEX 更新服务，请执行以下操作：

1. 将 system 分区中的服务标记为可更新。将 `updatable` 选项添加到服务定义中。

    ```
    /system/etc/init/myservice.rc:
    
    service myservice /system/bin/myservice
        class core
        user system
        ...
        updatable
    ```

2. 为更新后的服务创建新的 `.rc` 文件。使用 `override` 选项重新定义现有服务。

    ```
    /apex/my.apex/etc/init.rc:
    
    service myservice /apex/my.apex/bin/myservice
        class core
        user system
        ...
        override
    ```

只能在 APEX 的 `.rc` 文件中定义服务定义。APEX 不支持操作触发器。

如果标记为可更新的服务在 APEX 激活之前启动，启动会延迟，直到 APEX 的激活过程完成为止。

## 配置系统以支持 APEX 更新

将以下系统属性设置为 `true` 以支持 APEX 文件更新。

```
<device.mk>:

PRODUCT_PROPERTY_OVERRIDES += ro.apex.updatable=true

BoardConfig.mk:
TARGET_FLATTEN_APEX := false
```

或者仅设置

```
<device.mk>:

$(call inherit-product, $(SRC_TARGET_DIR)/product/updatable_apex.mk)
```

## 扁平化 APEX

对于旧版设备，通过更新旧内核来完全支持 APEX 有时无法执行或不可行。例如，内核可能是在没有 `CONFIG_BLK_DEV_LOOP=Y` 的情况下构建的，这对于在 APEX 中装载文件系统映像至关重要。

扁平化 APEX 是专门构建的 APEX，可在使用旧版内核的设备上激活。扁平化 APEX 中的文件直接安装到内置分区下的目录中。例如，扁平化 APEX `my.apex` 中的 `lib/libFoo.so` 安装到 `/system/apex/my.apex/lib/libFoo.so`。

激活扁平化 APEX 不需要使用循环设备。整个目录 `/system/apex/my.apex` 直接绑定装载到 `/apex/name@ver`。

无法通过从网络下载 APEX 的更新版本来更新扁平化 APEX，因为下载的 APEX 无法经过扁平化处理。只能通过常规 OTA 更新扁平化 APEX。

扁平化 APEX 是默认配置。这意味着，除非您将设备明确配置为编译非扁平化 APEX 以支持 APEX 更新（如上所述），否则所有 APEX 都默认经过扁平化处理。

不支持在设备中混用扁平化和非扁平化 APEX。设备中的 APEX 要么必须全部为扁平化，要么必须全部为非扁平化。在为 Mainline 等项目提供预签名的 APEX 预构建文件时，这一点尤其重要。未预签名（即从源代码构建）的 APEX 也应该是非扁平化的，并使用正确的密钥进行签名。设备应从 `updatable_apex.mk` 继承，如[使用 APEX 更新服务](https://source.android.com/devices/tech/ota/apex#updating_a_service_with_an_apex)中所述。

## 已压缩的 APEX

Android 12 及更高版本提供 APEX 文件压缩功能，用于减少可更新的 APEX 软件包对存储空间的影响。安装 APEX 的更新后，虽然其预安装版本不会再使用，但占用的空间量不变。被占用的空间仍然不可用。

APEX 文件压缩功能在只读分区（例如 `/system` 分区）中使用一组经过高度压缩的 APEX 文件，最大限度地降低了对存储空间的这种影响。Android 12 及更高版本使用 DEFLATE zip 压缩算法。

压缩并不会优化以下各项：

- 需要在启动序列早期装载的引导 APEX。
- 不可更新的 APEX。仅当 `/data` 分区上安装了 APEX 的更新版本时，压缩才是有益的。[模块化系统组件](https://source.android.com/devices/architecture/modular-system)页面上提供了可更新 APEX 的完整列表。
- 动态共享库 APEX。由于 `apexd` 始终会激活此类 APEX 的两个版本（预安装版本和升级后的版本），因此压缩它们不会带来更多价值。

### 已压缩 APEX 文件的格式

以下是已压缩 APEX 文件的格式。

![显示已压缩 APEX 文件的格式的图示](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151713292.png)

**图 2.** 已压缩 APEX 文件的格式

在顶层，已压缩的 APEX 文件是一个 zip 文件，它包含原始 apex 文件（采用 deflate 压缩方式，压缩级别为 9），以及在未压缩的情况下存储的其他文件。

以下四个文件构成一个 APEX 文件：

- `original_apex`：采用 deflate 压缩方式，压缩级别为 9。这是未经压缩的原始 [APEX 文件](https://source.android.com/devices/tech/ota/apex#apex-format)。
- `apex_manifest.pb`：仅存储
- `AndroidManifest.xml`：仅存储
- `apex_pubkey`：仅存储

`apex_manifest.pb`、`AndroidManifest.xml` 和 `apex_pubkey` 文件是其在 `original_apex` 中的对应文件的副本。

### 构建已压缩的 APEX

您可以使用位于 [`system/apex/tools`](https://android.googlesource.com/platform/system/apex/+/refs/heads/master/tools/) 的 `apex_compression_tool.py` 工具构建已压缩的 APEX。

**注意**：系统不会自动为生成的已压缩 APEX 文件的外部 apk 容器签名。您必须手动为其签名，并使用正确的证书。请参阅[对要发布的 build 进行签名](https://source.android.com/devices/tech/ota/sign_builds#apex-signing-key-replacement)。

构建系统中提供了与 APEX 文件压缩功能相关的一些参数。

在 `Android.bp` 中，APEX 文件是否可压缩由 `compressible` 属性控制：

```
apex {
    name: "apex.test",
    manifest: "apex_manifest.json",
    file_contexts: "file_contexts",
    compressible: true,
}
```

**注意**：这会告知构建系统此 APEX 可以压缩。必须执行此操作，因为并非所有 APEX 都是可压缩的，如[已压缩的 APEX](https://source.android.com/devices/tech/ota/apex#compressed-apex) 部分中所述。

`PRODUCT_COMPRESSED_APEX` 产品标志用于控制在源代码的基础上构建的系统映像是否必须包含已压缩的 APEX 文件。

对于本地实验，您可以通过将 `OVERRIDE_PRODUCT_COMPRESSED_APEX=` 设置为 `true` 来强制 build 压缩 APEX。

构建系统生成的已压缩的 APEX 文件具有 `.capex` 扩展名。此扩展名可让您更轻松地区分 APEX 文件的已压缩版本和未压缩版本。

#### 支持的压缩算法

Android 12 仅支持 deflate-zip 压缩。

#### 在启动期间激活已压缩的 APEX 文件

在可以激活已压缩的 APEX 文件之前，须将其中的 `original_apex` 文件解压缩到 `/data/apex/decompressed` 目录中。解压缩后的 APEX 文件会硬链接到 `/data/apex/active` 目录。

**注意**：由于解压缩后的 APEX 文件会硬链接到 `/data/apex/active` 目录，因此 `/data/apex/decompressed` 下的文件必须与 `/data/apex/active` 目录下的文件具有相同的 SELinux 标签。



为了演示上述流程，我们提供了下面的示例。

将 `/system/apex/com.android.foo.capex` 视为要激活的已压缩 APEX 文件，版本代码为 37。

1. `/system/apex/com.android.foo.capex` 中的 `original_apex` 文件解压缩到 `/data/apex/decompressed/com.android.foo@37.apex`。
2. 执行 `restorecon /data/apex/decompressed/com.android.foo@37.apex` 以验证它是否具有正确的 SELinux 标签。
3. 对 `/data/apex/decompressed/com.android.foo@37.apex` 执行验证检查，以确保其有效性：`apexd` 会检查 `/data/apex/decompressed/com.android.foo@37.apex` 中捆绑的公钥，以验证它是否与 `/system/apex/com.android.foo.capex` 中捆绑的公钥相同。
4. `/data/apex/decompressed/com.android.foo@37.apex` 文件硬链接到 `/data/apex/active/com.android.foo@37.apex` 目录。
5. 在 `/data/apex/active/com.android.foo@37.apex` 上执行未压缩的 APEX 文件的常规激活逻辑。

#### 与 OTA 交互

已压缩的 APEX 文件会影响 OTA 传送和应用。由于 OTA 更新可能包含已压缩的 APEX 文件，其版本高于设备上当前使用的版本，因此在重新启动设备以应用 OTA 更新之前，必须预留一定的可用空间。

为了支持 OTA 系统，`apexd` 提供了以下两个 binder API：

- `calculateSizeForCompressedApex` - 计算解压缩 OTA 软件包中的 APEX 文件所需的大小。这可用于验证设备是否有足够的可用空间，然后再下载 OTA。
- `reserveSpaceForCompressedApex` - 在磁盘上预留空间，供 `apexd` 未来解压缩 OTA 软件包中的已压缩 APEX 文件时使用。

如果是 A/B OTA 更新，`apexd` 会在安装后 OTA 例程中尝试在后台进行解压缩。如果解压缩失败，`apexd` 会在应用 OTA 更新的启动过程中执行解压缩。

## 开发 APEX 时考虑的替代方案

以下是 AOSP 在设计 APEX 文件格式时考虑的一些选项，以及添加或排除这些选项的原因。

### 普通软件包管理系统

Linux 发行版提供 `dpkg` 和 `rpm` 等软件包管理系统，这些系统功能强大、成熟且稳健。不过，APEX 没有采用这些系统，因为它们在安装后无法保护软件包。只有在安装软件包时才会执行验证。攻击者可以破坏已安装软件包的完整性而不被察觉。这会造成 Android 性能下降，因为 Android 的所有系统组件都存储在只读文件系统中，其完整性受每个 I/O 的 dm-verity 保护。对系统组件的任何篡改都必须禁止或可以检测到，以便设备在遭到入侵时可以拒绝启动。

### dm-crypt 对确保完整性的作用

APEX 容器中的文件来自于受 dm-verity 保护的内置分区（例如 `/system` 分区），即使在装载分区后，也会禁止对文件进行任何修改。为了对文件提供相同强度的安全保护，APEX 中的所有文件都存储在使用哈希树和 vbmeta 描述符配对的文件系统映像中。如果没有 dm-verity，`/data` 分区中的 APEX 在经过验证和安装后，很容易遭到意外修改。

实际上，`/data` 分区也受到 dm-crypt 等加密层的保护。虽然这在一定程度上能够防止篡改，但其主要目的是保护隐私，而不是确保完整性。当攻击者获得对 `/data` 分区的访问权限时，此类加密层就无法提供进一步保护，与 `/system` 分区中的每个系统组件相比，这也会造成性能下降。APEX 文件中的哈希树与 dm-verity 一同提供相同程度的内容保护。

### 将路径从 /system 重定向到 /apex

打包在 APEX 中的系统组件文件可通过 `/apex/<name>/lib/libfoo.so` 等新路径访问。如果文件存储在 `/system` 分区中，可以通过 `/system/lib/libfoo.so` 等路径访问这些文件。APEX 文件的客户端（其他 APEX 文件或平台）必须使用新路径。由于路径发生更改，您可能需要更新现有代码。

虽然避免路径更改的一种方法是将 APEX 文件中的文件内容叠加到 `/system` 分区上，但 Android 团队决定不将文件叠加到 `/system` 分区上，因为随着叠加（甚至可能会接连堆叠）的文件数的增加，这会对性能造成负面影响。

另一个方法是劫持文件访问函数（如 `open`、`stat` 和 `readlink`），以便以 `/system` 开头的路径可以重定向到 `/apex` 下的相应路径。Android 团队舍弃了这一方法，因为更改所有接受路径的函数这种做法不可行。例如，一些应用会静态链接 Bionic，从而实现此类函数。在这类情况下，系统不会重定向这些应用。