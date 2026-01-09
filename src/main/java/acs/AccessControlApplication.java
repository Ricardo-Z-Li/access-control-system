package acs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class AccessControlApplication {
    public static void main(String[] args) {
        // 设置AWT headless模式为false，允许Swing UI运行
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(AccessControlApplication.class, args);
    }
}