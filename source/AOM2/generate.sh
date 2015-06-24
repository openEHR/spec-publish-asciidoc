#!/usr/bin/bash
asciidoctor -a localyear=`date +%G` -a stylesdir=../../css -a stylesheet=openehr.css AOM2.adoc

# In theory, Use the following as a production command; note that it doesn't copy over all the needed images. Needs an extension to do this
#asciidoctor -a localyear=`date +%G` -a stylesdir=../css -a stylesheet=openehr.css --out-file ../../publishing/HTML/AOM2.html AOM2.adoc

# use the following to experiment with CSS changes; requires the Git repo http://github.com/asciidoctor/asciidoctor-stylesheet-factory.git to 
# be cloned as a sibling of the openEHR specifications-XX Git repos
#asciidoctor -a localyear=`date +%G` -a stylesdir=../../../asciidoctor-stylesheet-factory/stylesheets -a stylesheet=openehr.css AOM2.adoc
#asciidoctor -a localyear=`date +%G` -a stylesdir=../../../asciidoctor-stylesheet-factory/stylesheets AOM2.adoc
