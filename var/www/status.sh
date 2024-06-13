#!/bin/bash
memFree=$(cat /proc/meminfo | grep MemFree | awk '{ print $2 }')
memTotal=$(cat /proc/meminfo | grep MemTotal | awk '{ print $2 }')
memPourcent=$(bc <<< "scale=4; $memFree/$memTotal")
diskFree=$(df / | awk '/[0-9]%/{print $(NF-2)}')
diskTotal=$(df / | awk '/[0-9]%/{print $(NF-4)}')
diskPourcent=$(bc <<< "scale=4; $diskFree/$diskTotal")

# renvoi dans la page html
cat > "./var/www/status.html" << EOF
<!DOCTYPE html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Status</title>
</head>
<html>
  <body>
    <h1>Mémoire RAM disponible : </h1>
    <ul>
      <li><h2>$memFree ko</h2></li>
      <li><h2>${memPourcent:1:2},${memPourcent:3:2} %</h2></li>
    </ul>
    <h1>Espace disque disponible : </h1>
    <ul>
      <li><h2>$diskFree ko</h2></li>
      <li><h2>${diskPourcent:1:2},${diskPourcent:3:2} %</h2></li>
    </ul>
    <h1>Nombre de processus : </h1>
      <ul>
        <li><h2>$(ps -e --no-heading | wc -l)</h2></li>
      </ul>
  </body>
</html>
EOF