plugins {
    application
}

dependencies {
    implementation(project(":"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

application {
    mainClass.set("com.ytdlp.cli.CliMain")
}
