#
# Starts up the dred application if necessary, then signals it to open files
# 
DRED=/Applications/AppleDred.app/Contents/Resources/Java/AppleDred.jar
if java -jar $DRED --serving > /dev/null
then 
   echo -n
else 
   echo Starting AppleDred
   open -a AppleDred
   sleep 2
fi
java -jar $DRED "$@" 












