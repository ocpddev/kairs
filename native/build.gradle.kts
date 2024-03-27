task<Exec>("build") {
    group = "build"
    commandLine = listOf("cargo", "build")
    when (project.cargo().profile()) {
        CargoProfile.Debug -> {}
        CargoProfile.Release -> {
            args("--release")
        }
    }
}

task<Exec>("clean") {
    group = "build"
    commandLine = listOf("cargo", "clean")
}
