package de.raidcraft.achievements;

import net.kyori.adventure.text.feature.pagination.Pagination;

public final class Constants {

    public static final int RESULTS_PER_PAGE = 10;
    public static final int PAGE_WIDTH = Pagination.WIDTH - 4;
    public static final String TABLE_PREFIX = "rcachievements_";
    public static final String DEFAULT_TYPE = "none";
    public static final String PERMISSION_PREFIX = "rcachievements.";
    public static final String SHOW_HIDDEN = PERMISSION_PREFIX + "achievement.hidden";

    private Constants() {}
}
