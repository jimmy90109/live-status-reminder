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
    private static final int TAB_IPASS = 0;
    private static final int TAB_FOODPANDA = 1;
    private static final int TAB_UBER_EATS = 2;
    private static final String ACTION_OPEN_IPASS =
            "com.example.ridecodereminder.action.OPEN_IPASS";
    private static final String ACTION_OPEN_FOODPANDA =
            "com.example.ridecodereminder.action.OPEN_FOODPANDA";
    private static final String ACTION_OPEN_UBER_EATS =
            "com.example.ridecodereminder.action.OPEN_UBER_EATS";
    private static final String IPASS_PACKAGE = "com.ipass.ipassmoney";
    private static final String FOODPANDA_PACKAGE = "com.global.foodpanda.android";
    private static final String UBER_EATS_PACKAGE = "com.ubercab.eats";

    private static final int BACKGROUND = Color.rgb(248, 249, 250);
    private static final int ON_SURFACE = Color.rgb(32, 37, 42);
    private static final int ON_SURFACE_VARIANT = Color.rgb(91, 99, 106);
    private static final int COMMON_PRIMARY = Color.rgb(96, 105, 114);
    private static final int COMMON_CONTAINER = Color.rgb(232, 235, 238);
    private static final int COMMON_SURFACE = Color.rgb(240, 242, 244);
    private static final int IPASS_PRIMARY = Color.rgb(0, 111, 79);
    private static final int IPASS_CONTAINER = Color.rgb(191, 239, 216);
    private static final int IPASS_SECONDARY_CONTAINER = Color.rgb(214, 238, 224);
    private static final int IPASS_TERTIARY_CONTAINER = Color.rgb(205, 236, 205);
    private static final int FOODPANDA_PRIMARY = Color.rgb(224, 0, 119);
    private static final int FOODPANDA_CONTAINER = Color.rgb(255, 214, 235);
    private static final int FOODPANDA_SECONDARY_CONTAINER = Color.rgb(255, 232, 244);
    private static final int FOODPANDA_TEXT = Color.rgb(105, 0, 56);
    private static final int UBER_EATS_PRIMARY = Color.rgb(6, 143, 77);
    private static final int UBER_EATS_CONTAINER = Color.rgb(207, 244, 221);
    private static final int UBER_EATS_SECONDARY_CONTAINER = Color.rgb(229, 247, 235);
    private static final int UBER_EATS_TEXT = Color.rgb(0, 80, 42);
    private static final int SUCCESS_CONTAINER = Color.rgb(206, 237, 217);
    private static final int SUCCESS_TEXT = Color.rgb(27, 82, 49);
    private static final int WARNING_CONTAINER = Color.rgb(255, 224, 170);
    private static final int WARNING_TEXT = Color.rgb(91, 55, 0);

    private TextView notificationAccessStatus;
    private TextView notificationPermissionStatus;
    private TextView liveUpdateStatus;
    private int selectedAppTab = TAB_IPASS;
    private TextView ipassTabButton;
    private TextView foodpandaTabButton;
    private TextView uberEatsTabButton;
    private LinearLayout appTabContent;
    private TextView ipassInstallStatus;
    private TextView foodpandaInstallStatus;
    private TextView uberEatsInstallStatus;

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
        if (ACTION_OPEN_FOODPANDA.equals(getIntent().getAction())) {
            openFoodpanda();
            finish();
            return;
        }
        if (ACTION_OPEN_UBER_EATS.equals(getIntent().getAction())) {
            openUberEats();
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

    public static Intent createOpenFoodpandaIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .setAction(ACTION_OPEN_FOODPANDA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public static Intent createOpenUberEatsIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .setAction(ACTION_OPEN_UBER_EATS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private View createContentView() {
        LinearLayout page = column();
        page.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        page.setPadding(dp(20), dp(20), dp(20), dp(32));
        page.setBackgroundColor(BACKGROUND);

        page.addView(heroCard());
        page.addView(spacer(28));
        page.addView(sectionLabel("必要設定", "完成設定後，App 就能把乘車與外送狀態變成即時通知。"));
        page.addView(spacer(12));

        notificationAccessStatus = statusPill();
        page.addView(settingCard(
                "01",
                "讀取狀態通知",
                "允許 App 辨識 iPASS MONEY、foodpanda 與 Uber Eats 狀態。",
                notificationAccessStatus,
                "開啟通知存取權限",
                view -> openNotificationListenerSettings(),
                COMMON_CONTAINER
        ));
        page.addView(spacer(10));

        notificationPermissionStatus = statusPill();
        page.addView(settingCard(
                "02",
                "顯示即時通知",
                "讓提醒固定顯示在通知列，點一下就能開啟對應 App。",
                notificationPermissionStatus,
                "允許提醒通知",
                view -> requestNotificationPermission(),
                COMMON_CONTAINER
        ));

        liveUpdateStatus = statusPill();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            page.addView(spacer(10));
            page.addView(settingCard(
                    "03",
                    "Live Update",
                    "允許乘車與外送狀態顯示為 Android 16 即時通知與狀態列 chip。",
                    liveUpdateStatus,
                    "開啟 Live Update 設定",
                    view -> openLiveUpdateSettings(),
                    COMMON_CONTAINER
            ));
        } else {
            liveUpdateStatus.setVisibility(View.GONE);
        }

        page.addView(spacer(28));
        page.addView(sectionLabel("App", "檢查安裝狀態，並分別測試各 App 的即時通知。"));
        page.addView(spacer(12));
        page.addView(appTabs());
        page.addView(spacer(12));
        appTabContent = column();
        appTabContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        page.addView(appTabContent);
        showSelectedAppTab();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(page);
        return scrollView;
    }

    private View heroCard() {
        LinearLayout card = card(COMMON_CONTAINER, 36);
        card.setPadding(dp(22), dp(22), dp(22), dp(24));
        card.addView(labelPill("LIVE STATUS", COMMON_PRIMARY, Color.WHITE));
        card.addView(spacer(34));
        card.addView(text("重要狀態，\n留在最前面。", 34, ON_SURFACE, true));
        card.addView(spacer(12));
        card.addView(text(
                "進站後顯示乘車碼捷徑；外送期間顯示 foodpanda 或 Uber Eats 訂單進度。",
                16,
                ON_SURFACE_VARIANT,
                false
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
        LinearLayout card = card(COMMON_SURFACE, 26);
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
        card.addView(actionButton(action + "  →", COMMON_PRIMARY, Color.WHITE, listener));
        return card;
    }

    private View appTabs() {
        LinearLayout tabs = row();
        tabs.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(roundRect(COMMON_SURFACE, 100));
        ipassTabButton = appTabButton("iPASS MONEY", view -> showIpassTab());
        foodpandaTabButton = appTabButton("foodpanda", view -> showFoodpandaTab());
        uberEatsTabButton = appTabButton("Uber Eats", view -> showUberEatsTab());
        tabs.addView(ipassTabButton);
        tabs.addView(horizontalSpacer(4));
        tabs.addView(foodpandaTabButton);
        tabs.addView(horizontalSpacer(4));
        tabs.addView(uberEatsTabButton);
        return tabs;
    }

    private TextView appTabButton(String label, View.OnClickListener listener) {
        TextView button = text(label, 14, ON_SURFACE_VARIANT, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(11), dp(12), dp(11));
        button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        return button;
    }

    private void showSelectedAppTab() {
        if (selectedAppTab == TAB_UBER_EATS) {
            showUberEatsTab();
        } else if (selectedAppTab == TAB_FOODPANDA) {
            showFoodpandaTab();
        } else {
            showIpassTab();
        }
    }

    private void showIpassTab() {
        selectedAppTab = TAB_IPASS;
        if (appTabContent == null) {
            return;
        }
        setTabSelected(ipassTabButton, true, IPASS_PRIMARY);
        setTabSelected(foodpandaTabButton, false, FOODPANDA_PRIMARY);
        setTabSelected(uberEatsTabButton, false, UBER_EATS_PRIMARY);
        appTabContent.removeAllViews();
        ipassInstallStatus = statusPill();
        appTabContent.addView(appCard(
                "iPASS MONEY",
                "乘車碼狀態",
                "進站後顯示乘車碼捷徑，準備下車時快速開啟。",
                ipassInstallStatus,
                "開啟 iPASS MONEY",
                view -> openIpass(),
                IPASS_CONTAINER,
                IPASS_SECONDARY_CONTAINER,
                IPASS_PRIMARY,
                ON_SURFACE,
                new View[]{
                        actionButton(
                                "模擬上車，顯示提醒  ↑",
                                IPASS_PRIMARY,
                                Color.WHITE,
                                view -> RideReminder.show(this)
                        ),
                        actionButton(
                                "模擬下車，移除提醒  ✓",
                                IPASS_TERTIARY_CONTAINER,
                                ON_SURFACE,
                                view -> RideReminder.clear(this)
                        )
                }
        ));
        refreshInstallStatus();
    }

    private void showFoodpandaTab() {
        selectedAppTab = TAB_FOODPANDA;
        if (appTabContent == null) {
            return;
        }
        setTabSelected(ipassTabButton, false, IPASS_PRIMARY);
        setTabSelected(foodpandaTabButton, true, FOODPANDA_PRIMARY);
        setTabSelected(uberEatsTabButton, false, UBER_EATS_PRIMARY);
        appTabContent.removeAllViews();
        foodpandaInstallStatus = statusPill();
        appTabContent.addView(appCard(
                "foodpanda",
                "外送訂單狀態",
                "外送夥伴出發或即將抵達時，顯示取餐提醒。",
                foodpandaInstallStatus,
                "開啟 foodpanda",
                view -> openFoodpanda(),
                FOODPANDA_CONTAINER,
                FOODPANDA_SECONDARY_CONTAINER,
                FOODPANDA_PRIMARY,
                FOODPANDA_TEXT,
                new View[]{
                        actionButton(
                                "模擬外送中",
                                FOODPANDA_PRIMARY,
                                Color.WHITE,
                                view -> RideReminder.showFoodpanda(
                                        this,
                                        RideNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY
                                )
                        ),
                        actionButton(
                                "模擬即將抵達",
                                FOODPANDA_SECONDARY_CONTAINER,
                                FOODPANDA_TEXT,
                                view -> RideReminder.showFoodpanda(
                                        this,
                                        RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING
                                )
                        ),
                        actionButton(
                                "清除 foodpanda 狀態  ✓",
                                FOODPANDA_SECONDARY_CONTAINER,
                                FOODPANDA_TEXT,
                                view -> RideReminder.clearFoodpanda(this)
                        )
                }
        ));
        refreshInstallStatus();
    }

    private void showUberEatsTab() {
        selectedAppTab = TAB_UBER_EATS;
        if (appTabContent == null) {
            return;
        }
        setTabSelected(ipassTabButton, false, IPASS_PRIMARY);
        setTabSelected(foodpandaTabButton, false, FOODPANDA_PRIMARY);
        setTabSelected(uberEatsTabButton, true, UBER_EATS_PRIMARY);
        appTabContent.removeAllViews();
        uberEatsInstallStatus = statusPill();
        appTabContent.addView(appCard(
                "Uber Eats",
                "五階段訂單進度",
                "從接單到即將抵達持續更新；可辨識時也會顯示四位數 PIN。",
                uberEatsInstallStatus,
                "開啟 Uber Eats",
                view -> openUberEats(),
                UBER_EATS_CONTAINER,
                UBER_EATS_SECONDARY_CONTAINER,
                UBER_EATS_PRIMARY,
                UBER_EATS_TEXT,
                new View[]{
                        uberEatsTestButton(
                                "模擬訂單已收到",
                                RideNotificationParser.UberEatsEvent.ORDER_RECEIVED,
                                null,
                                UBER_EATS_PRIMARY,
                                Color.WHITE
                        ),
                        uberEatsTestButton(
                                "模擬正在準備訂單",
                                RideNotificationParser.UberEatsEvent.PREPARING,
                                null,
                                UBER_EATS_SECONDARY_CONTAINER,
                                UBER_EATS_TEXT
                        ),
                        uberEatsTestButton(
                                "模擬正在取餐",
                                RideNotificationParser.UberEatsEvent.PICKING_UP,
                                null,
                                UBER_EATS_SECONDARY_CONTAINER,
                                UBER_EATS_TEXT
                        ),
                        uberEatsTestButton(
                                "模擬配送中",
                                RideNotificationParser.UberEatsEvent.ON_THE_WAY,
                                null,
                                UBER_EATS_SECONDARY_CONTAINER,
                                UBER_EATS_TEXT
                        ),
                        uberEatsTestButton(
                                "模擬快到了（PIN 7616）",
                                RideNotificationParser.UberEatsEvent.ARRIVING,
                                "7616",
                                UBER_EATS_SECONDARY_CONTAINER,
                                UBER_EATS_TEXT
                        ),
                        actionButton(
                                "模擬送達，清除狀態  ✓",
                                UBER_EATS_SECONDARY_CONTAINER,
                                UBER_EATS_TEXT,
                                view -> RideReminder.clearUberEats(this)
                        )
                }
        ));
        refreshInstallStatus();
    }

    private TextView uberEatsTestButton(
            String label,
            RideNotificationParser.UberEatsEvent event,
            String pin,
            int background,
            int foreground
    ) {
        return actionButton(
                label,
                background,
                foreground,
                view -> RideReminder.showUberEats(this, event, pin)
        );
    }

    private View appCard(
            String appName,
            String title,
            String description,
            TextView installStatus,
            String openAction,
            View.OnClickListener openListener,
            int cardColor,
            int labelColor,
            int primaryColor,
            int foregroundColor,
            View[] testActions
    ) {
        LinearLayout card = card(cardColor, 30);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(labelPill(appName, labelColor, foregroundColor));
        card.addView(spacer(12));
        card.addView(text(title, 20, ON_SURFACE, true));
        card.addView(spacer(6));
        card.addView(text(description, 15, ON_SURFACE_VARIANT, false));
        card.addView(spacer(14));
        card.addView(installStatus);
        card.addView(spacer(14));
        card.addView(actionButton(openAction + "  →", primaryColor, Color.WHITE, openListener));
        card.addView(spacer(14));
        for (int index = 0; index < testActions.length; index++) {
            if (index > 0) {
                card.addView(spacer(8));
            }
            card.addView(testActions[index]);
        }
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
        refreshInstallStatus();
    }

    private void setStatus(TextView view, boolean enabled, String enabledText, String disabledText) {
        view.setText((enabled ? "✓  " : "•  ") + enabledText);
        view.setTextColor(enabled ? SUCCESS_TEXT : WARNING_TEXT);
        view.setBackground(roundRect(enabled ? SUCCESS_CONTAINER : WARNING_CONTAINER, 100));
    }

    private void refreshInstallStatus() {
        if (ipassInstallStatus != null) {
            setStatus(
                    ipassInstallStatus,
                    isPackageInstalled(IPASS_PACKAGE),
                    "已安裝",
                    "尚未安裝"
            );
        }
        if (foodpandaInstallStatus != null) {
            setStatus(
                    foodpandaInstallStatus,
                    isPackageInstalled(FOODPANDA_PACKAGE),
                    "已安裝",
                    "尚未安裝"
            );
        }
        if (uberEatsInstallStatus != null) {
            setStatus(
                    uberEatsInstallStatus,
                    isPackageInstalled(UBER_EATS_PACKAGE),
                    "已安裝",
                    "尚未安裝"
            );
        }
    }

    private void setTabSelected(TextView button, boolean selected, int selectedColor) {
        if (button == null) {
            return;
        }
        button.setTextColor(selected ? Color.WHITE : ON_SURFACE_VARIANT);
        button.setBackground(selected ? roundRect(selectedColor, 100) : roundRect(Color.TRANSPARENT, 100));
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

    private boolean isPackageInstalled(String packageName) {
        return getPackageManager().getLaunchIntentForPackage(packageName) != null;
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
        openPackage(IPASS_PACKAGE, "iPASS MONEY");
    }

    private void openFoodpanda() {
        openPackage(FOODPANDA_PACKAGE, "foodpanda");
    }

    private void openUberEats() {
        openPackage(UBER_EATS_PACKAGE, "Uber Eats");
    }

    private void openPackage(String packageName, String appName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        Toast.makeText(this, "尚未安裝 " + appName + "，將開啟 Google Play。", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
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
