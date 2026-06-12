package com.example.ridecodereminder;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 7;
    private static final String ACTION_OPEN_IPASS =
            "com.example.ridecodereminder.action.OPEN_IPASS";
    private static final String IPASS_PACKAGE = "com.ipass.ipassmoney";

    private static final int BACKGROUND = Color.rgb(246, 251, 247);
    private static final int ON_SURFACE = Color.rgb(24, 48, 41);
    private static final int ON_SURFACE_VARIANT = Color.rgb(70, 94, 86);
    private static final int PRIMARY = Color.rgb(0, 111, 79);
    private static final int PRIMARY_CONTAINER = Color.rgb(191, 239, 216);
    private static final int SECONDARY_CONTAINER = Color.rgb(214, 238, 224);
    private static final int TERTIARY_CONTAINER = Color.rgb(205, 236, 205);
    private static final int SURFACE_CONTAINER = Color.rgb(233, 244, 237);
    private static final int SUCCESS_CONTAINER = Color.rgb(206, 237, 217);
    private static final int SUCCESS_TEXT = Color.rgb(27, 82, 49);
    private static final int WARNING_CONTAINER = Color.rgb(255, 224, 170);
    private static final int WARNING_TEXT = Color.rgb(91, 55, 0);

    private TextView notificationAccessStatus;
    private TextView notificationPermissionStatus;
    private TextView liveUpdateStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RideReminder.createChannel(this);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        if (ACTION_OPEN_IPASS.equals(getIntent().getAction())) {
            openIpass();
            finish();
            return;
        }
        setContentView(createContentView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    public static Intent createOpenIpassIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .setAction(ACTION_OPEN_IPASS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private View createContentView() {
        LinearLayout page = column();
        page.setPadding(dp(20), dp(20), dp(20), dp(32));
        page.setBackgroundColor(BACKGROUND);

        page.addView(heroCard());
        page.addView(spacer(28));
        page.addView(sectionLabel("必要設定", "完成設定後，App 就能在乘車時自動提醒你。"));
        page.addView(spacer(12));

        notificationAccessStatus = statusPill();
        page.addView(settingCard(
                "01",
                "讀取乘車通知",
                "允許 App 辨識 iPASS MONEY 的進站與出站交易。",
                notificationAccessStatus,
                "開啟通知存取權限",
                view -> openNotificationListenerSettings(),
                PRIMARY_CONTAINER
        ));
        page.addView(spacer(10));

        notificationPermissionStatus = statusPill();
        page.addView(settingCard(
                "02",
                "顯示下車提醒",
                "讓即時通知固定顯示在通知列，點一下就能開啟乘車碼。",
                notificationPermissionStatus,
                "允許提醒通知",
                view -> requestNotificationPermission(),
                SECONDARY_CONTAINER
        ));

        liveUpdateStatus = statusPill();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            page.addView(spacer(10));
            page.addView(settingCard(
                    "03",
                    "Live Update",
                    "允許乘車狀態顯示為 Android 16 即時通知與狀態列 chip。",
                    liveUpdateStatus,
                    "開啟 Live Update 設定",
                    view -> openLiveUpdateSettings(),
                    TERTIARY_CONTAINER
            ));
        } else {
            liveUpdateStatus.setVisibility(View.GONE);
        }

        page.addView(spacer(28));
        page.addView(sectionLabel("先試一次", "不用真的搭車，也可以確認提醒操作是否順手。"));
        page.addView(spacer(12));
        page.addView(testCard());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(page);
        return scrollView;
    }

    private View heroCard() {
        LinearLayout card = card(PRIMARY_CONTAINER, 36);
        card.setPadding(dp(22), dp(22), dp(22), dp(24));
        card.addView(labelPill("LIVE RIDE", PRIMARY, Color.WHITE));
        card.addView(spacer(34));
        card.addView(text("下車時，\n不用再手忙腳亂。", 34, Color.rgb(0, 72, 51), true));
        card.addView(spacer(12));
        card.addView(text(
                "進站後自動顯示乘車碼捷徑，準備下車時點一下就能開啟 iPASS MONEY。",
                16,
                Color.rgb(33, 91, 71),
                false
        ));
        card.addView(spacer(20));
        card.addView(actionButton(
                "立即開啟 iPASS MONEY  →",
                PRIMARY,
                Color.WHITE,
                view -> openIpass()
        ));
        return card;
    }

    private View settingCard(
            String number,
            String title,
            String description,
            TextView status,
            String action,
            View.OnClickListener listener,
            int accentColor
    ) {
        LinearLayout card = card(SURFACE_CONTAINER, 26);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout top = row();
        TextView numberPill = labelPill(number, accentColor, ON_SURFACE);
        top.addView(numberPill);
        top.addView(horizontalSpacer(10));
        top.addView(text(title, 20, ON_SURFACE, true));
        card.addView(top);
        card.addView(spacer(12));
        card.addView(text(description, 15, ON_SURFACE_VARIANT, false));
        card.addView(spacer(14));
        card.addView(status);
        card.addView(spacer(14));
        card.addView(actionButton(action + "  →", accentColor, ON_SURFACE, listener));
        return card;
    }

    private View testCard() {
        LinearLayout card = card(Color.rgb(224, 242, 227), 30);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(text("測試搭車流程", 20, ON_SURFACE, true));
        card.addView(spacer(6));
        card.addView(text("依序點擊模擬上車與模擬下車，確認通知是否出現並消失。", 15, ON_SURFACE_VARIANT, false));
        card.addView(spacer(14));
        card.addView(actionButton(
                "模擬上車，顯示提醒  ↑",
                PRIMARY,
                Color.WHITE,
                view -> RideReminder.show(this)
        ));
        card.addView(spacer(8));
        card.addView(actionButton("模擬下車，移除提醒  ✓", TERTIARY_CONTAINER, ON_SURFACE, view -> RideReminder.clear(this)));
        return card;
    }

    private View sectionLabel(String title, String subtitle) {
        LinearLayout group = column();
        group.setPadding(dp(4), 0, dp(4), 0);
        group.addView(text(title, 24, ON_SURFACE, true));
        group.addView(spacer(4));
        group.addView(text(subtitle, 15, ON_SURFACE_VARIANT, false));
        return group;
    }

    private void refreshStatus() {
        if (notificationAccessStatus == null
                || notificationPermissionStatus == null
                || liveUpdateStatus == null) {
            return;
        }
        setStatus(notificationAccessStatus, isNotificationAccessEnabled(), "已開啟", "尚未開啟");
        setStatus(notificationPermissionStatus, canPostNotifications(), "已允許", "尚未允許");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            setStatus(
                    liveUpdateStatus,
                    getSystemService(NotificationManager.class).canPostPromotedNotifications(),
                    "系統允許顯示",
                    "未允許，將使用一般通知"
            );
        }
    }

    private void setStatus(TextView view, boolean enabled, String enabledText, String disabledText) {
        view.setText((enabled ? "✓  " : "•  ") + enabledText);
        view.setTextColor(enabled ? SUCCESS_TEXT : WARNING_TEXT);
        view.setBackground(roundRect(enabled ? SUCCESS_CONTAINER : WARNING_CONTAINER, 100));
    }

    private boolean isNotificationAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners"
        );
        ComponentName listener = new ComponentName(this, RideNotificationListenerService.class);
        return enabledListeners != null && enabledListeners.contains(listener.flattenToString());
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private void openNotificationListenerSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        }
    }

    private void openLiveUpdateSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "這台裝置沒有提供 Live Update 設定頁。", Toast.LENGTH_LONG).show();
        }
    }

    private void openIpass() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(IPASS_PACKAGE);
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        Toast.makeText(this, "尚未安裝 iPASS MONEY，將開啟 Google Play。", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + IPASS_PACKAGE)));
    }

    private LinearLayout card(int color, int radiusDp) {
        LinearLayout card = column();
        card.setBackground(roundRect(color, radiusDp));
        card.setClipToOutline(true);
        return card;
    }

    private TextView actionButton(String label, int background, int foreground, View.OnClickListener listener) {
        TextView button = text(label, 15, foreground, true);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setBackground(ripple(background, 100));
        button.setOnClickListener(listener);
        return button;
    }

    private TextView labelPill(String label, int background, int foreground) {
        TextView pill = text(label, 12, foreground, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        pill.setBackground(roundRect(background, 100));
        pill.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return pill;
    }

    private TextView statusPill() {
        TextView status = text("", 13, ON_SURFACE_VARIANT, true);
        status.setPadding(dp(12), dp(7), dp(12), dp(7));
        status.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return status;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private RippleDrawable ripple(int background, int radiusDp) {
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(40, 0, 0, 0)),
                roundRect(background, radiusDp),
                null
        );
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        if (bold) {
            view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        return view;
    }

    private View spacer(int heightDp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return spacer;
    }

    private View horizontalSpacer(int widthDp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), 1));
        return spacer;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
