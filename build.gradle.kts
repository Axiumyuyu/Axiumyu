plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}

val withTest = project.hasProperty("addTest")

sourceSets {
    main {
        kotlin {
            // 如果没有传入 -PaddTest，则排除测试相关代码
            if (!withTest) {
                exclude("**/test/**")
                println(">>> 编译模式：生产模式 (已排除测试代码)")
            } else {
                println(">>> 编译模式：测试模式 (包含测试代码)")
            }
        }
    }
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    if (withTest) {
        // 只有开启了测试模式时，才添加 -test 后缀
        archiveClassifier.set("test")
    } else {
        // 生产模式下，文件名不带任何 classifier
        archiveClassifier.set("")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"]) // 将标准的 Java 产物（JAR）和依赖关系包含进 POM

            // 【注意】如果你使用了 shadowJar 插件来打入 Kotlin 运行时依赖，
            // 请将上面的 from(components["java"]) 替换为下面这行：
            // artifact(tasks.named("shadowJar"))
        }
    }
}
