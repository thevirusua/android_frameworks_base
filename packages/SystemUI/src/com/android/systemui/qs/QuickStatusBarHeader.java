/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.content.Intent;
import android.provider.AlarmClock;
import android.widget.TextView;
import com.android.systemui.qs.TouchAnimator.Builder;
import com.android.systemui.qs.TouchAnimator.Listener;
import com.android.systemui.qs.TouchAnimator.ListenerAdapter;

import com.android.systemui.havoc.carrierlabel.CarrierLabelQS;
import com.android.settingslib.Utils;
import com.android.systemui.qs.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.id;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSDetail.Callback;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;


public class QuickStatusBarHeader extends RelativeLayout {

    private ActivityStarter mActivityStarter;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mListening;

    protected QuickQSPanel mHeaderQsPanel;
    protected QSTileHost mHost;
    private View mDate;

	private View mHeaderView;
	private String mHeaderColor = "#00FFFFFF";

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();

        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view

        updateResources();

        // Set the light/dark theming on the header status UI to match the current theme.
        int colorForeground = Color.parseColor("#ffffff");
        float intensity = colorForeground == Color.WHITE ? 0 : 1;
        Rect tintArea = new Rect(0, 0, 0, 0);
		
		mHeaderView = findViewById(R.id.qs_top_background);
		updateColor();


        BatteryMeterView battery = findViewById(R.id.battery);

        battery.setForceShowPercent(true);
        mDate = findViewById(R.id.date);
        mActivityStarter = Dependency.get(ActivityStarter.class);
        mDate.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
        	    mActivityStarter.postStartActivityDismissingKeyguard(new Intent(
                           AlarmClock.ACTION_SHOW_ALARMS), 0);
        	}
           });
        applyDarkness(R.id.battery, tintArea, intensity, colorForeground);
        applyDarkness(R.id.clock, tintArea, intensity, colorForeground);
        applyDarkness(R.id.left_clock, tintArea, intensity, colorForeground);
        applyDarkness(R.id.date, tintArea, intensity, colorForeground);
        applyDarkness(R.id.center_clock, tintArea, intensity, colorForeground);
        applyDarkness(R.id.qs_clock, tintArea, intensity, colorForeground);
        applyDarkness(R.id.qs_left_clock, tintArea, intensity, colorForeground);
		
    }
	
	private class ColorObserver extends ContentObserver {
        ColorObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            getContext().getContentResolver().registerContentObserver(Settings.System
                    .getUriFor(Settings.System.QS_HEADER_COLOR), false,
                    this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateColor();
        }
    }
	
	private void updateColor() {
        mHeaderColor = Settings.System.getStringForUser(getContext().getContentResolver(),
                Settings.System.QS_HEADER_COLOR,
                UserHandle.USER_CURRENT);
		mHeaderView.setBackgroundColor(Color.parseColor(mHeaderColor));
    }

    private void applyDarkness(int id, Rect tintArea, float intensity, int color) {
        View v = findViewById(id);
        if (v instanceof DarkReceiver) {
            ((DarkReceiver) v).onDarkChanged(tintArea, intensity, color);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
    }

    public int getCollapsedHeight() {
        return getHeight();
    }

    public int getExpandedHeight() {
        return getHeight();
    }

    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        mHeaderQsPanel.setExpanded(expanded);
    }

    public void setExpansion(float headerExpansionFraction) {
    }

    @Override
    @VisibleForTesting
    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mHeaderQsPanel.setListening(listening);
        mListening = listening;
    }

    public void updateEverything() {
        post(() -> setClickable(false));
    }

    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
        //host.setHeaderView(mExpandIndicator);
        mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        mHeaderQsPanel.setHost(host, null /* No customization in header */);
    }

    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }
}
