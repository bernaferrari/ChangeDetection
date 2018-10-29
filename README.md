<p align="center"><img src="logo/1024px.png" alt="ChangeDetection" height="200px"></p>

Change Detection
===================================

This app tracks changes on websites you otherwise would visit frequently to see if there is something new.
Use cases:
* Teacher says grades will be published "soon", but no one knows what "soon" means and you are tired of reloading.
* You are working with a server and wants to know the result from a request, periodically.
* You are waiting for updates on an Exam, like if something was postponed or updated.
* You want to monitor the Dagger documentation to see when Thermosiphon's explanation improves.

This app also showcases **all** the Android Architecture Components working together: [Room](https://developer.android.com/topic/libraries/architecture/room.html), [ViewModels](https://developer.android.com/reference/android/arch/lifecycle/ViewModel.html), [LiveData](https://developer.android.com/reference/android/arch/lifecycle/LiveData.html), [Paging](https://developer.android.com/topic/libraries/architecture/paging/), [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) and [Navigation](https://developer.android.com/topic/libraries/architecture/navigation/).

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Download from Google Play"
      height="80">](https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection)

 ![GIF](/.github/assets/card_gif.gif?raw=true)

## Screenshots

| Main Screen | Text Diff | PDF Diff | Settings |
|:-:|:-:|:-:|:-:|
| ![First](/.github/assets/main_screen.jpg?raw=true) | ![Sec](/.github/assets/diff_view_html.jpg?raw=true) | ![Third](/.github/assets/diff_view_pdf.jpg?raw=true) | ![Fourth](/.github/assets/settings.jpg?raw=true) |

Introduction
------------

### Features

This app contains the following screens:
* A list of websites that are currently being tracked.
* A text details view, that allows the user to compare the current website version with previous versions.
* An image details view, that allows the user to compare images in a carousel.
* A pdf details view, that allows the user to compare pdfs in a carousel, similar to the images.
* A settings view, that allows user to toggle auto-sync on/off and configure what is required for a sync to occur.
* An about screen, with contact information.

#### Presentation layer

This app is a Single-Activity app, with the following components:
* A main activity that handles navigation.
* A fragment to display the list of websites currently tracked.
* A fragment to display the history of changes from the selected website, when changes are not an image or a pdf.
* A fragment to display the history of changes from images in a carousel format.
* A fragment to display the history of changes from pdfs in a carousel format.

The app uses a Model-View-ViewModel (MVVM) architecture for the presentation layer. Each of the fragments corresponds to a MVVM View.
The View and ViewModel communicate using LiveData and general good principles.

#### Data layer

The database is created using Room and it has two entities: a `Site` and a `Snap` that generate corresponding SQLite tables at runtime.
There is a one to many relationshiop between them. The id from `Site` is a foreign key on `Snap`. Snap only contains the snapshot metadata, all the data retrieved from the http request (body response) is stored in Android's own File storage.

