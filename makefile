CFLAGS = -c -Wall -Wpedantic -Wextra -std=c11 -fpic -I"/usr/lib/jvm/java-7-openjdk-amd64/include"
LFLAGS = -fpic -shared

target/Controller.class: main.scala target/JsEvDev.class target/libSXB2KJsEvDev.so
	scalac main.scala -d target -cp target

target/JsEvDev.class: JsEvDev.java target/libSXB2KJsEvDev.so
	javac JsEvDev.java -d target -cp target

target/libSXB2KJsEvDev.so: target/JsEvDev.o
	gcc $(LFLAGS) target/JsEvDev.o -o target/libSXB2KJsEvDev.so

target/JsEvDev.o: JsEvDev.c JsEvDev.h
	mkdir -p target
	gcc JsEvDev.c $(CFLAGS) -o target/JsEvDev.o

JsEvDev.h: JsEvDev.java
	javah JsEvDev
