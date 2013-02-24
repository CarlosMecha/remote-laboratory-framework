GXX = gcc
PROGR = libtool
LIBS = sqlite3
CFLAGS = -Wall -c -l$(LIBS)

all: $(PROGR).so

$(PROGR).so :
	$(GXX) $(CFLAGS) -o $(PROGR).so $(PROGR).c

clean:
	-rm -f *.so  *~ $(PROGS)

