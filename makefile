#xinputtest: main.o
#	gcc -o xinputtest main.o -lXi -lX11

#main.o: main.c
#	gcc -c -Wall -Wpedantic -Wextra -std=c11 main.c

CFLAGS = -c -Wall -Wpedantic -Wextra -std=c11 -fpic -I"/usr/lib/jvm/java-7-openjdk-amd64/include"
LFLAGS = -fpic -shared

Main.class: main.scala JsEvDev.class libSXB2KJsEvDev.so
	scalac main.scala

JsEvDev.class: JsEvDev.java libSXB2KJsEvDev.so
	javac JsEvDev.java

libSXB2KJsEvDev.so: JsEvDev.o
	gcc $(LFLAGS) JsEvDev.o -o libSXB2KJsEvDev.so

JsEvDev.o: JsEvDev.c JsEvDev.h
	gcc JsEvDev.c $(CFLAGS)

JsEvDev.h: JsEvDev.java
	javah JsEvDev
