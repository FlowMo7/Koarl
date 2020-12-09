# Koarl - Kotlin Open Source Android Crash Reporting Library

![API 10](https://img.shields.io/badge/API-10-yellow.svg)

An open source, self hosted error and crash reporting system for Android applications.


## Setup

### Android

Include the `koarl-android` dependency into your project.
The dependency is provided in a custom maven repository.

```gradle
allprojects {
    repositories {
        ...
        maven {
            url 'https://nexus.moetz.dev/repository/koarl/'
        }
    }
}

dependencies {
    implementation 'dev.moetz.koarl:koarl-android:0.0.9'
}
```

Initialize the library in you applications onCreate method:
```kotlin
override fun onCreate() {
    super.onCreate()

    Koarl.init(context = this) {
        baseUrl("https://koarl.dev/")   //<-- The URL to your backend instance
    }
}
```

After that all application crashes are automatically sent to the backend service.
Furthermore, by using `Koarl.logException()`, a non-fatal error can be logged.


### Server

The server application is provided as docker image / docker-compose configuration (which includes a MySQL database as well as an NGINX reverse proxy as well).

Copy the given [docker-compose.yml] file, replace the passwords with customly generated passwords, and start up the container by using:
```shell script
docker-compose up -d
```

This will provide the backend application as well as the dashboard, available at 127.0.0.1:80/dashboard.

# License

```
Copyright 2020 Florian Mötz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
