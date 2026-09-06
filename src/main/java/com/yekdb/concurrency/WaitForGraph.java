package com.yekdb.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Kilit bekleyen owner'lar arasindaki bagimliliklari tutar.
 */
public final class WaitForGraph {

    private final Map<String, Set<String>> dependencies =
            new HashMap<>();

    public synchronized void replaceDependencies(
            String ownerId,
            Collection<String> blockingOwnerIds
    ) {

        Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        Objects.requireNonNull(
                blockingOwnerIds,
                "Blocking ownerIds cannot be null."
        );

        LinkedHashSet<String> blockers =
                new LinkedHashSet<>(blockingOwnerIds);

        blockers.remove(ownerId);

        if (blockers.isEmpty()) {
            dependencies.remove(ownerId);
            return;
        }

        dependencies.put(ownerId, blockers);
    }

    public synchronized void removeDependencies(
            String ownerId
    ) {
        dependencies.remove(ownerId);
    }

    public synchronized void replaceAllDependencies(
            Map<String, ? extends Collection<String>> newDependencies
    ) {

        Objects.requireNonNull(
                newDependencies,
                "NewDependencies cannot be null."
        );

        dependencies.clear();

        newDependencies.forEach(
                this::replaceDependencies
        );
    }

    public synchronized Optional<List<String>> findCycleFrom(
            String ownerId
    ) {

        Objects.requireNonNull(
                ownerId,
                "OwnerId cannot be null."
        );

        List<String> path =
                new ArrayList<>();

        path.add(ownerId);

        boolean cycleFound =
                findPathBackToOwner(
                        ownerId,
                        ownerId,
                        new HashSet<>(),
                        path
                );

        if (!cycleFound) {
            return Optional.empty();
        }

        return Optional.of(List.copyOf(path));
    }

    public synchronized int getWaitingOwnerCount() {
        return dependencies.size();
    }

    public synchronized Map<String, List<String>> snapshotDependencies() {

        Map<String, List<String>> snapshot =
                new TreeMap<>();

        dependencies.forEach((ownerId, blockers) ->
                snapshot.put(
                        ownerId,
                        List.copyOf(blockers)
                )
        );

        return Collections.unmodifiableMap(
                snapshot
        );
    }

    private boolean findPathBackToOwner(
            String currentOwnerId,
            String targetOwnerId,
            Set<String> visited,
            List<String> path
    ) {

        if (!visited.add(currentOwnerId)) {
            return false;
        }

        for (String blockingOwnerId
                : dependencies.getOrDefault(
                        currentOwnerId,
                        Set.of()
                )) {

            path.add(blockingOwnerId);

            if (blockingOwnerId.equals(targetOwnerId)) {
                return true;
            }

            if (findPathBackToOwner(
                    blockingOwnerId,
                    targetOwnerId,
                    visited,
                    path
            )) {
                return true;
            }

            path.remove(path.size() - 1);
        }

        return false;
    }
}
