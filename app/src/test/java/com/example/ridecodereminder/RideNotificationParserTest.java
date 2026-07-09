package com.example.ridecodereminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test
    public void uberEatsParsesAllFiveProgressStages() {
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.ORDER_RECEIVED,
                "Uber Eats\n訂單已收到"
        );
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.PREPARING,
                "Uber Eats\n正在準備訂單"
        );
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.PICKING_UP,
                "Uber Eats\n正在取餐"
        );
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.ON_THE_WAY,
                "Uber Eats\n正前往您所在位置"
        );
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.ARRIVING,
                "Uber Eats\n快到了！"
        );
    }

    @Test
    public void uberEatsEndedNotificationsEndTheJourney() {
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.ORDER_ENDED,
                "Uber Eats\n訂單已送達"
        );
        assertUberEatsEvent(
                RideNotificationParser.UberEatsEvent.ORDER_ENDED,
                "Uber Eats\n您的訂單已取消"
        );
    }

    @Test
    public void uberEatsReadsExactShortCriticalPin() {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats("Uber Eats\n快到了！", "7616");

        assertEquals("7616", update.getPin());
    }

    @Test
    public void uberEatsReadsContextualPinFromNotificationText() {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats(
                        "Uber Eats\n正前往您所在位置\n取餐碼：7616",
                        null
                );

        assertEquals("7616", update.getPin());
    }

    @Test
    public void uberEatsDoesNotTreatTimesYearsOrOrderNumbersAsPin() {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats(
                        "抵達時間 1:58 PM\n2026-07-09\n訂單 #7616",
                        null
                );

        assertNull(update.getPin());
    }

    @Test
    public void uberEatsRejectsAmbiguousContextualPins() {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats(
                        "PIN 7616\n交付碼 4821",
                        null
                );

        assertNull(update.getPin());
    }

    @Test
    public void uberEatsKeepsUpdatingWithoutPin() {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats(
                        "Uber Eats\n正在取餐\n抵達時間 1:58 PM",
                        null
                );

        assertEquals(
                RideNotificationParser.UberEatsEvent.PICKING_UP,
                update.getEvent()
        );
        assertNull(update.getPin());
    }

    private static void assertUberEatsEvent(
            RideNotificationParser.UberEatsEvent expected,
            String text
    ) {
        assertEquals(
                expected,
                RideNotificationParser.parseUberEats(text, null).getEvent()
        );
    }
}
