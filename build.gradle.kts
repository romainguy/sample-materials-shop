plugins {
    id("com.android.application") version "4.2.0-alpha12" apply false
    kotlin("android") version "1.4.10" apply false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}
