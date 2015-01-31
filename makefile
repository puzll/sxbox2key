CFLAGS = -c -Wall -Wpedantic -Wextra -std=c11 -fpic -I"/usr/lib/jvm/java-7-openjdk-amd64/include"
LFLAGS = -fpic -shared

target/classes/Controller.class: main.scala target/classes/JsEvDev.class
	scalac main.scala -d target/classes -cp target/classes

target/classes/JsEvDev.class: JsEvDev.java target/libSXB2KJsEvDev.so
	mkdir -p target/classes
	javac JsEvDev.java -d target/classes

target/libSXB2KJsEvDev.so: target/JsEvDev.o
	gcc $(LFLAGS) target/JsEvDev.o -o target/libSXB2KJsEvDev.so

target/JsEvDev.o: JsEvDev.c JsEvDev.h
	mkdir -p target
	gcc JsEvDev.c $(CFLAGS) -o target/JsEvDev.o

JsEvDev.h: JsEvDev.java
	javah JsEvDev

sxbox2key.jar:
	cd target/classes && jar cfm ../sxbox2key.jar ../../manifest.txt *.class
