package com.goosethings.tools.client.update;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** GooseTools uses +Alpha as a release sequence, rather than SemVer build metadata. */
public record UpdateVersion(BigInteger major, BigInteger minor, BigInteger patch,
                            BigInteger alphaMajor, BigInteger alphaMinor) implements Comparable<UpdateVersion> {
    private static final Pattern FORMAT = Pattern.compile(
            "v?(\\d+)\\.(\\d+)\\.(\\d+)(?:\\+Alpha(\\d+)\\.(\\d+))?");

    public static UpdateVersion parse(String value) {
        var matcher = FORMAT.matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Unsupported GooseTools version");
        return new UpdateVersion(new BigInteger(matcher.group(1)), new BigInteger(matcher.group(2)),
                new BigInteger(matcher.group(3)),
                matcher.group(4) == null ? null : new BigInteger(matcher.group(4)),
                matcher.group(5) == null ? null : new BigInteger(matcher.group(5)));
    }

    @Override
    public int compareTo(UpdateVersion other) {
        int result = major.compareTo(other.major);
        if (result == 0) result = minor.compareTo(other.minor);
        if (result == 0) result = patch.compareTo(other.patch);
        // In this project's convention 1.14.0+Alpha0.1 is an update AFTER 1.14.0.
        if (result == 0) result = Boolean.compare(alphaMajor != null, other.alphaMajor != null);
        if (result == 0 && alphaMajor != null) result = alphaMajor.compareTo(other.alphaMajor);
        if (result == 0 && alphaMinor != null) result = alphaMinor.compareTo(other.alphaMinor);
        return result;
    }
}
