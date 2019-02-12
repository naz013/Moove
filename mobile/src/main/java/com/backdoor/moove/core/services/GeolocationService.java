package com.backdoor.moove.core.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.backdoor.moove.R;
import com.backdoor.moove.ReminderDialogActivity;
import com.backdoor.moove.core.consts.Constants;
import com.backdoor.moove.core.consts.Prefs;
import com.backdoor.moove.core.helper.DataBase;
import com.backdoor.moove.core.helper.Module;
import com.backdoor.moove.core.helper.Notifier;
import com.backdoor.moove.core.helper.SharedPrefs;
import com.backdoor.moove.core.helper.Widget;
import com.backdoor.moove.core.location.LocationTracker;
import com.backdoor.moove.core.utils.TimeUtil;
import com.backdoor.moove.core.utils.ViewUtils;
import com.backdoor.moove.core.widgets.LeftDistanceWidgetConfigureActivity;
import com.backdoor.moove.core.widgets.SimpleWidgetConfigureActivity;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class GeolocationService extends Service {

    private static final String TAG = "GeolocationService";
    public static final int NOTIFICATION_ID = 1245;

    private LocationTracker mTracker;
    private boolean isNotificationEnabled;
    private int stockRadius;
    private boolean isWear;

    private LocationTracker.Callback mLocationCallback = (lat, lon) -> {
        Location locationA = new Location("point A");
        locationA.setLatitude(lat);
        locationA.setLongitude(lon);
        checkReminders(locationA);
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTracker != null) mTracker.removeUpdates();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPrefs prefs = SharedPrefs.getInstance(this);
        isWear = prefs.loadBoolean(Prefs.WEAR_NOTIFICATION);
        isNotificationEnabled = prefs.loadBoolean(Prefs.TRACKING_NOTIFICATION);
        stockRadius = prefs.loadInt(Prefs.LOCATION_RADIUS);
        mTracker = new LocationTracker(getApplicationContext(), mLocationCallback);
        showDefaultNotification();
        return START_STICKY;
    }

    private void checkReminders(Location locationA) {
        DataBase db = new DataBase(getApplicationContext());
        db.open();
        Cursor c = db.getReminders(Constants.ENABLE);
        if (c != null && c.moveToFirst()) {
            do {
                double lat = c.getDouble(c.getColumnIndex(DataBase.LATITUDE));
                double lon = c.getDouble(c.getColumnIndex(DataBase.LONGITUDE));
                long id = c.getLong(c.getColumnIndex(DataBase._ID));
                long startTime = c.getLong(c.getColumnIndex(DataBase.START_TIME));
                String task = c.getString(c.getColumnIndex(DataBase.SUMMARY));
                String type = c.getString(c.getColumnIndex(DataBase.TYPE));
                int status = c.getInt(c.getColumnIndex(DataBase.STATUS));
                int statusNot = c.getInt(c.getColumnIndex(DataBase.STATUS_NOTIFICATION));
                int statusRem = c.getInt(c.getColumnIndex(DataBase.STATUS_REMINDER));
                int radius = c.getInt(c.getColumnIndex(DataBase.RADIUS));
                String widgetId = c.getString(c.getColumnIndex(DataBase.WIDGET_ID));
                if (radius == -1) {
                    radius = stockRadius;
                }
                if (startTime <= 0) {
                    Location locationB = new Location("point B");
                    locationB.setLatitude(lat);
                    locationB.setLongitude(lon);
                    float distance = locationA.distanceTo(locationB);
                    int roundedDistance = Math.round(distance);
                    if (type.startsWith(Constants.TYPE_LOCATION_OUT)) {
                        if (status == Constants.NOT_LOCKED) {
                            if (roundedDistance < radius) {
                                db.setLocationStatus(id, Constants.LOCKED);
                            }
                        } else {
                            if (roundedDistance > radius) {
                                if (statusRem != Constants.SHOWN) {
                                    showReminder(id, task);
                                }
                            } else {
                                if (isNotificationEnabled) {
                                    showNotification(id, roundedDistance, statusNot, task, isWear);
                                }
                                updateWidget(widgetId, roundedDistance);
                            }
                        }
                    } else {
                        if (roundedDistance <= radius) {
                            if (statusRem != Constants.SHOWN) {
                                showReminder(id, task);
                            }
                        } else {
                            if (isNotificationEnabled) {
                                showNotification(id, roundedDistance, statusNot, task, isWear);
                            }
                            updateWidget(widgetId, roundedDistance);
                        }
                    }
                } else {
                    if (TimeUtil.isCurrent(startTime)) {
                        Location locationB = new Location("point B");
                        locationB.setLatitude(lat);
                        locationB.setLongitude(lon);
                        float distance = locationA.distanceTo(locationB);
                        int roundedDistance = Math.round(distance);
                        if (type.startsWith(Constants.TYPE_LOCATION_OUT)) {
                            if (status == Constants.NOT_LOCKED) {
                                if (roundedDistance <= radius) {
                                    db.setLocationStatus(id, Constants.LOCKED);
                                }
                            } else {
                                if (roundedDistance > radius) {
                                    if (statusRem != Constants.SHOWN) {
                                        showReminder(id, task);
                                    }
                                } else {
                                    if (isNotificationEnabled) {
                                        showNotification(id, roundedDistance, statusNot, task, isWear);
                                    }
                                    updateWidget(widgetId, roundedDistance);
                                }
                            }
                        } else {
                            if (roundedDistance <= radius) {
                                if (statusRem != Constants.SHOWN) {
                                    showReminder(id, task);
                                }
                            } else {
                                if (isNotificationEnabled) {
                                    showNotification(id, roundedDistance, statusNot, task, isWear);
                                }
                                updateWidget(widgetId, roundedDistance);
                            }
                        }
                    }
                }
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }
        db.close();
    }

    private void updateWidget(String prefsKey, int distance) {
        if (prefsKey != null) {
            Context context = getApplicationContext();
            LeftDistanceWidgetConfigureActivity.saveDistancePref(context, prefsKey, distance);
            SimpleWidgetConfigureActivity.saveDistancePref(context, prefsKey, distance);

            Widget.updateWidgets(context);
        }
    }

    private void showReminder(long id, String task) {
        DataBase db = new DataBase(getApplicationContext());
        db.open().setReminderStatus(id, Constants.SHOWN);
        db.close();
        Intent resultIntent = new Intent(getApplicationContext(), ReminderDialogActivity.class);
        resultIntent.putExtra("taskDialog", task);
        resultIntent.putExtra(Constants.ITEM_ID_INTENT, id);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(resultIntent);
    }

    private void showNotification(long id, int roundedDistance, int shown, String task, boolean isWear) {
        Integer i = (int) (long) id;
        Context context = getApplicationContext();
        String content = String.valueOf(roundedDistance);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifier.CHANNEL_SYSTEM);
        builder.setContentText(content);
        builder.setContentTitle(task);
        builder.setSmallIcon(R.drawable.ic_navigation_white_24dp);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Module.isLollipop()) {
            builder.setColor(ViewUtils.getColor(context, R.color.themePrimaryDark));
        }

        if (isWear) {
            if (Module.isJellyBean()) {
                builder.setOnlyAlertOnce(true);
                builder.setGroup("LOCATION");
                builder.setGroupSummary(true);
            }
        }

        if (shown != Constants.SHOWN) {
            DataBase db = new DataBase(context);
            db.open().setStatusNotification(id, Constants.SHOWN);
            db.close();
        }
        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        mNotifyMgr.notify(i, builder.build());

        if (isWear) {
            if (Module.isJellyBean()) {
                final NotificationCompat.Builder wearableNotificationBuilder = new NotificationCompat.Builder(context, Notifier.CHANNEL_SYSTEM);
                wearableNotificationBuilder.setSmallIcon(R.drawable.ic_navigation_white_24dp);
                wearableNotificationBuilder.setContentTitle(task);
                wearableNotificationBuilder.setContentText(content);
                wearableNotificationBuilder.setOngoing(false);
                if (Module.isLollipop()) {
                    wearableNotificationBuilder.setColor(ViewUtils.getColor(context, R.color.themePrimaryDark));
                }
                wearableNotificationBuilder.setOnlyAlertOnce(true);
                wearableNotificationBuilder.setGroup("LOCATION");
                wearableNotificationBuilder.setGroupSummary(false);
                mNotifyMgr.notify(i, wearableNotificationBuilder.build());
            }
        }
    }

    private void showDefaultNotification() {
        if (!isNotificationEnabled) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Notifier.CHANNEL_SYSTEM);
        builder.setContentText(getString(R.string.app_name));

        builder.setContentTitle(getString(R.string.location_tracking_service_running));
        builder.setSmallIcon(R.drawable.ic_navigation_white_24dp);
        startForeground(NOTIFICATION_ID, builder.build());
    }
}
