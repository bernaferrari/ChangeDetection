/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection.data.source.local;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.bernaferrari.changedetection.data.Diff;
import com.bernaferrari.changedetection.data.Site;

/**
 * The Room Database that contains the Site table.
 */
@Database(entities = {Site.class, Diff.class}, version = 1, exportSchema = false)
public abstract class ChangeDatabase extends RoomDatabase {

    private static final Object sLock = new Object();
    private static ChangeDatabase INSTANCE;

    public static ChangeDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        ChangeDatabase.class, "Changes.db")
                        .fallbackToDestructiveMigration()
                        .build();
            }
            return INSTANCE;
        }
    }

    public abstract SitesDao siteDao();

    public abstract DiffsDao diffsDao();
}
