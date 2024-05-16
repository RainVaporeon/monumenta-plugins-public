rootProject.name = "monumenta-plugins"
include(":adapter_api")
include(":adapter_unsupported")
include(":adapter_v1_19_R3")
include(":Monumenta")
project(":Monumenta").projectDir = file("paper")

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://repo.papermc.io/repository/maven-public/")
		maven("https://maven.floweytf.com/releases")
	}
}
