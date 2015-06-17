#!/bin/bash
asciidoctor -a localyear=`date +%G` --out-file ../../publishing/HTML/AOM2.html AOM2.adoc
