import org.gradle.api.Project
import java.io.File

/**
 * Get the Cargo extension for the project
 */
fun Project.cargo() = Cargo(this)

@JvmInline
value class Cargo(private val project: Project) {

    /**
     * Get the Cargo profile from the `cargo.profile` project property.
     * Defaults to 'debug' if not set
     */
    fun profileName() = project.findProperty("cargo.profile") as? String ?: "debug"

    fun profile() = CargoProfile.parse(profileName())

    fun libOutputPath(libName: String): File {
        val profile = profile()
        val mappedLibName = System.mapLibraryName(libName)
        return project.file(
            "target/${profile.profileName}/$mappedLibName"
        )
    }
}

enum class CargoProfile(val profileName: String) {
    Debug("debug"),
    Release("release");

    companion object {
        fun parse(name: String): CargoProfile {
            return when (name) {
                Debug.profileName -> Debug
                Release.profileName -> Release
                else -> error("Invalid cargo.profile: $name. Must be 'debug' or 'release'")
            }
        }
    }
}
