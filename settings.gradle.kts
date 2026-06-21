pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://jitpack.io") }
        google()
//        mavenLocal()
        maven {
            url = uri("https://androidsdk.insta360.com/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "insta360guest"
                password = "EXMSjSo8OeOrjU7d"
            }
        }
    }
}

rootProject.name = "sdkdemo2"
include(":app")
include(":wear")
