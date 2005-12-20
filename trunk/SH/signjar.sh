#
# Make a signed Dred.jar
#
read -p "Password (Wages with punctuation): " pwd
jarsigner -keystore sufrin.keystore -verbose -storepass $pwd -keypass $pwd -signedjar Dred.jar BUILD/Dred.jar sufrin