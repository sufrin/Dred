#!/bin/env python
#
# [Fires up and] invokes the Dred editor on a file 
#
# dred [filenames] -- start a background edit session for each of the filenames
# ted  [filenames] -- ditto
# ted  filename    -- start (and wait for termination of) an edit session for the file
#
#
import httplib
import os
from sys      import argv, exit
from os       import getcwd
from os.path  import abspath
from time     import sleep
#
#       Decide which socket to use
#
def chooseSocket():
   """
        We need one dred server per user per X server on a given machine.
        We should really ask a broker to choose the socket and establish the
        server dynamically.                       
   """
   if os.name is 'posix':
      return 60000+os.getuid()
   else:
      return 60000
#
#
#
#
#   
cwd    = getcwd()
socket = chooseSocket()
try:
   con = httplib.HTTPConnection('localhost:%s'%socket)
   con.connect()
except Exception,ex:
   # couldn't connect, so the server needs starting
   os.system("dredserver -socket=%s"%socket)
   # wait for the server to start listening; sorry about the race!
   sleep(3.5) 
   
try:
  # submit the editing request: it'll start a new session
  if len(argv)>1:
     for arg in argv[1:]:
         if con is None:
            con = httplib.HTTPConnection('localhost:%s'%socket)
            con.connect()            
         realfile = abspath(arg)
         req = con.request('GET', '/edit?FILE=%s&CWD=%s'%(realfile, cwd))
         if len(argv)>2: con = None
  else:
     req = con.request('GET', '/edit?CWD=%s'%cwd)
  if len(argv)==2 and argv[0].endswith("ted"):
     # wait until we get the end-session response
     res = con.getresponse()
     print res.read()
except Exception, ex:
     print ex
  












