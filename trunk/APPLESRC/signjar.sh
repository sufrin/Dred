#
# Make a signed Appledred.jar (-tsa stuff may not be necessary)
#
read -p "Password (Wages with punctuation): " pwd
jarsigner -keystore sufrin.keystore -tsa "http://timestamp.comodoca.com" -verbose -storepass $pwd -keypass $pwd -signedjar AppleDred.jar build/AppleDred.jar sufrin

