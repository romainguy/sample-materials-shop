plugins {
    id("com.android.application") version "7.0.4" apply false
    kotlin("android") version "1.7.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
