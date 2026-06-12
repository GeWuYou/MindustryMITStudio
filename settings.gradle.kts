pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内 Maven Central 镜像
        maven {
            name = "AliyunPublic"
            url = uri("https://maven.aliyun.com/repository/public")
        }

        maven {
            name = "AliyunCentral"
            url = uri("https://maven.aliyun.com/repository/central")
        }

        maven {
            name = "TencentCloud"
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MindustryMITStudio"

include(":server")
