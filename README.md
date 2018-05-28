Change Detection
===================================

This app tracks changes on websites you otherwise would visit frequently to see if there is something new.
Use cases:
* Teacher say grades will be published "soon", but no one knows what "soon" means and you are tired of reloading.
* You are working with a server and wants to know the result from a request, periodically.
* You are working with a server and wants to know if the server is working as intended, or if the release has happened successfully.

This app also showcases **all** the Android Architecture Components working together: [Room](https://developer.android.com/topic/libraries/architecture/room.html), [ViewModels](https://developer.android.com/reference/android/arch/lifecycle/ViewModel.html), [LiveData](https://developer.android.com/reference/android/arch/lifecycle/LiveData.html), [Paging](https://developer.android.com/topic/libraries/architecture/paging/), [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) and [Navigation](https://developer.android.com/topic/libraries/architecture/navigation/). 

 ![GIF](/.github/assets/card_gif.gif?raw=true)

## Screenshots

| Main Screen | Detail | Settings |
|:-:|:-:|:-:|
| ![First](/.github/assets/main_screen.jpg?raw=true) | ![Sec](/.github/assets/main_screen.jpg?raw=true) | ![Third](/.github/assets/settings.jpg?raw=true) |

Introduction
------------

### Features

This app contains four screens: 
* a list of websites that are currently being tracked.
* a detail view, that allows the user to compare the current website version with previous versions.
* a settings view, that allows user to toggle auto-sync on/off and configure what is required for a sync to occur.
* an about screen, with contact information.

For clarity, unless otherwise noted, I'll ignore the *settings* and *about* on most of this README; I will pretend the app has two views, the main list and the details view.

#### Presentation layer

The presentation layer consists of the following components:
* A main activity that handles navigation.
* A fragment to display the list of websites currently tracked.
* A fragment to display the details (history of changes) from the selected website.

The app uses a Model-View-ViewModel (MVVM) architecture for the presentation layer. Each of the fragments corresponds to a MVVM View.
The View and ViewModel communicate using LiveData and general good principles.

#### Data layer

The database is created using Room and it has two entities: a `Site` and a `Diff` that generate corresponding SQLite tables at runtime.

To let other components know when the data has finished populating, the `ViewModel` exposes a `LiveData` object via callbacks using interfaces (inspired from [this todo app](https://github.com/googlesamples/android-architecture/tree/dev-todo-mvvm-live)).
This could be extended to work with server and sync.

#### Diff Process

This app makes extensive use from [java-diff-utils](https://github.com/wumpz/java-diff-utils). All the diff process is made using Myer's diff algorithm, and the result is put on a RecyclerView (to avoid memory issues with long text).

#### How each Architecture Component is used
* Navigation: this is a single activity app. All fragment transactions (except one) are made using Navigation library.

* WorkManager: responsible for automatically syncing when the app is in background.
There are four constraints: *battery not low*, *device on idle state* (API 23+), *device charging* and *wifi on*.
Wifi is currently not a constraint from WorkManager, so I implemented it myself to work together.

* Paging: on details fragment. It is possible for a website to change hundreds of times.
If it changes frequently, in a year there will be hundreds of items.
To avoid OOM error once and for all, Paging was implemented.

* LiveData/ViewModel: written above.
* Room: written above.

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