package com.github.streamshub.systemtests;

import java.util.NavigableSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.github.zafarkhaja.semver.Version;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "CONSOLE_SYSTEMTESTS_TEST_GITHUB_API_ENABLED", matches = "true")
class EnvironmentGitHubReleaseTest {

    @Test
    void testGitHubReleaseAPIReturnsAll() {
        NavigableSet<Version> allReleases = Environment.findGitHubReleases();
        assertNotNull(allReleases);
        assertThat(allReleases, hasSize(greaterThan(0)));
    }
}
