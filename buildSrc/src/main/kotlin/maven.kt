import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication

fun MavenPublication.configurePom(block: MavenPom.() -> Unit) {
    pom {
        url.set("https://github.com/anatawa12/mod-patching#readme")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("anatawa12")
                name.set("anatawa12")
                email.set("anatawa12@icloud.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/anatawa12/mod-patching.git")
            developerConnection.set("scm:git:ssh://github.com:anatawa12/mod-patching.git")
            url.set("https://github.com/anatawa12/mod-patching")
        }
        block()
    }
}
