package com.example.yakallim

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
class YakallimApplication

fun main(args: Array<String>) {
	runApplication<YakallimApplication>(*args)
}
