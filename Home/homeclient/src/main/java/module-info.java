module homeclient {
	exports de.fimatas.home.client.request;
	exports de.fimatas.home.client.model;
	exports de.fimatas.home.client.domain.model;
	exports de.fimatas.home.client.domain.service;
	exports de.fimatas.home.client.service;
	exports de.fimatas.home.client;

	requires transitive homelibrary;
	requires java.annotation;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.tomcat.embed.core;
	requires spring.beans;
	requires spring.boot;
	requires spring.boot.autoconfigure;
	requires transitive spring.context;
	requires spring.core;
	requires spring.jcl;
	requires spring.web;
	requires spring.webmvc;
}