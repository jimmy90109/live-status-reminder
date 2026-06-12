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
}
