package net.hollowcube.apiserver.common;

/// The two ids every api process that talks to PostHog needs.
public final class PostHogIds {
    /// The project key, not a secret: it is on the website.
    public static final String PROJECT_KEY = "phc_mK0jji1aC3hvMBGLOLjuVARqolDGPS9AiuNUOhMwVyA";

    /// The distinct id the Go services file internal, not-about-a-player events under, so that
    /// they neither create a person nor attach to one.
    public static final String INTERNAL_ID = "cccccccb-57f7-45fc-98ef-b4d2f51f5ea6";

    private PostHogIds() {
    }
}
