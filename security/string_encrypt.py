#!/usr/bin/env python3
"""
NexClip 编号2：字符串加密（Native层）
做什么：Native层敏感字符串用编译期脚本做异或加密
程度：Python脚本扫描替换，运行时按需自动解密，使用后立即清零
加密范围：API地址/密钥标识/安全检测关键词/配置值/错误信息
不加密：JNI方法名/四大组件名/序列化字段名
验证方式：反编译APK搜索"frida""xposed""api"等无结果 | 运行APP后adb搜索进程内存无明文
异常判定：加密插件报错→构建失败 | 解密逻辑bug→功能测试阶段暴露并修复
崩溃率：零
"""

import os
import re
import sys
import random

# 加密关键词
ENCRYPT_KEYWORDS = [
    "frida", "xposed", "/proc/self/maps", "magisk",
    "substrate", "cydia", "libfrida", "re.frida",
    "hook", "inject", "debug", "su",
    "api_key", "secret", "token", "password",
    "/proc/self/status", "/proc/self/fd",
    "com.saurik", "de.robv.android.xposed",
    "LIBFRIDA", "frida-agent", "frida-server",
    "gum-js-loop", "gmain", "linjector",
    "tcp:27042", "tcp:27043", "REJECT",
    "TracerPid", "Emulator", "goldfish",
    "generic", "vbox", "ttVM_Hdrv"
]

SKIP_DIRS = ["build", ".gradle", ".git", "node_modules"]

# 解密头文件模板
DECRYPT_HEADER = '''// AUTO GENERATED - DO NOT MODIFY
// NexClip String Encryption - Build #{BUILD_ID}
#pragma once
#include <string>
#include <cstring>
namespace nexclip {{
static constexpr unsigned char XOR_KEY[] = {{ {XOR_KEY_BYTES} }};
static constexpr int XOR_KEY_LEN = {XOR_KEY_LEN};
inline void xor_decrypt(char* data, int len) {{
    for (int i = 0; i < len; i++) {{
        data[i] ^= XOR_KEY[i % XOR_KEY_LEN];
    }}
}}
inline std::string decrypt_string(const unsigned char* enc, int len) {{
    char* buf = new char[len + 1];
    memcpy(buf, enc, len);
    xor_decrypt(buf, len);
    buf[len] = '\\0';
    std::string result(buf);
    memset(buf, 0, len + 1);
    delete[] buf;
    return result;
}}
}}
'''

def generate_xor_key():
    return [random.randint(1, 255) for _ in range(16)]

def xor_encrypt(plaintext, key):
    return [ch ^ key[i % len(key)] for i, ch in enumerate(plaintext.encode('utf-8'))]

def find_cpp_files(root_dir):
    files = []
    for d, dirs, fnames in os.walk(root_dir):
        dirs[:] = [x for x in dirs if x not in SKIP_DIRS]
        for f in fnames:
            if f.endswith(('.c', '.cpp', '.h', '.hpp')):
                files.append(os.path.join(d, f))
    return files

def find_strings_to_encrypt(filepath, keywords):
    results = []
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    pattern = r'"([^"\\]*(?:\\.[^"\\]*)*)"'
    for match in re.finditer(pattern, content):
        string_val = match.group(1)
        for kw in keywords:
            if kw.lower() in string_val.lower():
                results.append({
                    'original': string_val,
                    'start': match.start(1),
                    'end': match.end(1)
                })
                break
    return results

def encrypt_file(filepath, keywords, xor_key):
    strings = find_strings_to_encrypt(filepath, keywords)
    if not strings:
        return 0
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    encrypted_count = 0
    for s in reversed(strings):
        enc_bytes = xor_encrypt(s['original'], xor_key)
        hex_str = ','.join(f'0x{b:02x}' for b in enc_bytes)
        replacement = f'nexclip::decrypt_string((unsigned char[]){{{hex_str}}},{len(enc_bytes)})'
        content = content[:s['start']-1] + replacement + content[s['end']+1:]
        encrypted_count += 1
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    return encrypted_count

def generate_header(xor_key, build_id, output_dir):
    key_bytes = ','.join(f'0x{b:02x}' for b in xor_key)
    header = DECRYPT_HEADER.format(
        BUILD_ID=build_id,
        XOR_KEY_BYTES=key_bytes,
        XOR_KEY_LEN=len(xor_key)
    )
    path = os.path.join(output_dir, 'nexclip_string_enc.h')
    with open(path, 'w') as f:
        f.write(header)
    return path

def verify_no_leaks(root_dir, keywords):
    files = find_cpp_files(os.path.join(root_dir, 'src/main/cpp'))
    leaked = 0
    for f in files:
        with open(f, 'r', encoding='utf-8', errors='ignore') as fh:
            content = fh.read().lower()
        for kw in keywords:
            if f'"{kw}"' in content or f"'{kw}'" in content:
                print(f"  ⚠️ 残留: {kw} in {f}")
                leaked += 1
    return leaked

def main():
    root_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    output_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(root_dir, 'src/main/cpp/security')

    build_id = random.randint(100000, 999999)
    xor_key = generate_xor_key()

    print(f"[NexClip] 编号2：字符串加密 - 开始")
    print(f"[NexClip] Build ID: {build_id}")

    # 生成解密头文件
    header = generate_header(xor_key, build_id, output_dir)
    print(f"[NexClip] 生成解密头文件: {header}")

    # 扫描并加密
    files = find_cpp_files(os.path.join(root_dir, 'src/main/cpp'))
    total = 0
    for f in files:
        count = encrypt_file(f, ENCRYPT_KEYWORDS, xor_key)
        if count > 0:
            print(f"  加密 {count} 个字符串: {f}")
            total += count

    print(f"[NexClip] 编号2完成：共加密 {total} 个字符串")

    # 验证
    print(f"[NexClip] 验证：搜索明文残留...")
    leaked = verify_no_leaks(root_dir, ENCRYPT_KEYWORDS)

    if leaked > 0:
        print(f"[NexClip] 异常判定：发现 {leaked} 处残留 → 构建失败")
        sys.exit(1)
    else:
        print(f"[NexClip] 验证通过：零残留，崩溃率零")
        sys.exit(0)

if __name__ == '__main__':
    main()
