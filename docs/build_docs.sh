#/bin/sh

pandoc --from gfm *.md -o docs.pdf
pandoc --from gfm *.md -o docs.docx
