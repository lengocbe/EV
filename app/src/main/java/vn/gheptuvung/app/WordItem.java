package vn.gheptuvung.app;

public final class WordItem {
    public final long id;
    public final String english;
    public final String vietnamese;

    public WordItem(long id, String english, String vietnamese) {
        this.id = id;
        this.english = english;
        this.vietnamese = vietnamese;
    }
}
