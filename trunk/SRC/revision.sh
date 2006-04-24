#!/bin/sh
#
#       If svn exists and the current directory is under svn control then 
#       output the latest svn revision number
#
function revision() {
if which svn > /dev/null 2> /dev/null && svn info 1>/dev/null 2>/dev/null
then
   svn info -R | grep Revision | sort | tail -1 | sed -e 's/Revision: //'
else 
   echo "(no svn revision)"
fi
}

REVISION=`revision`
DATE=`date`
cat > BUILD/org/sufrin/dred/REVISION.java <<END
package org.sufrin.dred;
class REVISION
{ 
  public static String number = "$REVISION";
  public static String date   = "$DATE";
}
END
