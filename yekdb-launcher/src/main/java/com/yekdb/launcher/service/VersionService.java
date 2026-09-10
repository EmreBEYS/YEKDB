package com.yekdb.launcher.service;

import java.util.ArrayList;
import java.util.List;

public final class VersionService {
    public boolean isNewer(String candidate, String current) {
        List<Integer> left = parts(candidate);
        List<Integer> right = parts(current);
        int length = Math.max(left.size(), right.size());
        for (int index = 0; index < length; index++) {
            int candidatePart = index < left.size() ? left.get(index) : 0;
            int currentPart = index < right.size() ? right.get(index) : 0;
            if (candidatePart != currentPart) return candidatePart > currentPart;
        }
        return false;
    }

    private List<Integer> parts(String version) {
        String normalized = version == null ? "" : version.trim().replaceFirst("^[vV]", "");
        String numeric = normalized.split("[-+]", 2)[0];
        List<Integer> result = new ArrayList<>();
        for (String part : numeric.split("\\.")) {
            try {
                result.add(Integer.parseInt(part.replaceAll("[^0-9].*$", "")));
            } catch (NumberFormatException exception) {
                result.add(0);
            }
        }
        return result;
    }
}
