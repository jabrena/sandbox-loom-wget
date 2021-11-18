plugins {
    `java-library`
}

group = "br.dev.pedrolamarao"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.test {
    useJUnitPlatform()
}