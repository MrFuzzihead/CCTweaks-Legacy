import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream

// JUnit 5 is not detected by the Gradle test task unless the platform is requested explicitly.
// Without this the suite compiles but silently executes zero tests.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

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

// `useJUnitPlatform()` still succeeds when a test source set compiles to zero executables, so a
// refactor that moves every test out of a detected package would silently pass CI. Fail instead.
val verifyTestSuiteExecuted = tasks.register("verifyTestSuiteExecuted") {
    group = "verification"
    description = "Fails if the test task produced no JUnit XML results."
    dependsOn(tasks.named("test"))
    val results = layout.buildDirectory.dir("test-results/test")
    doLast {
        val dir = results.get().asFile
        val files = dir.listFiles { file -> file.name.endsWith(".xml") } ?: emptyArray()
        if (files.isEmpty()) {
            throw GradleException("No test results in $dir - the test suite did not run (check useJUnitPlatform and the test source set)")
        }
        logger.lifecycle("verifyTestSuiteExecuted: ${files.size} test result file(s)")
    }
}

tasks.named("check").configure { dependsOn(verifyTestSuiteExecuted) }

// Coverage: the `jacoco` plugin instruments the existing test task and registers
// `jacocoTestReport`. The report is attached to `check` so `./gradlew build` publishes
// build/reports/jacoco/test/html without a second test run.
apply(plugin = "jacoco")

tasks.withType<Test>().configureEach {
    extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isEnabled = true
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    // Generated mod metadata has no logic worth covering.
                    exclude("org/squiddev/cctweaks/Tags.class")
                    // The in-world self test is exercised by the dedicated-server harness
                    // (scripts/headless-smoke.sh -PselfTest), which runs in a second JVM that this
                    // report does not instrument. Excluded so the number stays a measure of the
                    // unit suite; the harness asserts its results on every run instead.
                    exclude("org/squiddev/cctweaks/selfTest/**")
                }
            }
        )
    )
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }
}

tasks.named("check").configure { dependsOn(tasks.named("jacocoTestReport")) }

// `-PselfTest` runs the in-world self test (org.squiddev.cctweaks.selfTest) as soon as the
// dedicated server has loaded its worlds. The test writes a few blocks into a scratch area near
// the spawn, checks digging/placement/attacks/upgrades against the real world and restores them.
// scripts/headless-smoke.sh enables it by default; pass SMOKE_SELFTEST=0 to skip it.
if (providers.gradleProperty("selfTest").isPresent) {
    tasks.matching { it.name == "runServer25" }.configureEach {
        if (this is JavaExec) {
            systemProperty("cctweaks.selftest", "true")
        }
    }
}

// `-PsmokeTest` makes the dedicated server stop itself shortly after it finishes loading, so a
// headless smoke run (scripts/headless-smoke.sh, CI) terminates on its own instead of needing an
// external kill. The dev server reads commands from stdin, but Gradle gives an Exec task a closed
// stdin, so we hand it a pipe and write "stop" into it once the log says the server is up.
if (providers.gradleProperty("smokeTest").isPresent) {
    // How long to wait for "Done (" before sending stop anyway. The console command is queued by
    // the dedicated server and executed on the next tick, so sending it early is safe: it takes
    // effect as soon as the server is actually running.
    val stopAfterSeconds = providers.gradleProperty("smokeStopAfter").orNull?.toLongOrNull() ?: 300L

    tasks.matching { it.name == "runServer25" }.configureEach {
        if (this is JavaExec) {
            doFirst {
                // The pipe is created here, not at configuration time, because the configuration
                // cache cannot serialise a PipedInputStream.
                val pipe = PipedOutputStream()
                standardInput = PipedInputStream(pipe, 1 shl 12)

                val logFile = File(workingDir, "logs/fml-server-latest.log")
                Thread {
                    val started = System.currentTimeMillis()
                    var up = false
                    while (!up && System.currentTimeMillis() - started < stopAfterSeconds * 1000L) {
                        // Reading the log while log4j holds it open can fail on Windows, so this is
                        // only an optimisation: if it never succeeds we still stop the server on the
                        // time limit above.
                        up = try {
                            logFile.isFile && logFile.length() > 0L && logFile.readText().contains("Done (")
                        } catch (e: Exception) {
                            false
                        }
                        if (!up) Thread.sleep(1000)
                    }
                    // Give mods a moment to run their post-load world hooks (version checks, custom
                    // save data) so the smoke assertions see their output too.
                    if (up) Thread.sleep(10_000)
                    runCatching {
                        pipe.write(("stop" + System.lineSeparator()).toByteArray())
                        pipe.flush()
                    }
                }.apply { isDaemon = true }.start()
            }
        }
    }
}
