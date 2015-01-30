import java.nio.ByteBuffer;

public class JsEvDev {

    public static final int EV_KEY = 0x01;
    public static final int EV_ABS = 0x03;
    public static final int EV_MAX = 0x1f;
    public static final int EV_CNT = (EV_MAX+1);

    public static final int BTN_SOUTH = 0x130;
    public static final int BTN_A = BTN_SOUTH;
    public static final int BTN_EAST = 0x131;
    public static final int BTN_B = BTN_EAST;
    public static final int BTN_C = 0x132;
    public static final int BTN_NORTH = 0x133;
    public static final int BTN_X = BTN_NORTH;
    public static final int BTN_WEST = 0x134;
    public static final int BTN_Y = BTN_WEST;
    public static final int BTN_Z = 0x135;
    public static final int BTN_TL = 0x136;
    public static final int BTN_TR = 0x137;
    public static final int BTN_TL2 = 0x138;
    public static final int BTN_TR2 = 0x139;
    public static final int BTN_SELECT = 0x13a;
    public static final int BTN_START = 0x13b;
    public static final int BTN_MODE = 0x13c;
    public static final int BTN_THUMBL = 0x13d;
    public static final int BTN_THUMBR = 0x13e;
    public static final int KEY_MAX = 0x2ff;
    public static final int KEY_CNT = (KEY_MAX+1);

    public static final int ABS_X = 0x00;
    public static final int ABS_Y = 0x01;
    public static final int ABS_Z = 0x02;
    public static final int ABS_RX = 0x03;
    public static final int ABS_RY = 0x04;
    public static final int ABS_RZ = 0x05;
    public static final int ABS_HAT0X = 0x10;
    public static final int ABS_HAT0Y = 0x11;
    public static final int ABS_MAX = 0x3f;
    public static final int ABS_CNT = (ABS_MAX+1);

    public static final int INPUT_EVENT_SIZE = input_event_size();
    public static final int INPUT_EVENT_TYPE_OFFSET = input_event_type_offset();

    public static native long open(String filename);
    public static native long close(long fd);
    public static native String getName(long fd);
    public static native long getBits(long fd, short ev, byte[] bits);
    public static native long getAbs(long fd, short code, int[] info);
    public static native long read(long fd, ByteBuffer buf);
    public static native long openPipe();
    public static native void closePipe();
    public static native long cancelOn();
    public static native long cancelOff();
    public static native int input_event_size();
    public static native int input_event_type_offset();
}
