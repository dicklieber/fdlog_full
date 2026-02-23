/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * Utility helpers to record Micrometer metrics from Scala 3 without overload ambiguity.
 */
package fdswarm.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.DistributionSummary;
import java.util.concurrent.TimeUnit;

public final class MetricsHelpers {
    private MetricsHelpers() {}

    public static void recordTimerNanos(MeterRegistry registry, String name, long nanos) {
        Timer timer = registry.find(name).timer();
        if (timer == null) {
            timer = Timer.builder(name).register(registry);
        }
        timer.record(nanos, TimeUnit.NANOSECONDS);
    }

    public static void recordSummary(MeterRegistry registry, String name, double value) {
        DistributionSummary summary = registry.find(name).summary();
        if (summary == null) {
            summary = DistributionSummary.builder(name).register(registry);
        }
        summary.record(value);
    }
}
