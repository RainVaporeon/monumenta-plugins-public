import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.github.johnrengelman.shadow") version "7.1.2"
	id("com.playmonumenta.plugins.java-conventions")
	id("com.playmonumenta.deployment") version "1.0"
	id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
	id("net.minecrell.plugin-yml.bungee") version "0.5.1" // Generates bungee.yml
	id("java")
	id("net.ltgt.errorprone") version "3.1.0"
	id("net.ltgt.nullaway") version "1.6.0"
}

repositories {
	mavenCentral()
	maven("https://libraries.minecraft.net/")
}

dependencies {
	// NOTE - Make sure if you add another version here you make sure to exclude it from minimization below!
	implementation(project(":adapter_api"))
	implementation(project(":adapter_unsupported"))
	implementation(project(":adapter_v1_19_R3", "reobf"))

	implementation("org.openjdk.jmh:jmh-core:1.19")
	implementation("org.openjdk.jmh:jmh-generator-annprocess:1.19")
	implementation("com.opencsv:opencsv:5.5") // generateitems
	implementation("net.kyori:adventure-text-serializer-bungeecord:4.3.2")

	// Note this version should match what's in the Paper jar
	compileOnly("net.kyori:adventure-api:4.11.0")
	compileOnly("net.kyori:adventure-text-minimessage:4.11.0")

	compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
	compileOnly("dev.jorel.CommandAPI:commandapi-core:8.7.0")
	compileOnly("me.clip:placeholderapi:2.10.9")
	compileOnly("de.jeff_media:ChestSortAPI:12.0.0")
	compileOnly("net.luckperms:api:5.4")
	compileOnly("net.coreprotect:coreprotect:2.15.0") {
		exclude(group = "org.bukkit")
	}
	compileOnly("com.playmonumenta:scripted-quests:7.0")
	compileOnly("com.playmonumenta:redissync:4.1")
	compileOnly("com.playmonumenta:monumenta-network-chat:2.7.4")
	compileOnly("com.playmonumenta:monumenta-network-relay:1.1")
	compileOnly("com.playmonumenta:structures:10.0")
	compileOnly("com.playmonumenta:worlds:2.0")
	compileOnly("com.playmonumenta:libraryofsouls:4.2")
	compileOnly("com.bergerkiller.bukkit:BKCommonLib:1.19.4-v2")
	compileOnly("com.mojang:brigadier:1.0.18")
	compileOnly("com.goncalomb.bukkit:nbteditor:3.2")
	compileOnly("de.tr7zw:item-nbt-api-plugin:2.12.0-SNAPSHOT")
	compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
	compileOnly("io.prometheus:simpleclient:0.11.0")
	compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.6.3")
	errorprone("com.google.errorprone:error_prone_core:2.23.0")
	errorprone("com.uber.nullaway:nullaway:0.10.18")

	// Bungeecord deps
	compileOnly("net.md-5:bungeecord-api:1.12-SNAPSHOT")
	compileOnly("com.google.code.gson:gson:2.8.5")
	compileOnly("com.playmonumenta:monumenta-network-relay:1.0")
	compileOnly("com.vexsoftware:nuvotifier-universal:2.7.2")
}

group = "com.playmonumenta"
description = "Monumenta Main Plugin"
version = rootProject.version

// Configure plugin.yml generation
bukkit {
	load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
	main = "com.playmonumenta.plugins.Plugin"
	apiVersion = "1.19"
	name = "Monumenta"
	authors = listOf("The Monumenta Team")
	depend = listOf("CommandAPI", "ScriptedQuests", "NBTAPI")
	softDepend = listOf(
		"MonumentaRedisSync",
		"PlaceholderAPI",
		"ChestSort",
		"LuckPerms",
		"CoreProtect",
		"NBTEditor",
		"LibraryOfSouls",
		"BKCommonLib",
		"MonumentaNetworkChat",
		"MonumentaNetworkRelay",
		"PremiumVanish",
		"ProtocolLib",
		"PrometheusExporter",
		"MonumentaStructureManagement",
		"MonumentaWorldManagement"
	)
}

// Configure bungee.yml generation
bungee {
	name = "Monumenta-Bungee"
	main = "com.playmonumenta.bungeecord.Main"
	author = "The Monumenta Team"
	softDepends = setOf("MonumentaNetworkRelay", "MonumentaRedisSync", "Votifier", "SuperVanish", "PremiumVanish", "BungeeTabListPlus", "LuckPerms")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.add("-Xmaxwarns")
	options.compilerArgs.add("10000")

	options.compilerArgs.add("-Xlint:deprecation")

	options.errorprone {
		option("NullAway:AnnotatedPackages", "com.playmonumenta")

		allErrorsAsWarnings.set(true)

		/*** Disabled checks ***/
		// These we almost certainly don't want
		check("InlineMeSuggester", CheckSeverity.OFF) // We won't keep deprecated stuff around long enough for this to matter
		check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
		check("FutureReturnValueIgnored", CheckSeverity.OFF) // This one is dumb and doesn't let you check return values with .whenComplete()
		check("ImmutableEnumChecker", CheckSeverity.OFF) // Would like to turn this on but we'd have to annotate a bunch of base classes
		check("LockNotBeforeTry", CheckSeverity.OFF) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
		check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
		check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
		check("MutablePublicArray", CheckSeverity.OFF) // These are bad practice but annoying to refactor and low risk of actual bugs
	}
}

// Relocation / shading
tasks {
	shadowJar {
		relocate("com.opencsv", "com.playmonumenta.plugins.internal.com.opencsv") // /generateitems
		relocate("org.openjdk.jmh", "com.playmonumenta.plugins.internal.org.openjdk.jmh") // Benchmarking Sin/Cos
		relocate("joptsimple", "com.playmonumenta.plugins.internal.joptsimple") // Dependency of jmh
		relocate(
			"org.apache.commons.lang3",
			"com.playmonumenta.plugins.internal.org.apache.commons.lang3"
		) // Dependency of several things
		relocate(
			"org.apache.commons.math3",
			"com.playmonumenta.plugins.internal.org.apache.commons.math3"
		) // Dependency of several things
		minimize {
			exclude(project(":adapter_api"))
			exclude(project(":adapter_unsupported"))
			exclude(project(":adapter_v1_19_R3"))
		}
	}
}

ssh.easySetup(tasks.named<ShadowJar>("shadowJar").get(), "Monumenta")
