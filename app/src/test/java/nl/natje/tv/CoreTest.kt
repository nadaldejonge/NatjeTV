package nl.natje.tv

import org.junit.Assert.*
import org.junit.Test
import nl.natje.tv.settings.Configuration
import nl.natje.tv.nightmode.FilterMath
import java.time.LocalTime

class CoreTest {
    @Test fun scheduleCoversEveryMinuteAcrossMidnight() {
        for(minute in 0..1439) assertEquals("minute $minute",minute>=1320 || minute<420,Configuration.inSchedule(minute,1320,420))
    }
    @Test fun daytimeScheduleAndEqualBoundaries() {
        for(minute in 0..1439) {
            assertEquals(minute in 600..899,Configuration.inSchedule(minute,600,900))
            assertFalse(Configuration.inSchedule(minute,100,100))
        }
    }
    @Test fun manualOffAndOnOverrideSchedule() {
        assertFalse(Configuration(nightMode="off").nightActive(LocalTime.of(23,0)))
        assertTrue(Configuration(nightMode="on").nightActive(LocalTime.NOON))
        assertFalse(Configuration(nightMode="schedule").nightActive(LocalTime.of(7,0)))
        assertTrue(Configuration(nightMode="schedule").nightActive(LocalTime.of(22,0)))
    }
    @Test fun latestNatjeTubeRangeIsSixtyPercentNotSix() {
        assertEquals(.60f,FilterMath.warmthAlpha(100),.00001f)
        assertEquals(.45f,FilterMath.warmthAlpha(75),.00001f)
        assertEquals(.80f,FilterMath.dimAlpha(80),.00001f)
        assertEquals(0f,FilterMath.warmthAlpha(100,0f),.00001f)
        assertEquals(.30f,FilterMath.warmthAlpha(100,.5f),.00001f)
        assertEquals(.60f,FilterMath.warmthAlpha(999),.00001f)
    }
}
