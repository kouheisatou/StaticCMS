import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.spotless)
    kotlin("plugin.serialization") version "2.1.21"
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesSwing)

            // CSV reading
            implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")

            // HTTP client for GitHub API
            implementation("io.ktor:ktor-client-core:3.0.1")
            implementation("io.ktor:ktor-client-cio:3.0.1")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
            implementation("io.ktor:ktor-client-auth:3.0.1")

            // JGit for Git operations
            implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")

            // Local server for OAuth callback
            implementation("io.ktor:ktor-server-core:3.0.1")
            implementation("io.ktor:ktor-server-cio:3.0.1")
            implementation("io.ktor:ktor-server-html-builder:3.0.1")

            // File chooser dialog (JNA not needed for basic JFileChooser)
            // Using standard Java Swing components for file selection
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.kouheisatou.staticcms.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.kouheisatou.staticcms"
            packageVersion = "1.0.0"
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktfmt().dropboxStyle()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt().dropboxStyle()
    }
}

// Custom format task that runs spotless (primary formatter)
tasks.register("format") {
    dependsOn("spotlessApply")
    group = "formatting"
    description = "Formats code using Spotless (unified formatting)"
}

// Custom lint check task that prioritizes spotless
tasks.register("lintCheck") {
    dependsOn("spotlessCheck", "check")
    group = "verification"
    description = "Runs Spotless format check and build verification"
}
