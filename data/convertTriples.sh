#!/bin/bash

FILES=/home/roland/workspace/NDE/data/*.nt
for f in $FILES
do
  echo "Processing $f file..."
  # take action on each file. $f store current file name
  
  #_fileOut= "$(dirname "$f")/ttl/$(basename "$f" .nt).ttl"

  #echo "rapper -i ntriples $f -o turtle $_fileOut"
  rapper -i ntriples $f -o turtle > "$(dirname "$f")/ttl/$(basename "$f" .nt).ttl"
done

cp *.nt lodkb/
cd lodkb/
sed -i 's|https://data.netwerkdigitaalerfgoed|http://lod.kb|g' *.nt