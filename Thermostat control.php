date_default_timezone_set('America/Los_Angeles');


$currentTime = new DateTime('NOW');

$mydate_1 = new DateTime('NOW');;
$mydate_1->setTime(8, 00);
$mydate_2 = new DateTime('NOW');;
$mydate_2->add(new DateInterval('PT17H'));


echo "DateTime 1: ".$mydate_1->format('Y-m-d H:i:s');
echo "DateTime 2: ".$mydate_2->format('Y-m-d H:i:s');
echo "Current time: ".$currentTime->format('Y-m-d H:i:s');

if (($currentTime > $mydate_1) && ($currentTime < $mydate_2)) 
{
	echo "Should be On";
} else {
    echo "Should be Off";
}