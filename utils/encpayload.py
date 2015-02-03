#!/usr/bin/env python

import os
import sys
import datetime
import time
import hashlib
import re
import ntpath
import subprocess


def process_payload(file, outfile, key, encrypter):
    if os.path.exists(outfile):
        os.remove(outfile)
    # print "encrypting:\n%s crypt %s %s \"%s\"" % (encrypter, file, outfile, key)
    p = subprocess.Popen([encrypter, 'crypt', file, outfile, key], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    if os.path.exists(outfile):
        print "done %s" % outfile


def check_dir(dir):
    if not os.path.exists(dir):
        d = os.mkdir(dir)
    if os.path.exists(dir):
        return True
    return False


def unrot13(list):
    result = ""
    # Loop over number.
    for v in list:
        # Convert to number with ord.
        c = int(v)
        c ^= 0x11
        result += chr(c)

    # print "%s rotted to :\"%s\"" % (list, result)
    # Return transformation.
    return result


# search for this patter:  int pass[] = {114,120,112,126,49,124,126,127,117,126};
# in order to isolate the numbers sequence


def read_key(file):
    if not os.path.exists(file):
        return -1, ""
    with open(file) as f:
        for line in f:
            m = re.search(r'int pass\[\] = {(.*)}', line)
            if m:
                # print  m.group(1)
                return 0, unrot13(m.group(1).split(","))
    return 1, ""


def usage():
    print("usage :")
    print("\t<inputDir> <suffix=.gz>")


if __name__ == '__main__':

    if len(sys.argv) < 2:
        usage()
        sys.exit()

    # print "sys.argv[1]=%s" % sys.argv[1]
    inDir = sys.argv[1]
    if len(sys.argv) > 2:
        suffix = sys.argv[2]
    else:
        suffix = ".gz"
    # parse the key from rc_4*/
    encrypter = "./tfc"
    (res, key) = read_key("../src/libbson/preprocessed/include/rc4_enc.h")
    if res != 0:
        print "error recovering key"
    exit
    for file in os.listdir(inDir):
        if file.endswith(suffix):
            print "skipping %s -> %s" % (inDir, file)
        else:
            # print "processing %s -> %s" %(inDir+"/"+file, inDir+"/"+file+outDir)
            process_payload(inDir + "/" + file, inDir + "/" + file + suffix, key, encrypter)
