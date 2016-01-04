<?php

date_default_timezone_set('America/Los_Angeles');

$testmode = 1;

# number of GPIO pin on Raspberry Pi
$gpio_channel = 8;

# Time (Hour and minute) when thermostat circuit should be ON
$daytime_start_hours = 8;
$daytime_start_minutes = 0;

# Duration of 'daytime' period in hours and minutes, when a thermostat circuit should be ON
$daytime_duration_hours = 17;
$daytime_duration_minutes = 0;

$currentTime = new DateTime('NOW');
$mydate_begin = new DateTime('NOW');
$mydate_begin->setTime($daytime_start_hours, $daytime_start_minutes);
$mydate_end = new DateTime('NOW');
$mydate_end->setTime($daytime_start_hours, $daytime_start_minutes);
$mydate_end->add(new DateInterval('PT'.($daytime_duration_hours * 60 + $daytime_duration_minutes).'M'));

echo "DateTime (begin): ".$mydate_begin->format('Y-m-d H:i:s')."\n";
echo "DateTime (end)  : ".$mydate_end->format('Y-m-d H:i:s')."\n";
echo "Current time    : ".$currentTime->format('Y-m-d H:i:s')."\n";

$decision = "off";

if (($currentTime > $mydate_begin) && ($currentTime < $mydate_end)) {
    $decision = "on";
}
elseif (($currentTime < $mydate_begin) && ($currentTime->format('H:i') < $mydate_end->format('H:i'))) {
    $decision = "on";
}

# debugging output
echo "Thermostat should be ".$decision.".\n";

# actual communication to switching relay
if ($testmode == 0) {
    $setmode_channel = shell_exec("/usr/local/bin/gpio -g mode ".$gpio_channel." out");
    shell_exec("/usr/local/bin/gpio -g write ".$gpio_channel." ".($decision=="on" ? "1" : "0"));
}

?>