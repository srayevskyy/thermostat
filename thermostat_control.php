<?php

date_default_timezone_set('America/Los_Angeles');

$gpio_channel = 7;

$currentTime = new DateTime('NOW');
$mydate_begin = new DateTime('NOW');;
$mydate_begin->setTime(8, 00);
$mydate_end = new DateTime('NOW');;
$mydate_end->add(new DateInterval('PT17H'));

echo "DateTime (begin): ".$mydate_begin->format('Y-m-d H:i:s')."\n";
echo "DateTime (end)  : ".$mydate_end->format('Y-m-d H:i:s')."\n";
echo "Current time    : ".$currentTime->format('Y-m-d H:i:s')."\n";

$setmode_channel = shell_exec("/usr/local/bin/gpio -g mode ".$gpio_channel." out");

if (($currentTime > $mydate_begin) && ($currentTime < $mydate_end)) {
    echo "Thermostat should be ON\n";
    $gpio_on = shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." 0");
} else {
    echo "Should be OFF\n";
    $gpio_off = shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." 1");
}

?>