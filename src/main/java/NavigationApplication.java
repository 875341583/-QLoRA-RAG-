package com.ct.navigation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 导航系统主启动类
 * 
 * <p>作为Spring Boot应用的入口点，负责加载配置、初始化Bean、启动内嵌服务器</p>
 * <p>采用微服务架构，集成多模态输入处理、QLoRA模型管理、RAG知识库管理等核心功能模块</p>
 * 
 * @author Alex
 * @version 1.0
 * @since 2024
 */
@SpringBootApplication
@EntityScan("com.ct.navigation.domain.entity")
@EnableJpaRepositories("com.ct.navigation.domain.repository")
@EnableAsync
@EnableScheduling
public class NavigationApplication {

    /**
     * 应用主入口方法
     * 
     * <p>启动Spring Boot应用，初始化所有配置的Bean和组件</p>
     * <p>自动扫描并加载application.yml和bootstrap.yml中的配置信息</p>
     * 
     * @param args 命令行参数，可用于覆盖默认配置
     */
    public static void main(String[] args) {
        SpringApplication.run(NavigationApplication.class, args);
    }
}


// 内容由AI生成，仅供参考