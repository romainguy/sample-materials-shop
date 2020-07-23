plugins {
    id("com.android.application") version "4.2.0-alpha05" apply false
    kotlin("android") version "1.4-M3" apply false
}

allprojects {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/")
        jcenter()
        google()
    }
}
