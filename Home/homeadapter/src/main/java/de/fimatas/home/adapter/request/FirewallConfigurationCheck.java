package de.fimatas.home.adapter.request;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;

@Component
@CommonsLog
/*
  Due to security vulnerabilities in dependent libraries,
  this application should only be operated WITHOUT the possibility of incoming and outgoing Internet connections.
  This component checks for active blocking of outgoing connections and interrupts startup if
  a connection can be established.

  Example iptables configuration:
  Application has to be started by a user with group 'noAccessToInternet'.
  The IP range for local network has to be adjusted.

  iptables -A OUTPUT -m owner --gid-owner noAccessToInternet -d 192.168.2.0/24 -j ACCEPT
  iptables -A OUTPUT -m owner --gid-owner noAccessToInternet -d 127.0.0.0/8 -j ACCEPT
  iptables -A OUTPUT -m owner --gid-owner noAccessToInternet -j DROP
  ip6tables -A OUTPUT -m owner --gid-owner noAccessToInternet -d fc80::/8 -j ACCEPT
  ip6tables -A OUTPUT -m owner --gid-owner noAccessToInternet -d fd80::/8 -j ACCEPT
  ip6tables -A OUTPUT -m owner --gid-owner noAccessToInternet -d fe80::/8 -j ACCEPT
  ip6tables -A OUTPUT -m owner --gid-owner noAccessToInternet -d ::1 -j ACCEPT
  ip6tables -A OUTPUT -m owner --gid-owner noAccessToInternet -j DROP
  iptables -A INPUT -d 192.168.2.0/24 -j ACCEPT
  iptables -A INPUT -d 127.0.0.0/8 -j ACCEPT
  iptables -A INPUT -j DROP
  ip6tables -A INPUT -d fc80::/8 -j ACCEPT
  ip6tables -A INPUT -d fd80::/8 -j ACCEPT
  ip6tables -A INPUT -d fe80::/8 -j ACCEPT
  ip6tables -A INPUT -d ::1 -j ACCEPT
  ip6tables -A INPUT -j DROP
 */
public class FirewallConfigurationCheck {

    @Autowired
    @Qualifier("restTemplateFirewallConfigurationCheck")
    private RestTemplate restTemplateLowTimeout;

    @Value("${application.firewallConfigurationCheckEnabled:true}")
    private boolean firewallConfigurationCheckEnabled;

    @PostConstruct
    public void init() throws ConfigurationException{
        if(firewallConfigurationCheckEnabled){
            try {
                restTemplateLowTimeout.getForEntity("https://fimatas.de", String.class);
                throw new ConfigurationException("Outgoing internet traffic is not blocked!");
            } catch(RuntimeException re){
                log.info("Firewall configutation check ok: Outgoing internet traffic is blocked.");
            }
        }
    }

}
