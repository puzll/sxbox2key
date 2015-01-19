#include "JsEvDev.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/input.h>
#include <stddef.h>

JNIEXPORT jlong JNICALL Java_JsEvDev_open(JNIEnv* env, __attribute__((unused)) jclass unused, jstring filename)
{
    const char* filename_str = (**env).GetStringUTFChars(env, filename, NULL);
    if (filename_str == NULL) return -1;

    int fd = open(filename_str, O_RDONLY);

    (**env).ReleaseStringUTFChars(env, filename, filename_str);

    return fd;
}

JNIEXPORT jlong JNICALL Java_JsEvDev_close(__attribute__((unused)) JNIEnv *env, __attribute__((unused)) jclass unused, jlong fd)
{
    return close(fd);
}

JNIEXPORT jstring JNICALL Java_JsEvDev_getName(JNIEnv* env, __attribute__((unused)) jclass unused, jlong fd)
{
    char buf[256] = "Unknown";
    ioctl(fd, EVIOCGNAME(sizeof(buf)), buf);
    buf[sizeof(buf)-1] = 0;
    return (**env).NewStringUTF(env, buf);
}

JNIEXPORT jlong JNICALL Java_JsEvDev_getBits(JNIEnv* env, __attribute__((unused)) jclass unused, jlong fd, jshort type, jbyteArray bits)
{
    jsize len = (**env).GetArrayLength(env, bits);
    jbyte* buf = (**env).GetByteArrayElements(env, bits, NULL);
    if (buf == NULL) return -1;

    int result = ioctl(fd, EVIOCGBIT(type, len), buf);

    (**env).ReleaseByteArrayElements(env, bits, buf, 0);

    return result;
}

JNIEXPORT jlong JNICALL Java_JsEvDev_getAbs(JNIEnv* env, __attribute__((unused)) jclass unused, jlong fd, jshort code, jintArray info)
{
    jint* buf = (**env).GetIntArrayElements(env, info, NULL);
    if (buf == NULL) return -1;

    int result = ioctl(fd, EVIOCGABS(code), buf);

    (**env).ReleaseIntArrayElements(env, info, buf, 0);

    return result;
}

JNIEXPORT jboolean JNICALL Java_JsEvDev_read(JNIEnv* env, __attribute__((unused)) jclass unused, jlong fd, jobject buf)
{
    struct input_event* buf_ptr = (struct input_event*)(**env).GetDirectBufferAddress(env, buf);
    if (buf_ptr == NULL) return JNI_FALSE;

    return read(fd, buf_ptr, sizeof(struct input_event)) == sizeof(struct input_event);
}

JNIEXPORT jint JNICALL Java_JsEvDev_input_1event_1size(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz)
{
    return sizeof(struct input_event);
}

JNIEXPORT jint JNICALL Java_JsEvDev_input_1event_1type_1offset(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz)
{
    return offsetof(struct input_event, type);
}
