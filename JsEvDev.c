#include "JsEvDev.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/select.h>
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

static int pipefd[2];

JNIEXPORT jlong JNICALL Java_JsEvDev_read(JNIEnv* env, __attribute__((unused)) jclass unused, jlong fd, jobject buf)
{
    jlong len = (**env).GetDirectBufferCapacity(env, buf);
    if (len <= 0) return -1;

    void* buf_ptr = (**env).GetDirectBufferAddress(env, buf);
    if (buf_ptr == NULL) return -1;

    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(fd, &rfds);
    FD_SET(pipefd[0], &rfds);
    int nfds = (fd >= pipefd[0] ? fd : pipefd[0]) + 1;
    int cnt = select(nfds, &rfds, NULL, NULL, NULL);
    if (cnt <= 0) return -1;

    if (FD_ISSET(pipefd[0], &rfds)) return 0;
    if (FD_ISSET(fd, &rfds)) return read(fd, buf_ptr, len);
    return -1;
}

JNIEXPORT jlong JNICALL Java_JsEvDev_openPipe(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass unused)
{
    return pipe(pipefd);
}

JNIEXPORT void JNICALL Java_JsEvDev_closePipe(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass unused)
{
    if (pipefd[0]) close(pipefd[0]);
    if (pipefd[1]) close(pipefd[1]);
    pipefd[0] = 0;
    pipefd[1] = 0;
}

JNIEXPORT jlong JNICALL Java_JsEvDev_cancelOn(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass unused)
{
    char buf = 0;
    return write(pipefd[1], &buf, 1);
}

JNIEXPORT jlong JNICALL Java_JsEvDev_cancelOff(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass unused)
{
    char buf = 0;
    return read(pipefd[0], &buf, 1);
}

JNIEXPORT jint JNICALL Java_JsEvDev_input_1event_1size(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz)
{
    return sizeof(struct input_event);
}

JNIEXPORT jint JNICALL Java_JsEvDev_input_1event_1type_1offset(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz)
{
    return offsetof(struct input_event, type);
}
