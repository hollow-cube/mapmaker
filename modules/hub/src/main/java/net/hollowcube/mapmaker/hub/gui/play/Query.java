package net.hollowcube.mapmaker.hub.gui.play;

public class Query {
    public String query = "None";
    public Boolean isQueryMap = false;
    public Boolean takeQuery = false;

    public Query(String query, Boolean isQueryMap, Boolean takeQuery) {
        this.query = query;
        this.isQueryMap = isQueryMap;
        this.takeQuery = takeQuery;
    }

    public Query() {

    }

    @Override
    public String toString() {
        return "Query{" +
                "query='" + query + '\'' +
                ", isQueryMap=" + isQueryMap +
                ", takeQuery=" + takeQuery +
                '}';
    }
}
