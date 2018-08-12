Dred
====

Dred is a lightweight, modeless Unicode text editor built along the lines explored in my 1982 paper: "Formal Specification of a Display-Oriented Text Editor" (Science of Computer Programming). It is customizeable, and extensible and comes complete with documentation in a single executable jar (~256K). 

It is packaged with several extensions, including simple tools to support
the input of html/xml and Latex; and to manage the invocation of 
programs such as <tt>latex, make, subversion, cvs, rcs,
ant</tt>, and <tt>bash</tt>.  It provides a particularly easy method of
keyboarding exotic (!) Unicode symbols using straightforward mnemonics.

Additional tools can easily be added by a modestly-competent
Java programmer (Note added in 2012: The extension API was designed <i>ad-hoc</i> and almost
certainly requires more understanding on the part of an extension writer than the
documentation provides).

Dred was written in Java 1.5 between early February and mid-April 2005 because at the time I needed a run-anywhere lightweight and reliable editor that I could use to edit UTF8-encoded Jape theories. I had tried a number of public domain editors written in Java, but found that they made editing mathematical texts quite tedious, had very long startup times, or were insufficiently customizeable for me.

I've been using Dred on various x86 Linux distros, on Solaris, on OS/X (from Tiger to High Sierra), and on Windows (from NT through 7 to 10) for all my editing since mid-May (2005), and have found it completely reliable. Although it has no universal UNDO/REDO provision, there is user-configurable cut-ring, and every keystroke that results in big changes to the document being editing has a corresponding keystroke to undo its effect. 

Remark (May 2017): I recently implemented pdfsync compatibility, and did some maintenance enabling the editor to run on OS/X with Java 1.7, 1.8, 1.9, and to use recent variants of subversion. In doing so I corrected a couple of long-unnnoticed bugs.

See HTML documentation in trunk/DOC.
