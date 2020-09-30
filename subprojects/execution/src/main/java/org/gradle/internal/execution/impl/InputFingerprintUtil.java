/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.execution.impl;

import com.google.common.collect.ImmutableSortedMap;
import org.gradle.api.UncheckedIOException;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.snapshot.ValueSnapshot;
import org.gradle.internal.snapshot.ValueSnapshotter;

public class InputFingerprintUtil {

    public static ImmutableSortedMap<String, ValueSnapshot> fingerprintInputProperties(
        UnitOfWork work,
        ImmutableSortedMap<String, ValueSnapshot> previousSnapshots,
        ValueSnapshotter valueSnapshotter,
        ImmutableSortedMap<String, ValueSnapshot> alreadyKnownSnapshots
    ) {
        ImmutableSortedMap.Builder<String, ValueSnapshot> builder = ImmutableSortedMap.naturalOrder();
        builder.putAll(alreadyKnownSnapshots);
        work.visitInputProperties((propertyName, value, identity) -> {
            if (alreadyKnownSnapshots.containsKey(propertyName)) {
                return;
            }
            try {
                ValueSnapshot previousSnapshot = previousSnapshots.get(propertyName);
                if (previousSnapshot == null) {
                    builder.put(propertyName, valueSnapshotter.snapshot(value));
                } else {
                    builder.put(propertyName, valueSnapshotter.snapshot(value, previousSnapshot));
                }
            } catch (Exception e) {
                throw new UncheckedIOException(String.format("Unable to store input properties for %s. Property '%s' with value '%s' cannot be serialized.",
                    work.getDisplayName(), propertyName, value), e);
            }
        });
        return builder.build();
    }

    public static ImmutableSortedMap<String, CurrentFileCollectionFingerprint> fingerprintInputFiles(
        UnitOfWork work,
        ImmutableSortedMap<String, CurrentFileCollectionFingerprint> alreadyKnownFingerprints
    ) {
        ImmutableSortedMap.Builder<String, CurrentFileCollectionFingerprint> builder = ImmutableSortedMap.naturalOrder();
        builder.putAll(alreadyKnownFingerprints);
        work.visitInputFileProperties((propertyName, value, type, identity, fingerprinter) -> {
            if (alreadyKnownFingerprints.containsKey(propertyName)) {
                return;
            }
            builder.put(propertyName, fingerprinter.get());
        });
        return builder.build();
    }
}
