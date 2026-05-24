# ============================================
# NexClip 构建期防护 - CMake配置
# 编号8：符号表Strip（编译参数）
# 编号9：日志清除（Native层）
# 编号28：控制流平坦化（OLLVM）
# ============================================

# ===== 编号8：符号表Strip =====
# 做什么：去除SO文件中所有符号信息，让IDA/Ghidra只能看到sub_xxxxxxx格式
# 程度：-fvisibility=hidden，-fno-unwind-tables，-fno-rtti，-fno-exceptions，
#       -ffunction-sections+链接脚本随机排列，-s
# 崩溃率：零

# 所有符号默认隐藏
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden")

# 去除异常展开表
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fno-unwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-unwind-tables")

# 去除RTTI
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti")

# 去除异常
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions")

# 函数分段（配合链接脚本随机排列）
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections")

# strip所有
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -s")

# ===== 编号9：日志清除（Native层）=====
# 做什么：Native层日志宏编译为空操作
# 程度：#define NDEBUG下所有LOG宏编译为((void)0)
# 崩溃率：零

add_definitions(-DNDEBUG)

# Android日志宏全部替换为空
add_definitions(-D__android_log_print(...)=((void)0))
add_definitions(-D__android_log_vprint(...)=((void)0))
add_definitions(-D__android_log_write(...)=((void)0))
add_definitions(-DLOGV(...)=((void)0))
add_definitions(-DLOGD(...)=((void)0))
add_definitions(-DLOGI(...)=((void)0))
add_definitions(-DLOGW(...)=((void)0))
add_definitions(-DLOGE(...)=((void)0))
add_definitions(-DALOGV(...)=((void)0))
add_definitions(-DALOGD(...)=((void)0))
add_definitions(-DALOGI(...)=((void)0))
add_definitions(-DALOGW(...)=((void)0))
add_definitions(-DALOGE(...)=((void)0))

# ===== 编号28：控制流平坦化 =====
# 做什么：用OLLVM的Flattening Pass，函数控制流从if-else变成巨型switch-case状态机
# 程度：编译选项-mllvm -fla
# 应用范围：所有detect_*.c检测函数/crypto/*.c加解密函数/VM解释器核心循环/
#           签名验证/完整性校验/决策逻辑函数
# 不应用：视频处理编解码/UI渲染/模型推理纯计算
# 验证方式：IDA打开SO找到安全函数F5反编译应看到巨型switch-case不可阅读
# 异常判定：CI/CD反编译目标函数确认平坦化生效→未检测到则构建失败
# 崩溃率：零

# 平坦化编译选项
set(FLATTEN_FLAGS "-mllvm -fla")

# 安全检测函数源码（平坦化编译）
file(GLOB SECURITY_DETECT_SOURCES "src/main/cpp/security/detect_*.c")
file(GLOB SECURITY_DETECT_CPP_SOURCES "src/main/cpp/security/detect_*.cpp")
file(GLOB SECURITY_CRYPTO_SOURCES "src/main/cpp/security/crypto/*.c")
file(GLOB SECURITY_CRYPTO_CPP_SOURCES "src/main/cpp/security/crypto/*.cpp")
file(GLOB SECURITY_VM_SOURCES "src/main/cpp/security/vm/*.c")
file(GLOB SECURITY_VM_CPP_SOURCES "src/main/cpp/security/vm/*.cpp")
file(GLOB SECURITY_VERIFY_SOURCES "src/main/cpp/security/verify/*.c")
file(GLOB SECURITY_VERIFY_CPP_SOURCES "src/main/cpp/security/verify/*.cpp")

# 合并所有安全相关源码
set(ALL_SECURITY_SOURCES
    ${SECURITY_DETECT_SOURCES}
    ${SECURITY_DETECT_CPP_SOURCES}
    ${SECURITY_CRYPTO_SOURCES}
    ${SECURITY_CRYPTO_CPP_SOURCES}
    ${SECURITY_VM_SOURCES}
    ${SECURITY_VM_CPP_SOURCES}
    ${SECURITY_VERIFY_SOURCES}
    ${SECURITY_VERIFY_CPP_SOURCES}
)

if(ALL_SECURITY_SOURCES)
    add_library(security_static STATIC ${ALL_SECURITY_SOURCES})
    # 程度：安全相关SO编译时启用平坦化
    set_target_properties(security_static PROPERTIES
        COMPILE_FLAGS "${FLATTEN_FLAGS}"
    )
    target_link_libraries(security_static log)
    message(STATUS "[NexClip] 编号28：安全模块已启用控制流平坦化")
else()
    message(STATUS "[NexClip] 编号28：未找到安全模块源码，跳过平坦化")
endif()

# 不应用平坦化的模块（纯计算）
# 视频编解码/渲染/模型推理/时间轴引擎 不加-mllvm -fla
# 这些模块使用默认CMAKE_C_FLAGS编译，不含平坦化