To let other components know when the data has finished populating, the `ViewModel` exposes a `LiveData` object via callbacks using interfaces (inspired from [this todo app](https://github.com/googlesamples/android-architecture/tree/dev-todo-mvvm-live)).
This could be, eventually, easily extended to work with server and sync. The app also makes use of Kotlin's Coroutines to deal with some callbacks.

#### Simple comparison process
The app works like this:

1. Make http request and store the body response in a byteArray.
2. Retrieve most recent stored file for that site, if any.
3. Convert to string, clean up Jsoup and compare them. If same, don't do anything else.
If different, add the new byteArray to storage and create a new entry on Snap table. When this happens in background, a notification is created to warn the user.

| Inside the App | Outside the App |
|:-:|:-:|
| ![inside](/.github/assets/notification_inside.jpg?raw=true) | ![outside](/.github/assets/notification_outside.jpg?raw=true) |

#### Diff Process for text files

After a change is detected and user taps to see it, a byte to byte comparision wouldn't be readable, so it makes sense to make a text comparison.

That's why this app makes extensive use from [java-diff-utils](https://github.com/wumpz/java-diff-utils).
In fact, part of the library was converted to Kotlin and is now working perfectly on Java 6 (the original library makes use of Streams, which is only supported on Java 8).
All the diff process is made using Myer's diff algorithm, and the result, for performance reasons, is put on a RecyclerView.

When this diff process happens, the app will use [jsoup](https://jsoup.org) with a [relaxed whitelist](https://jsoup.org/apidocs/org/jsoup/safety/Whitelist.html#relaxed--) to remove all the useless tags from html to avoid pages that generate them at every request.
Example: pages that make use of Google Analytics and pages that were made in WordPress.
The app will also use jsoup to unescape "<" and ">" from html.

#### Diff Process for image and pdf files

It makes no sense to compare images and visual files using strings, so there is a carousel to compare them. PDF's are rendered to an imageView, while images are rendered with support for tiling, which is great for ultra-heavy pictures - in case user is tracking changes for a 20mb photo.

#### How each Architecture Component is used
* Navigation: this is a single activity app. All fragment transactions (except one) are made using Navigation library.

* WorkManager: responsible for automatically syncing when the app is in background.
There are four constraints: *battery not low*, *device on idle state* (API 23+), *device charging* and *wifi on*.
Wifi is currently not a constraint from WorkManager, so I implemented it myself to work together.

* Paging: on details fragment. As time goes, it is possible for a website to receive hundreds of updates. To avoid OOM error once and for all, Paging was implemented. When visualizing PDF/Image changes (the carousel view), paging is implemented on the carousel, so that it doesn't loads all Files into memory at once.

* LiveData/ViewModel: written above.
* Room: written above.

#### Third Party Libraries Used

  * [Android-Iconics][1] deal with icons without suffering.
  * [Architecture Components][2] all of them, as stated above.
  * [Groupie][3] for making RecyclerViews as simple as possible.
  * [java-diff-utils][4] generate the difference between two strings.
  * [Logger][5] logs that are useful and disabled on release.
  * [material-about-library][6] create an about page without suffering.
  * [Material Dialogs][7] show dialogs in a simple and easy way.
  * [Notify][8] create notifications without effort.
  * [ok-http][9] fetch the webpages.
  * [Stetho][10] debug the database easily.
  * [ThreeTenABP][11] for dealing with time in a Java 8 way.
  * [timeago][12] makes it easy display relative dates (i.e. 1 day ago).
  * [RxJava][13] responsible for coordinating the reload button animation and updating the text on main screen cards periodically.
  * [jsoup][14] cleaning up html files.
  * [Dagger 2][15] dependency injection for sharedPreferences with application Context, provides singleton database instances.
  * [Alerter][16] show beautiful notifications when some site was updated while the user is browsing the main fragment.

[1]: https://github.com/mikepenz/Android-Iconics
[2]: https://developer.android.com/topic/libraries/architecture/
[3]: https://github.com/lisawray/groupie
[4]: https://github.com/wumpz/java-diff-utils
[5]: https://github.com/orhanobut/logger
[6]: https://github.com/daniel-stoneuk/material-about-library
[7]: https://github.com/afollestad/material-dialogs
[8]: https://github.com/Karn/notify
[9]: https://github.com/square/okhttp
[10]: http://facebook.github.io/stetho/
[11]: https://github.com/JakeWharton/ThreeTenABP
[12]: https://github.com/marlonlom/timeago
[13]: https://github.com/ReactiveX/RxJava
[14]: https://jsoup.org
[15]: https://github.com/google/dagger
[16]: https://github.com/Tapadoo/Alerter


### Reporting Issues

Issues and Pull Requests are welcome.
You can report [here](https://github.com/bernaferrari/ChangeDetection/issues).

License
-------

Copyright 2018 Bernardo Ferrari.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
