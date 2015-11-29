<?php

$gpio_channel = 7;

$setmode_channel = shell_exec("/usr/local/bin/gpio -g mode ".$gpio_channel." out");

$gpio_off = shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." 1");

echo "Done!";

?>
