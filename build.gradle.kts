plugins {
    id("com.android.application") version "4.2.0-alpha08" apply false
    kotlin("android") version "1.4.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}
