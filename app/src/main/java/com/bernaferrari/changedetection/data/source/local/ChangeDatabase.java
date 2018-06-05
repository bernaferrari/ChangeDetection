package com.bernaferrari.changedetection.data.source.local;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.bernaferrari.changedetection.data.Site;
import com.bernaferrari.changedetection.data.Snap;

/**
 * The Room Database that contains the Site table.
 * Inspired from Architecture Components MVVM sample app
 */
@Database(entities = {Site.class, Snap.class}, version = 1, exportSchema = false)
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

    public abstract SnapsDao snapsDao();
}
