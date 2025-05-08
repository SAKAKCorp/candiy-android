plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("maven-publish")
}

android {
    namespace = "com.candiy.candiyhc"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
    packagingOptions {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/io.netty.versions.properties")


    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.firebase.appdistribution.gradle)
    implementation(libs.androidx.media3.common.ktx)
    val work_version = "2.9.1"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.appcompat.v170)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.connect.client)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // 비동기 실행
    implementation(libs.kotlinx.coroutines.android)
    // 로그 확인용
    implementation(libs.timber)

    // 헬스커넥트 API
    implementation("androidx.health.connect:connect-client:1.1.0-beta01")
    // Retrofit (서버 통신용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")  // Retrofit과 통합하기 위한 Moshi 컨버터

    // room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-testing:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // android debug db viewer
    debugImplementation("com.github.amitshekhariitbhu.Android-Debug-Database:debug-db:1.0.7")
    debugImplementation("com.github.amitshekhariitbhu.Android-Debug-Database:debug-db-encrypt:1.0.7")

    // moshi
    implementation("com.squareup.moshi:moshi:1.13.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")

    // workManager
    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")
}
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.SAKAKCorp"
                artifactId = "candiy-android"
                version = "0.1.0"

                pom {
                    name.set("candiyHC")
                    description.set("This is an SDK that helps you easily integrate and manage Android Health Connect data in your application.")
                }
            }
        }
    }
}