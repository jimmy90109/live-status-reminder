package com.example.ridecodereminder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RideNotificationParserTest {
    @Test
    public void entryNotificationStartsReminder() {
        assertEquals(
                RideNotificationParser.RideEvent.ENTERED,
                RideNotificationParser.parse(
                        "乘車碼交易\nNT$0\n商店名稱：臺北捷運\n中山國中2026-05-15 14:54:08 > [尚未出站]\n進站交易已完成"
                )
        );
    }

    @Test
    public void exitNotificationClearsReminder() {
        assertEquals(
                RideNotificationParser.RideEvent.EXITED,
                RideNotificationParser.parse(
                        "乘車碼交易\nNT$25\n商店名稱：臺北捷運\n中山國中 > 西湖\n出站交易已完成，謝謝您使用iPASS MONEY。"
                )
        );
    }

    @Test
    public void unrelatedNotificationsAreIgnored() {
        assertEquals(
                RideNotificationParser.RideEvent.NONE,
                RideNotificationParser.parse("付款完成")
        );
    }

    @Test
    public void foodpandaCourierOnTheWayUpdatesReminder() {
        assertEquals(
                RideNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY,
                RideNotificationParser.parseFoodpanda(
                        "foodpanda\n外送夥伴在路上囉🚴，請隨時留意手機來電或訊息！"
                )
        );
    }

    @Test
    public void foodpandaCourierArrivingUpdatesReminder() {
        assertEquals(
                RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING,
                RideNotificationParser.parseFoodpanda(
                        "foodpanda\n我們的外送夥伴即將抵達💪！"
                )
        );
    }

    @Test
    public void foodpandaEndedNotificationsClearReminder() {
        assertEquals(
                RideNotificationParser.FoodpandaEvent.ORDER_ENDED,
                RideNotificationParser.parseFoodpanda("foodpanda\n您的訂單已送達")
        );
        assertEquals(
                RideNotificationParser.FoodpandaEvent.ORDER_ENDED,
                RideNotificationParser.parseFoodpanda("foodpanda\n訂單完成，祝您用餐愉快！")
        );
        assertEquals(
                RideNotificationParser.FoodpandaEvent.ORDER_ENDED,
                RideNotificationParser.parseFoodpanda("foodpanda\n您的訂單已取消")
        );
        assertEquals(
                RideNotificationParser.FoodpandaEvent.ORDER_ENDED,
                RideNotificationParser.parseFoodpanda("foodpanda\n訂單取消")
        );
    }

    @Test
    public void unrelatedFoodpandaNotificationsAreIgnored() {
        assertEquals(
                RideNotificationParser.FoodpandaEvent.NONE,
                RideNotificationParser.parseFoodpanda("foodpanda\n優惠快訊")
        );
    }
}
