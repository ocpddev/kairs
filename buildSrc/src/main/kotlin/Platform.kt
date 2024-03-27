/**
 * A utility to determine the current platform to help load the correct native library.
 *
 * Currently, this only supports x86_64 and aarch64 architectures on Windows, Linux, and macOS,
 * which should be sufficient for our own use cases.
 *
 * The judge logic is borrowed from `com.sun.jna.Platform`
 */
class Platform(private val arch: Arch, private val os: OS) {

    enum class Arch(val canonicalName: String) {
        X86_64("x86_64"),
        AARCH64("aarch64"),
        ;

        companion object {
            fun current() = when (val arch = System.getProperty("os.arch")) {
                "amd64", "x86_64" -> X86_64
                "aarch64" -> AARCH64
                else -> throw UnsupportedOperationException("Unsupported architecture: $arch")
            }
        }
    }

    enum class OS(val canonicalName: String) {
        Windows("windows"),
        Linux("linux"),
        MacOS("macos"),
        ;

        companion object {
            fun current(): OS {
                val os = System.getProperty("os.name")
                return when {
                    os.startsWith("Windows") -> Windows
                    os.startsWith("Linux") -> Linux
                    os.startsWith("Mac") || os.startsWith("Darwin") -> MacOS
                    else -> throw UnsupportedOperationException("Unsupported OS: $os")
                }
            }
        }
    }

    companion object {
        fun current() = Platform(Arch.current(), OS.current())
    }

    fun identifier() = "${arch.canonicalName}-${os.canonicalName}"

    override fun toString() = identifier()
}
