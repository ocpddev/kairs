plugins {
    base
}

private val cargoProfile = run {
    val property = providers.gradleProperty("cargo.profile").getOrElse("debug")
    when (property) {
        CargoProfile.Debug.value -> CargoProfile.Debug
        CargoProfile.Release.value -> CargoProfile.Release
        else -> error("Invalid cargo.profile: $name. Must be 'debug' or 'release'")
    }
}

enum class CargoProfile(val value: String) {
    Debug("debug"),
    Release("release");
}

val cargoClean = task<Exec>("cargoClean") {
    group = "build"
    commandLine = listOf("cargo", "clean")
}

tasks.clean {
    dependsOn(cargoClean)
}

val cargoBuild = task<Exec>("cargoBuild") {
    group = "build"
    commandLine = listOf("cargo", "build")
    when (cargoProfile) {
        CargoProfile.Debug -> {}
        CargoProfile.Release -> {
            args("--release")
        }
    }
    val mappedLibName = System.mapLibraryName(rootProject.name)
    outputs.file("target/${cargoProfile.value}/$mappedLibName")
}

val collectArtifacts = task<Copy>("collectArtifacts") {
    group = "build"
    dependsOn(cargoBuild)
    from(cargoBuild.outputs)
    into(base.libsDirectory.dir(Platform.current().identifier()))
}

tasks.assemble {
    dependsOn(collectArtifacts)
}
