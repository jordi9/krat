import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
  id("com.vanniktech.maven.publish")
}

// Version from -Pversion (CI sets this from git tag) or default snapshot
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.0.0-SNAPSHOT"

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()

  configure(KotlinJvm(
    javadocJar = JavadocJar.Empty(),
    sourcesJar = true
  ))

  pom {
    name.set(project.name)
    description.set(project.description ?: project.name)
    url.set("https://github.com/jordi9/krat")

    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }

    developers {
      developer {
        id.set("jordi9")
        name.set("jordi9")
        url.set("https://github.com/jordi9")
      }
    }

    scm {
      url.set("https://github.com/jordi9/krat")
      connection.set("scm:git:git://github.com/jordi9/krat.git")
      developerConnection.set("scm:git:ssh://git@github.com/jordi9/krat.git")
    }
  }
}
