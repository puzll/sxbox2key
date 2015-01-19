#include <X11/Xlib.h>
#include <X11/extensions/XInput.h>
#include <stdio.h>

int main()
{
    int fd = open("/dev/input/event14", O_CLOEXEC, O_RDONLY);
    if (fd == -1) {
        fprintf(stderr, "Failed to open device");
        return 1;
    }

    ioctl(fd);

    close(fd);

    /*Display* display = XOpenDisplay(NULL);
    if (display == NULL) {
        fprintf(stderr, "Failed to open display");
        return 1;
    }

    //Window w = DefaultRootWindow(display);

    int ndevices = 0;
    XDeviceInfo* devices = XListInputDevices(display, &ndevices);

    for (int i = 0; i < ndevices; ++i)
        printf("%s\n", devices[i].name);

    if (devices != NULL) XFreeDeviceList(devices);

    XCloseDisplay(display);*/
}
