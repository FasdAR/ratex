apply plugin: 'java-library'
apply plugin: 'kotlin'


dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"

    testImplementation "junit:junit:${rootProject.ext.ver.jUnit}"
    testImplementation "org.mockito:mockito-core:${rootProject.ext.ver.mockitoCore}"
    testImplementation "org.assertj:assertj-core:${rootProject.ext.ver.assertJ}"

    //RxJava
    implementation "io.reactivex.rxjava2:rxkotlin:${rootProject.ext.ver.rxKotlin}"
}

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}