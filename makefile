#xinputtest: main.o
#	gcc -o xinputtest main.o -lXi -lX11

#main.o: main.c
#	gcc -c -Wall -Wpedantic -Wextra -std=c11 main.c

CFLAGS = -c -Wall -Wpedantic -Wextra -std=c11 -fpic -I"/usr/lib/jvm/java-7-openjdk-amd64/include"
LFLAGS = -fpic -shared

XTestFakeKey.class: XTestFakeKey.java JsEvDev.class libSXBox2Key.so
	javac XTestFakeKey.java

JsEvDev.class: JsEvDev.java
	javac JsEvDev.java

libSXBox2Key.so: JsEvDev.o XTestFakeKey.o
	gcc $(LFLAGS) JsEvDev.o XTestFakeKey.o -o libSXBox2Key.so

JsEvDev.o: JsEvDev.c JsEvDev.h
	gcc JsEvDev.c $(CFLAGS)

JsEvDev.h: JsEvDev.java
	javah JsEvDev

XTestFakeKey.o: XTestFakeKey.c XTestFakeKey.h
	gcc XTestFakeKey.c $(CFLAGS)

XTestFakeKey.h: XTestFakeKey.java
	javah XTestFakeKey
