public class XTestFakeKey {
    public static native long openDisplay();
    public static native void closeDisplay(long display);
    public static native boolean queryExtension(long display);
    public static native void fakeKeyEvent(long display, long keycode, boolean isPress);
}
