/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.voidstar.glass.nerf;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class HudService extends Service {
    private static final String LIVE_CARD_ID = "nerfHud";
    
    private final BlasterManager.OnChangedListener mBlasterOnChangedListener = 
    		new BlasterManager.OnChangedListener() {
				
				@Override
				public void onStatsChanged(BlasterManager blasterManager) {
					if (mRemoteViews == null || mLiveCard == null ||
							mLiveCard.isPublished() == false) return;
					
					mRemoteViews.setTextViewText(R.id.ammoLeft, 
							Integer.toString(blasterManager.getAmmoLeft()));
					
					mRemoteViews.setTextViewText(R.id.magSize, 
							Integer.toString(blasterManager.getMagSize()));
					
					if (blasterManager.getAmmoLeft() == 0) {
						mRemoteViews.setViewVisibility(R.id.warningBox, View.VISIBLE);
						mRemoteViews.setTextViewText(R.id.warningString, "No Ammo!");
					}
					else mRemoteViews.setViewVisibility(R.id.warningBox, View.INVISIBLE);

					if (blasterManager.getMagInserted() == false) {
						mRemoteViews.setViewVisibility(R.id.warningBox, View.VISIBLE);
						mRemoteViews.setTextViewText(R.id.warningString, "No Mag!");
					}
					else mRemoteViews.setViewVisibility(R.id.warningBox, View.INVISIBLE);

					mLiveCard.setViews(mRemoteViews);
				}
			};
    
    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class HudBinder extends Binder {
    	public void pairBlaster() {
    		mBlasterManager.Pair(getBaseContext()); // TODO: Check this
    	}
    	
		public void connectBlaster() {
			mBlasterManager.Connect();
		}
		
		public void toggleScreenOn() {
			isScreenOn = !isScreenOn;
			//mRenderer.keepScreenOn(isScreenOn);
		}
    }

    private final HudBinder mBinder = new HudBinder();

    private BlasterManager mBlasterManager;
   
    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private RemoteViews mRemoteViews;

    //private HudRenderer mRenderer;
    
    private boolean isScreenOn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        mTimelineManager = TimelineManager.from(this);
        
        mBlasterManager = new BlasterManager();
        mBlasterManager.addOnChangedListener(mBlasterOnChangedListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);
            //mRenderer = new HudRenderer(this, mBlasterManager);

            mRemoteViews = new RemoteViews(getPackageName(), R.layout.nerfhud);
            mLiveCard.setViews(mRemoteViews);
            
            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mBlasterManager.Disconnect();
        mBlasterManager = null;

        super.onDestroy();
    }
}
