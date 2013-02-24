#!/bin/bash
cd lib;
make clean && make;
cd ..;
gcc -Wall -o rlfmusic rlfmusic.c lib/libtool.so -lsqlite3;
