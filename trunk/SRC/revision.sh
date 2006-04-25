#!/bin/sh
#################################################################################
#
#       If a file under svn control has changed revision recently 
#       then generate a new REVISION.java with the latest svn revision and
#       date as Strings.    
#
#################################################################################

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
OLDREVISION=`cat build.revision`
#
#
#
if [ "$OLDREVISION" = "$REVISION" ]
then
   echo Using REVISION.java "($REVISION)"
   exit
fi
echo Generating REVISION.java "($REVISION)"
echo $REVISION > build.revision
#
#
#
cat > BUILD/org/sufrin/dred/REVISION.java <<END
package org.sufrin.dred;
class REVISION
{ 
  public static String number = "$REVISION";
  public static String date   = "$DATE";
}
END


