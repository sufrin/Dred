#!/bin/sh
#
#       If svn exists and the current directory is under svn control then 
#       output the latest svn revision number
#
if which svn > /dev/null 2> /dev/null && svn info 1>/dev/null 2>/dev/null
then
   svn info -R | grep Revision | sort | tail -1 | sed -e 's/Revision: //'
else 
   echo 99999
fi

