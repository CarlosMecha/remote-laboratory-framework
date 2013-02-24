#!/bin/bash

cd /home/rodriguezmecha/Escritorio/PFC/rlf/doc;
killall evince;
cd include;
rm -rf *.aux *.log *.out *.idx *.bbl *.blg *.ilg *.ind *.lof *.lot *.toc;
cd ..;

rm main.pdf;
pdflatex main.tex;
bibtex main.aux;
makeindex main.idx;
pdflatex main.tex;
pdflatex main.tex;

rm -rf *.aux *.log *.out *.idx *.bbl *.blg *.ilg *.ind *.lof *.lot *.toc;
cd include;
rm -rf *.aux *.log *.out *.idx *.bbl *.blg *.ilg *.ind *.lof *.lot *.toc;
cd ..;

evince main.pdf;
