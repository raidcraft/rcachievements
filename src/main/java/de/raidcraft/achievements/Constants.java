package de.raidcraft.achievements;

import net.kyori.adventure.text.feature.pagination.Pagination;

public final class Constants {

    public static final int RESULTS_PER_PAGE = 10;
    public static final int PAGE_WIDTH = Pagination.WIDTH - 4;
    public static final String TABLE_PREFIX = "rcachievements_";
    public static final String DEFAULT_TYPE = "none";
    public static final String PERMISSION_PREFIX = "rcachievements.";
    public static final String SHOW_OTHERS_PERMISSION = PERMISSION_PREFIX + "achievements.others";
    public static final String ACHIEVEMENT_PERMISSION_PREFIX = PERMISSION_PREFIX + "achievement.";
    public static final String SHOW_HIDDEN = PERMISSION_PREFIX + "achievements.hidden";
    public static final String SHOW_SECRET = PERMISSION_PREFIX + "achievements.secret";
    public static final String SHOW_ALIAS = PERMISSION_PREFIX + "achievements.showalias";
    public static final String SHOW_ADMIN_DETAILS = PERMISSION_PREFIX + "admin.details";

    private Constants() {}
}
