# General
This is a Home automation controlling web application.

Application is based on [Spring Boot](https://spring.io/projects/spring-boot) and uses [Thymeleaf](https://www.thymeleaf.org) for GUI rendering and [Font Awesome](https://fontawesome.com) for icons (all not included).

# Customizing
Sources in */domain/ -packages are depending on individual Homematic setup and therefore needed to be changed.

# Installation
Build Home.jar and HomeController.jar with Maven.  
Install both spring boot applications as described here: [Spring Documentation - Installing Spring Boot Applications] (https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)  

Client (Home.jar): Commented application.properties needed to be configured in external 'homeapp.properties'.  
Controller (HomeController.jar): Commented application.properties needed to be configured in external 'homecontroller.properties'.

# Secure communication to CCU
* CCU USL (property "homematic.hostName") has to be a https URL
* Generate SSL certificate for the CCU
```
    openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem -subj "/CN=<CCU_HOSTNAME>/OU=<CCU_SERIAL_NO>/C=DE/emailAddress=<MAIL_ADRESS>"
    cat key.pem > hmccu3.pem
    cat cert.pem >> hmccu3.pem
```    
* Upload combined certificate/key to the CCU via the settings page
* Set reference to certificate in property "homematic.sslcert"
* User authentication sould be enabled in CCU settings and referenced in properties "homematic.authuser" and "homematic.authpass"

# Naming conventions inside CCU
Some functions are needing CCU system variables and CCU programs with names corresponding to the devices.
Device name means type and placeName in Device.java, recommended the name of the device in CCU too.
In contrast to CCU device names and names in Device.java, CCU system variables are needed to be literal-escaped from german umlauts and removed from spaces.

## General
CCU System variables:
* refreshadress - set manually with the adress of the update method within the controller application, e.g. 'http://localhost:8098/controller/refresh', 
used by CCU programs to trigger value refreh in the controller application. Sample CCU Script:
```
  string adress = dom.GetObject("refreshadress").State();
  system.Exec("wget '" + adress + "?notify=false' --no-check-certificate -q -O /dev/null", &stdout, &stderr);
```
Set parameter 'notify=true' when the ccu program was called by home application instead of (CCU-sided) timer- or event triggered program, because of async start from ccu programs over Homematic XML-API.

## Switches
CCU System variables:
* [devicename]Automatic - Boolean - updated by Home application to control automatic/manual mode, used by CCU programms (see below).
* [devicename]AutomaticInfoText - String - used by Home application to show info text about program control.
CCU Programs:
* Any amount and name, but have to evaluate [devicename]Automatic system variable before controlling the device

## Thermostates
CCU System variables:
* [devicename]Temperature - Number - updated by Home application, used by CCU program [devicename]Manual
CCU Programs:
* [devicename]Boost - starts boost mode
* [devicename]Manual - starts manual mode with destination temperature from system variable [devicename]Temperature

# Recommended control principles:
Simple programs (based on clocks, timers etc) are written in the CCU to ensure function even if the Home application is not running or accessible.  

More complex programs are splitted. The trigger of the actor is implemented over an CCU program or Homematic XML-API call. The activator is implemented in Home application e.g. based on a hint.  

Example: A roller shutter motor has CCU programs to automatically drive up and down based on sunrise and sunset times. An additional CCU program drives the shutter down if a CCU system variable (which is linked to the ‚close roller shutter‘ hint based on temperature and sunshine) is set to true.
