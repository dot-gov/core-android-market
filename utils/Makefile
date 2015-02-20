ALL:tfc

CPPFLAGS=-fpermissive
tfc:encrypt.o util_hexdump.c
	g++ $(CFLAGS) $(CPPFLAGS) -o $@ $?
%.o: %.c
	g++ -c $(CFLAGS) $(CPPFLAGS) $< -o $@

clean:
	rm *.o tfc