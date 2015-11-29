<?php

date_default_timezone_set('America/Los_Angeles');

$testmode = 0;

# number of GPIO pin on Raspberry Pi
$gpio_channel = 7;

# Hour and minute when our 'morning' starts and thermostat should be ON
$daytime_start_hours = 8;
$daytime_start_minutes = 0;

# Duration of 'daytime' period in hours and minutes
$daytime_period_hours = 17;
$daytime_period_minutes = 0;

$currentTime = new DateTime('NOW');
$mydate_begin = new DateTime('NOW');
$mydate_begin->setTime($daytime_start_hours, $daytime_start_minutes);
$mydate_end = new DateTime('NOW');
$mydate_end->setTime($daytime_start_hours, $daytime_start_minutes);
$mydate_end->add(new DateInterval('PT'.($daytime_period_hours * 60 + $daytime_period_minutes).'M'));

echo "DateTime (begin): ".$mydate_begin->format('Y-m-d H:i:s')."\n";
echo "DateTime (end)  : ".$mydate_end->format('Y-m-d H:i:s')."\n";
echo "Current time    : ".$currentTime->format('Y-m-d H:i:s')."\n";

if ($testmode == 0) {
	$setmode_channel = shell_exec("/usr/local/bin/gpio -g mode ".$gpio_channel." out");
}
	
if (($currentTime > $mydate_begin) && ($currentTime < $mydate_end)) {
    echo "Thermostat should be ON\n";
    if ($testmode == 0) {
    	$gpio_on = shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." 0"); 
    }
} else {
    echo "Should be OFF\n";
    if ($testmode == 0) {
        $gpio_off = shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." 1");
    }
}

?>