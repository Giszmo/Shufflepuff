#!/bin/bash

# Argument must be given. 

if [ -z "$1" ]
then
  echo "No argument supplied"
  exit 1
fi

N=$1

if [[ ! $N =~ ^[0-9]{1,2}$ ]] 
then
  echo "Integer argument >= 2 required"
  exit 1
fi

# player index
player=1

# output file name
out="shuffle-output"

# error file name
err="shuffle-error"

# delete all output files that might have been leftover. 
while [ $player -le $N ] ;
do
  if [ -e $out$player.txt ] 
  then rm $out$player.txt
  fi
  if [ -e $err$player.txt ]
  then rm $err$player.txt
  fi
  player=$((player+1))
done

if [ -e shuffler/build/distributions/shuffler ]
then rm -r shuffler/build/distributions/shuffler
fi

# run gradle task to create zip file. 
bash gradlew distZip

# Extract contents of generated file. 
unzip shuffler/build/distributions/shuffler.zip -d shuffler/build/distributions

# run the protocol. 

echo "Running shuffle tcp test for $N players."

minPort=1808

player=1

while [ $player -le $N ] ;
do
  echo "running player $player with port $((minPort + player))"
  bash ./shuffler/build/distributions/shuffler/bin/shuffler -players $N -identity $player -amount 17 -minport $minPort -threads 4 > $out$player.txt 2> $err$player &
  player=$((player+1))
done

wait

echo "Protocol complete."
