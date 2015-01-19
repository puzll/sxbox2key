#include "XTestFakeKey.h"
#include <X11/Xlib.h>
#include <X11/extensions/XTest.h>

JNIEXPORT jlong JNICALL Java_XTestFakeKey_openDisplay(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz)
{
    return (jlong)XOpenDisplay(0);
}

JNIEXPORT void JNICALL Java_XTestFakeKey_closeDisplay(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz, jlong display)
{
    XCloseDisplay((Display*)display);
}

JNIEXPORT jboolean JNICALL Java_XTestFakeKey_queryExtension(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz, jlong display)
{
    int b[4];
    return XTestQueryExtension((Display*)display, b, b+1, b+2, b+3);
}

JNIEXPORT void JNICALL Java_XTestFakeKey_fakeKeyEvent(__attribute__((unused)) JNIEnv* env, __attribute__((unused)) jclass clazz, jlong display, jlong keycode, jboolean isPress)
{
    XTestFakeKeyEvent((Display*)display, keycode, isPress, 0);
}
