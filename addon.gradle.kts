// ComputerCraft 3.2.0 declares `required-after:lwjgl3ify` in its mcmod.info, and lwjgl3ify needs a
// modern JVM. GTNHGradle's plain runServer/runClient use the Java 8 toolchain and carry no
// lwjgl3ify on the classpath, so they die at MissingModsException before the game starts. Forward
// them to the modern variants (which GTNHGradle wires to lwjgl3ify and a Java 17+/21/25 JVM), so
// both `./gradlew runServer` and CI's server smoke test keep working.
//
// This file is applied before the convention plugin, so the run tasks do not exist yet: configure
// them lazily and resolve the modern task by name when they are realised.
mapOf("runServer" to "runServer25", "runClient" to "runClient25").forEach { (plain, modern) ->
    tasks.matching { it.name == plain }.configureEach {
        if (project.tasks.names.contains(modern)) {
            description = "Alias for $modern - ComputerCraft 3.2.0 requires lwjgl3ify, which needs Java 17+"
            dependsOn(modern)
            onlyIf("the plain task forwards to $modern") { false }
        }
    }
}
