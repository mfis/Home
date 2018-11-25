# Home

Home automation controlling web application.

Client: Commented application.properties needed to be configured in external 'homeapp.properties'.
Controller: Commented application.properties needed to be configured in external 'homecontroller.properties'.

Sources in /domain/ -packages are depending on individual Homematic setup and therefore needed to be changed.

Application is based on [Spring Boot](https://spring.io/projects/spring-boot) and uses [Thymeleaf](https://www.thymeleaf.org) for GUI rendering and [Font Awesome](https://fontawesome.com) for icons (all not included).

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
